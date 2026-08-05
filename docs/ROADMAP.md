# Passly — Roadmap 0 → Portfolio

Guía completa para construir una web app de **reserva de tickets para eventos** como proyecto de portfolio. No es una app que se despliegue en producción; el objetivo es demostrar arquitectura avanzada y prácticas de ingeniería reales en un CV.

---

## 1. Visión del proyecto

**Qué:** un usuario se registra e inicia sesión (Keycloak), navega y filtra eventos, ve el detalle de un evento, reserva un ticket y recibe el ticket por email.

**Para qué:** diferenciarte de otros candidatos mostrando, no solo que "haces CRUDs", sino que dominas arquitectura de software, backend avanzado, testing a todos los niveles y CI/CD.

**Límite:** ~5 casos de uso funcionales. No es una app gigante; es una app pequeña con arquitectura de empresa.

### Casos de uso objetivo (a afinar en el grill)

1. Registrar e iniciar sesión (Keycloak, OAuth2 Authorization Code + PKCE).
2. Ver la lista de eventos y filtrar.
3. Ver el detalle de un evento.
4. Reservar un ticket para un evento (con control de concurrencia).
5. Recibir el ticket por email tras la reserva.

---

## 2. Decisiones confirmadas

| Área | Decisión |
| --- | --- |
| Issue tracker | GitHub Issues (repo `Ainsiel/Passly`) |
| Flujo de skills | Main flow: grill-with-docs → to-spec → to-tickets → implement |
| Microservicios | 3 + gateway: `catalog`, `booking`, `notification` + Spring Cloud Gateway |
| Mensajería | RabbitMQ + patrón **outbox** (consistencia transaccional real) |
| Entorno QA | **Ephemeral** en GitHub Actions (docker compose en el runner, se destruye al final) |
| Tests de carga | **k6** (spike, soak, capacity) |
| Auth | Keycloak (self-hosted en docker compose) |
| Backend | Spring Boot 3.x, arquitectura hexagonal, Spring Data JPA, Testcontainers |
| Frontend | Next.js App Router, shadcn/ui, arquitectura por features (Vercel) |
| Monorepo | Un solo repo, `apps/` + `services/` + `infra/` |
| Entornos | `develop` (local docker compose) y `QA` (ephemeral CI) |
| CI/CD | GitHub Actions (build, test, lint, e2e, smoke, load, deploy QA) |
| JDK | OpenJDK 25.0.4 LTS en `C:\Program Files\Microsoft\jdk-25.0.4.7-hotspot` |

> Nota: OpenJDK 25 es reciente. En el grill se verificará la compatibilidad con la versión de Spring Boot elegida y se ajustará si hace falta.

---

## 3. Skills del agente

### 3.1 Flujo de skills de Matt (idea → implementación)

```
FASE 0  /setup-matt-pocock-skills         precondición, corre primero
FASE 1  /grill-with-docs                  afinar la idea (grilling + domain-modeling)
FASE 2  /handoff → /prototype → /handoff  solo si una pregunta necesita respuesta ejecutable
FASE 3  /to-spec                          sintetiza en spec, sin entrevista
FASE 4  /to-tickets                       tickets tracer-bullet con blocking edges
FASE 5  /implement (→ /tdd → /code-review) por ticket, blockers-first
FASE 6  /improve-codebase-architecture + QA continuo
```

- **FASE 0 — `/setup-matt-pocock-skills`.** Configura el issue tracker (GitHub Issues), los triage labels y los domain docs (`CONTEXT.md` + `docs/adr/`). Crea `AGENTS.md` con el bloque "Agent skills".
- **FASE 1 — `/grill-with-docs`.** Entrevista por rondas sobre un árbol de decisiones. Produce `CONTEXT.md` (glosario del dominio) y ADRs en `docs/adr/`. Las decisiones esperadas: casos de uso exactos, NFRs, límites de los servicios, modelo de concurrencia, layout del monorepo, estrategia de entornos, email.
- **FASE 2 — Prototipo (solo si surge).** Si una pregunta necesita respuesta *ejecutable* — candidatas: la **concurrencia de reservas** (modelo de estado) y la **UI de booking** — se hace `/handoff` a una carpeta aparte, `/prototype`, y `/handoff` de vuelta. El prototipo vive en rama `prototype/<name>` y se referencia desde su ticket.
- **FASES 3-4 — `/to-spec` → `/to-tickets`.** Todo en **un solo contexto sin interrumpir** (nada de `/clear` ni `/compact` hasta después de to-tickets). `/to-tickets` corta la spec en slices verticales demoables con blocking edges (links nativos en GitHub). Se trabaja blockers-first por el **frontier**.
- **FASE 5 — `/implement` por ticket.** Cada sesión arranca fresca del ticket. El implement usa `/tdd` (rojo→verde, en seams acordados) y cierra con `/code-review` (ejes Standards + Spec) antes de commitear. Entre tickets: `/clear` (contexto desechable) o `/compact` si algo importa.
- **FASE 6 — QA y salud.** Los tests de carga/stress son tickets propios. Al final: `/improve-codebase-architecture` + README.

**Skills standalone que aparecerán por el camino:** `/research` (Keycloak, Testcontainers, outbox, k6), `/diagnosing-bugs` (si algo se rompe), `/handoff` en cada límite de fase, `/to-questionnaire` (si algo depende de otra persona), `/teach` (aprender conceptos).

### 3.2 Skills tecnológicas instaladas

| Skill | Origen | Para qué |
| --- | --- | --- |
| `vercel-react-best-practices` | vercel-labs/agent-skills | Best practices oficiales de Vercel para React/Next.js |
| `vercel-composition-patterns` | vercel-labs/agent-skills | Patrones de composición (RSC, Server Actions) |
| `web-design-guidelines` | vercel-labs/agent-skills | Guías de diseño web |
| `frontend-design` | anthropics/skills | Diseño de interfaces frontend |
| `webapp-testing` | anthropics/skills | Testing de webapps con Playwright (e2e) |
| `shadcn` | shadcn/ui | Uso correcto de shadcn/ui |
| `java-springboot` | github/awesome-copilot | Best practices de Spring Boot (oficial GitHub) |

**Pendientes:**
- `sivaprasadreddy/sivalabs-agent-skills@spring-boot` (opcional): `npx skills add sivaprasadreddy/sivalabs-agent-skills --skill spring-boot -y`
- Skill propia de k6 (si `npx skills find k6` no encuentra nada sólido): crear con `/writing-for-agents`.

---

## 4. Roadmap técnico

### Fase A — Bootstrap del repo

Estructura monorepo propuesta:

```
Passly/
├── apps/
│   └── web/                    # Next.js (App Router, shadcn/ui)
├── services/
│   ├── gateway/                # Spring Cloud Gateway (routing, CORS, auth)
│   ├── catalog-service/        # Eventos (bounded context: catálogo)
│   ├── booking-service/        # Reservas (bounded context: reservas) — concurrencia + outbox
│   └── notification-service/   # Emails (bounded context: notificaciones)
├── infra/
│   ├── docker-compose.yml      # postgres xN, keycloak, mailhog, rabbitmq, prometheus, grafana
│   ├── keycloak/               # realm-export.json (import automático)
│   └── k6/                     # escenarios de carga (spike/soak/capacity)
├── docs/
│   ├── ROADMAP.md              # este archivo
│   ├── adr/                    # registros de decisión de arquitectura
│   └── agents/                 # config de las skills (issue-tracker, domain)
├── .github/workflows/          # pipelines CI/CD
├── AGENTS.md                   # instrucciones para agentes + skills
└── CONTEXT.md                  # glosario del dominio
```

Puntos de esta fase:
- Cablear `JAVA_HOME` a `C:\Program Files\Microsoft\jdk-25.0.4.7-hotspot` (por comando + documentado en AGENTS.md).
- Maven wrapper por servicio (no requiere Maven global).
- `docker-compose.yml` para el entorno `develop`.
- Commit inicial del esqueleto.

### Fase B — Backend (Spring Boot)

- **Keycloak:** realm con cliente de frontend (Authorization Code + PKCE), clients de servicio (client credentials) para comunicación entre servicios, `realm-export.json` versionado e importado por docker compose.
- **Por servicio:** arquitectura hexagonal (dominio en el centro, puertos/adaptadores afuera), DTOs en los bordes, validación con Bean Validation, manejo global de errores (`@RestControllerAdvice` + RFC 7807 Problem Details), transacciones en el servicio, config tipada con `@ConfigurationProperties`.
- **catalog-service:** CRUD de eventos + filtros; consultas de lectura. Spring Data JPA.
- **booking-service:** el corazón. Reserva con **optimistic locking** (columna `version` / `@Version`) + constraint único, **idempotencia** con idempotency key, y **outbox pattern** (escribe reserva + evento de outbox en la misma transacción; un poller/publicador empuja a RabbitMQ).
- **notification-service:** consume de RabbitMQ, genera el ticket (plantilla/PDF) y lo envía por email vía Mailhog en local; retries con dead-letter queue.
- **gateway:** Spring Cloud Gateway, routing hacia los servicios, propagación del JWT.
- **Observabilidad:** Spring Boot Actuator + Micrometer, métricas a Prometheus, dashboards en Grafana.

### Fase C — Frontend (Next.js)

- **App Router + React Server Components**, Server Actions para mutaciones, shadcn/ui + Tailwind.
- **Arquitectura por features** (la recomendada por Vercel): cada feature agrupa sus componentes, hooks y tipos; capa de acceso a datos separada.
- **Auth:** Authorization Code + PKCE contra Keycloak (via servidor, cookies seguras).
- Rutas: lista de eventos (con filtros), detalle de evento, formulario de reserva, confirmación/ticket, perfil con tus reservas.
- Estados de carga/error/empty; accesibilidad.

### Fase D — Testing (pirámide)

| Nivel | Herramienta | Cobertura |
| --- | --- | --- |
| Unit | JUnit 5 + Mockito + AssertJ (Java) · Vitest + React Testing Library (TS) | Reglas de dominio, servicios, validación |
| Integración | Spring Boot Test + Testcontainers · MSW/Playwright API | Repos, colas, Keycloak, flujos cross-service |
| E2E | Playwright | Happy paths + auth completa contra Keycloak |
| Smoke | Health checks + Playwright smoke | Tras CI/CD, verifica que el sistema respira |
| Regresión | Suite E2E completa en PRs | No romper lo ya hecho |
| Load/stress | k6 | Spike, soak y capacity sobre la reserva |

**Concurrencia bajo test:** el escenario estrella es "N usuarios reservan los últimos M tickets" — debe quedar demostrado en tests de integración y en un escenario k6.

### Fase E — CI/CD (GitHub Actions)

- **Workflow por PR:** build + unit/integration tests → lint → e2e Playwright → reporte k6.
- **Workflow QA (ephemeral):** levanta el stack en docker compose, corre smoke + load tests, publica reportes (artifacts), y destruye todo.
- Badges de CI en el README.
- Branch strategy: `main` como la verdad; `develop` y `QA` como entornos.

### Fase F — Portfolio

- README que cuente la arquitectura (diagrama), el stack y cómo levantar el proyecto en un comando.
- ADRs documentando las decisiones con trade-offs.
- Reportes de carga k6 en el repo como evidencia.
- Capturas / demo en el README.

---

## 5. Entregables del portfolio (checklist)

- [ ] Monorepo limpio y reproducible (`docker compose up` = entorno develop completo).
- [ ] 3 microservicios hexagonales + gateway, con límites claros.
- [ ] Concurrencia real de reservas (optimistic locking + idempotencia) demostrada con tests.
- [ ] Outbox pattern: reserva y notificación consistentes.
- [ ] Keycloak integrado (login real, no mock).
- [ ] Frontend Next.js con buenas prácticas de Vercel + shadcn/ui.
- [ ] Pirámide de testing completa: unit, integración, e2e, smoke, regresión, carga.
- [ ] Pipelines GitHub Actions con entorno QA ephemeral y reportes.
- [ ] Observabilidad: métricas + dashboards.
- [ ] ADRs + README + diagrama de arquitectura.

---

## 6. Notas de entorno (Windows)

- Shell: PowerShell 5.1.
- JDK 25: `C:\Program Files\Microsoft\jdk-25.0.4.7-hotspot`. `java` no está en el PATH global del shell del agente; se cablea `JAVA_HOME` por comando y queda documentado en AGENTS.md.
- Los builds de Maven se ejecutan con el Maven wrapper (`./mvnw`) para no depender de instalación global.
