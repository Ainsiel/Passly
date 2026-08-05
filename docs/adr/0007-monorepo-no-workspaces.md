# Monorepo sin tooling de workspaces

El repositorio es un monorepo de layout, no de build: el frontend es una app Next.js independiente y cada servicio es un proyecto Maven independiente con su propio wrapper. Se descartaron pnpm/turborepo (no hay múltiples apps frontend) y Maven multi-module (una sola build acopla el deploy de servicios con ciclos de vida distintos). Scripts raíz (`scripts/dev.ps1`) orquestan el arranque local.
