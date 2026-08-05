# AGENTS.md

## Proyecto

Passly — sistema de reserva de tickets para eventos, proyecto de portfolio. Monorepo: frontend Next.js (`apps/web`) + microservicios Spring Boot (`services/*`) + infraestructura (`infra/`). Ver `docs/ROADMAP.md` para la guía completa.

## Entorno (Windows)

- JDK 25 en `C:\Program Files\Microsoft\jdk-25.0.4.7-hotspot`. `java` no está en el PATH global del shell del agente: cablear `$env:JAVA_HOME` a esa ruta (y anteponer `$env:JAVA_HOME\bin` a `$env:Path`) antes de ejecutar builds.
- Builds de Maven con el Maven wrapper (`./mvnw`), sin Maven global.

## Agent skills

### Issue tracker

Issues y specs viven en GitHub Issues (repo Ainsiel/Passly) vía la CLI `gh`. Ver `docs/agents/issue-tracker.md`.

### Triage labels

Vocabulario de labels: `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`. Ver `docs/agents/triage-labels.md`.

### Domain docs

Layout single-context: `CONTEXT.md` + `docs/adr/` en la raíz del repo. Ver `docs/agents/domain.md`.
