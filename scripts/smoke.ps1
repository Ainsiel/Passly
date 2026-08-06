#!/usr/bin/env pwsh
# Smoke test del entorno 'develop' de Passly.
# Verifica el núcleo (postgres, keycloak, catalog, gateway, web) y el flujo de auth 401/200.
# Los servicios opcionales (rabbitmq, mailhog, prometheus, grafana) solo se comprueban si estan levantados.

[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $PSScriptRoot
$ComposeFile = Join-Path $Root "infra\docker-compose.yml"
$Failures = [System.Collections.Generic.List[string]]::new()
$Passes = 0
$Skips = 0

function Assert {
    param([string]$Name, [scriptblock]$Condition)
    try {
        $Result = & $Condition
        if ($Result) {
            Write-Host "  [PASS] $Name" -ForegroundColor Green
            $script:Passes++
        }
        else {
            Write-Host "  [FAIL] $Name" -ForegroundColor Red
            $script:Failures.Add($Name)
        }
    }
    catch {
        Write-Host "  [FAIL] $Name - $($_.Exception.Message)" -ForegroundColor Red
        $script:Failures.Add($Name)
    }
}

function Assert-Skippable {
    param([string]$Name, [string]$Container, [scriptblock]$Condition)
    if ($Container -notin $script:Running) {
        Write-Host "  [SKIP] $Name (contenedor no levantado)" -ForegroundColor DarkGray
        $script:Skips++
        return
    }
    Assert -Name $Name -Condition $Condition
}

function Get-Token {
    param([string]$Realm, [string]$Username, [string]$Password)
    $Body = @{
        grant_type    = "password"
        client_id     = "admin-cli"
        client_secret = "admin-cli-secret"
        username      = $Username
        password      = $Password
    }
    $Response = Invoke-WebRequest -UseBasicParsing -Method Post `
        -Uri "http://localhost:8080/realms/$Realm/protocol/openid-connect/token" `
        -Body $Body -TimeoutSec 15
    ($Response.Content | ConvertFrom-Json).access_token
}

$Script:Running = @(docker compose -f $ComposeFile ps --format json 2>$null | ConvertFrom-Json |
    ForEach-Object { $_.Service })

Write-Host "Smoke test del entorno 'develop' de Passly" -ForegroundColor Cyan
Write-Host ""

# 1. Núcleo: contenedores levantados y healthy
Assert "docker compose: nucleo healthy (postgres, keycloak, catalog, gateway, web)" {
    $Ps = docker compose -f $ComposeFile ps --format json 2>$null | ConvertFrom-Json
    if (-not $Ps) { return $false }
    $Services = @($Ps)
    $Core = @("postgres", "keycloak", "catalog-service", "gateway", "web")
    @($Core | Where-Object { $_ -in $Services.Service }).Count -eq $Core.Count -and
        @($Services | Where-Object { $_.State -ne "running" -or $_.Health -ne "healthy" }).Count -eq 0
}

# 2. Postgres: existen las 3 bases
Assert "Postgres: bases catalog, booking y notification" {
    $Dbs = docker compose -f $ComposeFile exec -T postgres psql -U passly -d postgres `
        -tAc "SELECT datname FROM pg_database WHERE datname IN ('catalog','booking','notification') ORDER BY datname;" 2>$null
    @($Dbs | Where-Object { $_ -match "\S" }).Count -eq 3
}

# 3. Keycloak: realm 'passly' importado (issuer del well-known)
Assert "Keycloak: realm 'passly' responde al well-known" {
    $Body = (Invoke-WebRequest -UseBasicParsing `
        -Uri "http://localhost:8080/realms/passly/.well-known/openid-configuration" `
        -TimeoutSec 15).Content
    $Body -match '"issuer"\s*:\s*"http://localhost:8080/realms/passly"'
}

# 4. Keycloak: roles ADMIN y USER presentes
Assert "Keycloak: roles ADMIN y USER en el realm" {
    $Token = Get-Token -Realm "master" -Username "admin" -Password "admin"
    $Roles = (Invoke-WebRequest -UseBasicParsing `
        -Uri "http://localhost:8080/admin/realms/passly/roles" `
        -Headers @{ Authorization = "Bearer $Token" } -TimeoutSec 15).Content | ConvertFrom-Json
    @($Roles | Where-Object { $_.name -in @("ADMIN", "USER") }).Count -eq 2
}

# 5. Keycloak: usuario admin de prueba existe
Assert "Keycloak: usuario 'admin' existe en el realm" {
    $Token = Get-Token -Realm "master" -Username "admin" -Password "admin"
    $Users = (Invoke-WebRequest -UseBasicParsing `
        -Uri "http://localhost:8080/admin/realms/passly/users?username=admin" `
        -Headers @{ Authorization = "Bearer $Token" } -TimeoutSec 15).Content | ConvertFrom-Json
    @($Users | Where-Object { $_.username -eq "admin" }).Count -ge 1
}

# 6. Keycloak: las credenciales del usuario admin importadas son validas
Assert "Keycloak: login del usuario admin (admin123)" {
    try {
        $Null = Get-Token -Realm "passly" -Username "admin" -Password "admin123"
        $true
    }
    catch {
        $false
    }
}

# 7. Catalog-service: sin token -> 401
Assert "Catalog: /me sin token -> 401" {
    try {
        $Null = Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:8081/me" -TimeoutSec 15
        $false
    }
    catch {
        $_.Exception.Response.StatusCode.value__ -eq 401
    }
}

# 8. Catalog-service: con token -> 200 y usuario 'admin'
Assert "Catalog: /me con token -> 200 (admin)" {
    $Token = Get-Token -Realm "passly" -Username "admin" -Password "admin123"
    $Body = (Invoke-WebRequest -UseBasicParsing `
        -Uri "http://localhost:8081/me" `
        -Headers @{ Authorization = "Bearer $Token" } -TimeoutSec 15).Content | ConvertFrom-Json
    $Body.username -eq "admin" -and @($Body.roles).Count -ge 1
}

# 9. Gateway: rutas /api/catalog/** -> 401 sin token
Assert "Gateway: /api/catalog/me sin token -> 401" {
    try {
        $Null = Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:8090/api/catalog/me" -TimeoutSec 15
        $false
    }
    catch {
        $_.Exception.Response.StatusCode.value__ -eq 401
    }
}

# 10. Gateway: con token -> 200 y propagacion del Authorization header
Assert "Gateway: /api/catalog/me con token -> 200 (admin)" {
    $Token = Get-Token -Realm "passly" -Username "admin" -Password "admin123"
    $Body = (Invoke-WebRequest -UseBasicParsing `
        -Uri "http://localhost:8090/api/catalog/me" `
        -Headers @{ Authorization = "Bearer $Token" } -TimeoutSec 15).Content | ConvertFrom-Json
    $Body.username -eq "admin" -and @($Body.roles).Count -ge 1
}

# 11. Web: Auth.js responde en /api/auth/providers
Assert "Web: /api/auth/providers responde" {
    $Providers = (Invoke-WebRequest -UseBasicParsing `
        -Uri "http://localhost:3000/api/auth/providers" -TimeoutSec 15).Content
    $Providers -match "keycloak"
}

# 12. RabbitMQ: management API responde
Assert-Skippable "RabbitMQ: management API responde" -Container "rabbitmq" {
    $Auth = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("passly:passly"))
    $Status = (Invoke-WebRequest -UseBasicParsing `
        -Uri "http://localhost:15672/api/overview" `
        -Headers @{ Authorization = "Basic $Auth" } -TimeoutSec 15).StatusCode
    $Status -eq 200
}

# 13. Mailhog: API responde
Assert-Skippable "Mailhog: API responde" -Container "mailhog" {
    (Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:8025/api/v2/messages" -TimeoutSec 15).StatusCode -eq 200
}

# 14. Prometheus: /-/healthy responde
Assert-Skippable "Prometheus: /-/healthy responde" -Container "prometheus" {
    (Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:9090/-/healthy" -TimeoutSec 15).StatusCode -eq 200
}

# 15. Grafana: /api/health responde
Assert-Skippable "Grafana: /api/health responde" -Container "grafana" {
    (Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:3001/api/health" -TimeoutSec 15).StatusCode -eq 200
}

Write-Host ""
if ($Failures.Count -eq 0) {
    $SkipText = if ($Skips -gt 0) { " ($Skips opcionales omitidos)" } else { "" }
    Write-Host "Smoke test completado: $Passes verificaciones OK$SkipText." -ForegroundColor Green
    exit 0
}
else {
    Write-Host "Smoke test con $($Failures.Count) fallo(s):" -ForegroundColor Red
    $Failures | ForEach-Object { Write-Host "  - $_" -ForegroundColor Red }
    exit 1
}
