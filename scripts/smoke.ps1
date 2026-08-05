#!/usr/bin/env pwsh
# Smoke test del entorno 'develop' de Passly (ticket #2).
# Verifica: contenedores healthy, las 3 bases de Postgres, realm de Keycloak importado
# y health checks de RabbitMQ, Mailhog, Prometheus y Grafana.

[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $PSScriptRoot
$ComposeFile = Join-Path $Root "infra\docker-compose.yml"
$Failures = [System.Collections.Generic.List[string]]::new()
$Passes = 0

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

function Get-Token {
    param([string]$Realm, [string]$Username, [string]$Password)
    $Body = @{
        grant_type = "password"
        client_id  = "admin-cli"
        username   = $Username
        password   = $Password
    }
    $Response = Invoke-WebRequest -UseBasicParsing -Method Post `
        -Uri "http://localhost:8080/realms/$Realm/protocol/openid-connect/token" `
        -Body $Body -TimeoutSec 15
    ($Response.Content | ConvertFrom-Json).access_token
}

Write-Host "Smoke test del entorno 'develop' de Passly" -ForegroundColor Cyan
Write-Host ""

# 1. Todos los contenedores levantados y healthy
Assert "docker compose: todos los contenedores healthy" {
    $Ps = docker compose -f $ComposeFile ps --format json 2>$null | ConvertFrom-Json
    if (-not $Ps) { return $false }
    $Services = @($Ps)
    $Services.Count -ge 6 -and @($Services | Where-Object { $_.State -ne "running" -or $_.Health -ne "healthy" }).Count -eq 0
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

# 7. RabbitMQ: management API responde
Assert "RabbitMQ: management API responde" {
    $Auth = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("passly:passly"))
    $Status = (Invoke-WebRequest -UseBasicParsing `
        -Uri "http://localhost:15672/api/overview" `
        -Headers @{ Authorization = "Basic $Auth" } -TimeoutSec 15).StatusCode
    $Status -eq 200
}

# 8. Mailhog: API responde
Assert "Mailhog: API responde" {
    (Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:8025/api/v2/messages" -TimeoutSec 15).StatusCode -eq 200
}

# 9. Prometheus: /-/healthy responde
Assert "Prometheus: /-/healthy responde" {
    (Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:9090/-/healthy" -TimeoutSec 15).StatusCode -eq 200
}

# 10. Grafana: /api/health responde
Assert "Grafana: /api/health responde" {
    (Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:3000/api/health" -TimeoutSec 15).StatusCode -eq 200
}

Write-Host ""
if ($Failures.Count -eq 0) {
    Write-Host "Smoke test completado: $Passes verificaciones OK." -ForegroundColor Green
    exit 0
}
else {
    Write-Host "Smoke test con $($Failures.Count) fallo(s):" -ForegroundColor Red
    $Failures | ForEach-Object { Write-Host "  - $_" -ForegroundColor Red }
    exit 1
}
