# 0013 — CI Quality Gate

## Contexto

El monorepo Passly (4 microservicios Spring Boot + frontend Next.js) no tiene quality gate automático. El código se mergea a main sin verificación de build, tests ni E2E. Esto permite regresiones silenciosas y rompe la confianza en el estado de la rama principal.

## Decisión

Implementar dos workflows de GitHub Actions:

1. **CI (por PR):** Build + tests unitarios/integración (matrix por servicio) + lint del frontend + E2E Playwright contra el stack completo en Docker Compose.
2. **QA (main + manual):** Stack efímero completo (incluyendo observabilidad) + smoke tests + E2E + publicación de artefactos (reportes, métricas Prometheus, dashboards Grafana).

### Por qué GitHub Actions

- Nativo del repositorio (ya hosted en GitHub). Sin infraestructura adicional ni configuración de runners externos.
- Integración directa con status checks de PR y branch protection.
- macOS/Linux runners gratuitos para proyectos de portfolio (2,000 min/mes).
- Docker pre-instalado en `ubuntu-latest`, sin setup adicional.

Se descartó Jenkins (requiere servidor self-hosted) y GitLab CI (el repo no está en GitLab).

### Matrix build para servicios Java

Cada microservicio se builda y teste independientemente en paralelo (`fail-fast: false`). Esto reduce el tiempo total de CI (~2-3min en paralelo vs ~8-10min en serie) y aísla fallos por servicio.

### E2E contra stack real (no mock)

Los tests Playwright corren contra Docker Compose levantado en el runner, no contra mocks o stubs. Esto valida:
- Comunicación inter-servicio (gateway → catalog-service, booking → rabbitmq → notification)
- Integración con Keycloak (auth real, tokens reales)
- Healthchecks y dependencias de infraestructura

El tradeoff es un CI más lento (~3-4min de startup del stack), pero la confianza en la cobertura de integración justifica el costo.

### QA efímero

Consistente con ADR-0008. El entorno QA se destruye después de cada run. No hay despliegue permanente ni superficie de mantenimiento. Los artefactos (reportes, métricas, dashboards) se preservan como artifacts de GitHub Actions con retención de 7 días.

## Consecuencias

### Positivas
- Regresiones detectadas antes de merge (build roto, test fallido, E2E roto).
- Feedback rápido al autor del PR (matrix paralelo, ~5-7min total).
- Artefactos de QA para auditoría y debugging (métricas Prometheus, dashboards Grafana).
- Consistencia con stack de observabilidad (ADR-0009).

### Negativos
- Tiempo de CI: ~5-7min con stack completo (aceptable para portfolio).
- Almacenamiento de artifacts: reportes Playwright + métricas pueden consumir ~50-100MB por run.
- Mantenimiento de workflows: cambios en docker-compose requieren actualizar health checks en CI.

### Riesgos
- Docker Compose merge entre base y overlay de messaging puede causar problemas si se añaden servicios con conflicts de nombre.
- Maven wrapper (v3.3.4) sin `.mvn/wrapper/` completo puede fallar en CI si no descarga Maven automáticamente.
- Keycloak cold start (~30s) es el cuello de botella del health poll.
