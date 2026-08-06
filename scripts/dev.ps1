#!/usr/bin/env pwsh
# Orquesta el entorno de desarrollo local de Passly (docker compose).
# 'up' levanta el núcleo (postgres, keycloak, catalog, gateway, web).
# 'full' añade las opcionales: rabbitmq + mailhog (messaging) y prometheus + grafana (observability).

[CmdletBinding()]
param(
    [ValidateSet("up", "full", "down", "logs", "status", "smoke")]
    [string]$Command = "up"
)

$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $PSScriptRoot
$ComposeFile = Join-Path $Root "infra\docker-compose.yml"
$EnvFile = Join-Path $Root "infra\.env"
$SmokeScript = Join-Path $PSScriptRoot "smoke.ps1"

function Invoke-Compose {
    param([Parameter(Mandatory)][string[]]$ComposeArgs)
    docker compose -f $ComposeFile --env-file $EnvFile @ComposeArgs
    if ($LASTEXITCODE -ne 0) {
        throw "docker compose fallo (codigo $LASTEXITCODE): docker compose $($ComposeArgs -join ' ')"
    }
}

function Assert-EnvFile {
    if (-not (Test-Path $EnvFile)) {
        $Rng = New-Object System.Security.Cryptography.RNGCryptoServiceProvider
        $Bytes = New-Object byte[] 32
        $Rng.GetBytes($Bytes)
        $Secret = [Convert]::ToBase64String($Bytes)
        Set-Content -Path $EnvFile -Value "AUTH_SECRET=$Secret" -Encoding ascii
        Write-Host "Generado infra\.env con AUTH_SECRET nuevo (no se versiona)." -ForegroundColor Yellow
    }
}

function Wait-Healthy {
    param([int]$TimeoutSeconds = 180)
    $Deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    $Pending = $null
    do {
        Start-Sleep -Seconds 5
        $State = docker compose -f $ComposeFile --env-file $EnvFile ps --format json 2>$null | ConvertFrom-Json
        if ($State) {
            $Pending = @($State | Where-Object { $_.Health -ne "healthy" })
        }
    } while ($Pending.Count -gt 0 -and (Get-Date) -lt $Deadline)
    if ($Pending -and $Pending.Count -gt 0) {
        Write-Host "Aviso: algunos servicios no llegan a healthy (consulta '.\scripts\dev.ps1 status' o 'logs')." -ForegroundColor Yellow
    }
}

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw "Docker no esta disponible. Arranca Docker Desktop y vuelve a intentarlo."
}

switch ($Command) {
    "up" {
        Assert-EnvFile
        Write-Host "Levantando el nucleo de Passly (postgres, keycloak, catalog, gateway, web)..." -ForegroundColor Cyan
        Invoke-Compose @("up", "-d", "--build")
        Write-Host "Esperando a que los servicios esten healthy (max 3 min)..."
        Wait-Healthy
        Write-Host ""
        Write-Host "Entorno 'develop' listo:" -ForegroundColor Green
        Write-Host "  Keycloak      http://localhost:8080  (admin / admin)"
        Write-Host "  Postgres      localhost:5432  (passly / passly) - bases: catalog, booking, notification"
        Write-Host "  Catalog API   http://localhost:8081/actuator/health"
        Write-Host "  Gateway       http://localhost:8090/actuator/health"
        Write-Host "  Web app       http://localhost:3000"
        Write-Host ""
        Write-Host "Opcionales (rabbitmq, mailhog, prometheus, grafana): .\scripts\dev.ps1 full"
        Write-Host "Verificacion: .\scripts\dev.ps1 smoke" -ForegroundColor Yellow
    }
    "full" {
        Assert-EnvFile
        Write-Host "Levantando Passly completo (nucleo + messaging + observability)..." -ForegroundColor Cyan
        Invoke-Compose @("up", "-d", "--build", "--profile", "messaging", "--profile", "observability")
        Write-Host "Esperando a que los servicios esten healthy (max 4 min)..."
        Wait-Healthy -TimeoutSeconds 240
        Write-Host ""
        Write-Host "Entorno 'develop' completo listo:" -ForegroundColor Green
        Write-Host "  Keycloak      http://localhost:8080  (admin / admin)"
        Write-Host "  Postgres      localhost:5432  (passly / passly) - bases: catalog, booking, notification"
        Write-Host "  RabbitMQ      http://localhost:15672  (passly / passly)"
        Write-Host "  Mailhog       http://localhost:8025"
        Write-Host "  Prometheus    http://localhost:9090"
        Write-Host "  Grafana       http://localhost:3001  (admin / admin)"
        Write-Host "  Catalog API   http://localhost:8081/actuator/health"
        Write-Host "  Gateway       http://localhost:8090/actuator/health"
        Write-Host "  Web app       http://localhost:3000"
        Write-Host ""
        Write-Host "Verificacion: .\scripts\dev.ps1 smoke" -ForegroundColor Yellow
    }
    "down" {
        Invoke-Compose @("down")
    }
    "logs" {
        Invoke-Compose @("logs", "-f")
    }
    "status" {
        Invoke-Compose @("ps")
    }
    "smoke" {
        & $SmokeScript
    }
}
