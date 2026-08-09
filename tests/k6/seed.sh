#!/usr/bin/env bash
# seed.sh — Inserta datos de prueba para k6 en la DB de booking-service.
# Uso: bash tests/k6/seed.sh
set -euo pipefail

BOOKING_CONTAINER="${BOOKING_CONTAINER:-passly-booking}"
PG_CONTAINER="${PG_CONTAINER:-passly-postgres}"
EVENT_ID="${EVENT_ID:-9999}"
CAPACITY="${CAPACITY:-100000}"

echo "Seed: creando evento de prueba (id=$EVENT_ID, capacity=$CAPACITY)..."

docker exec -T "$PG_CONTAINER" psql -U passly -d booking -tAc "
INSERT INTO event_projections (event_id, name, starts_at, price, capacity, reserved_tickets, version, updated_at)
VALUES ($EVENT_ID, 'Load Test Event', '2099-12-31T23:59:59', 0.00, $CAPACITY, 0, 0, now())
ON CONFLICT (event_id) DO UPDATE SET
  capacity = EXCLUDED.capacity,
  reserved_tickets = 0,
  version = 0,
  updated_at = now();
"

echo "Seed: evento creado exitosamente."
echo "Seed: verificando conectividad del booking-service..."

for i in $(seq 1 30); do
  if docker exec -T "$BOOKING_CONTAINER" curl -fsS http://127.0.0.1:8082/actuator/health >/dev/null 2>&1; then
    echo "Seed: booking-service está saludable."
    exit 0
  fi
  sleep 2
done

echo "Seed: ADVERTENCIA - booking-service no responde健康 check después de 60s"
exit 1
