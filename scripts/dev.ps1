#!/usr/bin/env pwsh
# Orquesta el entorno de desarrollo local de Passly (docker compose).

[CmdletBinding()]
param(
    [ValidateSet("up", "down", "logs", "status", "smoke")]
    [string]$Command = "up"
)

$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $PSScriptRoot
$ComposeFile = Join-Path $Root "infra\docker-compose.yml"
$SmokeScript = Join-Path $PSScriptRoot "smoke.ps1"

function Invoke-Compose {
    param([Parameter(Mandatory)][string[]]$ComposeArgs)
    docker compose -f $ComposeFile @ComposeArgs
    if ($LASTEXITCODE -ne 0) {
        throw "docker compose fallo (codigo $LASTEXITCODE): docker compose $($ComposeArgs -join ' ')"
    }
}

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw "Docker no esta disponible. Arranca Docker Desktop y vuelve a intentarlo."
}

switch ($Command) {
    "up" {
        Write-Host "Levantando el entorno 'develop' de Passly..." -ForegroundColor Cyan
        Invoke-Compose @("up", "-d", "--wait", "--wait-timeout", "300")
        Write-Host ""
        Write-Host "Entorno 'develop' listo:" -ForegroundColor Green
        Write-Host "  Keycloak   http://localhost:8080  (admin / admin)"
        Write-Host "  Postgres   localhost:5432  (passly / passly) - bases: catalog, booking, notification"
        Write-Host "  RabbitMQ   http://localhost:15672  (passly / passly)"
        Write-Host "  Mailhog    http://localhost:8025"
        Write-Host "  Prometheus http://localhost:9090"
        Write-Host "  Grafana    http://localhost:3000  (admin / admin)"
        Write-Host ""
        Write-Host "Verificacion de health checks: .\scripts\dev.ps1 smoke" -ForegroundColor Yellow
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
