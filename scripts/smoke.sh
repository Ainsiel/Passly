#!/usr/bin/env bash
# Smoke test del entorno de Passly.
# Verifica el nucleo (postgres, keycloak, catalog, gateway, web) y el flujo de auth 401/200.
# Los servicios opcionales (rabbitmq, mailhog, prometheus, grafana) solo se comprueban si estan levantados.

set -euo pipefail

# Resolve project root: try BASH_SOURCE first, fall back to git rev-parse
if [ -n "${BASH_SOURCE[0]:-}" ]; then
  SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
else
  SCRIPT_DIR="$(pwd)"
fi
COMPOSE_FILE="${SCRIPT_DIR}/../infra/docker-compose.yml"
# Fallback: if compose file doesn't exist, try relative to cwd
[ ! -f "$COMPOSE_FILE" ] && COMPOSE_FILE="$(pwd)/infra/docker-compose.yml"
FAILURES=()
PASSES=0
SKIPS=0

# Detect Python: use python3 on Linux, python on Windows
if command -v python3 &>/dev/null && python3 --version &>/dev/null; then
  PYTHON=python3
elif command -v python &>/dev/null && python --version &>/dev/null; then
  PYTHON=python
else
  echo "Error: python3 or python not found" >&2
  exit 1
fi

# --- Helpers ---

log_pass() { printf "  \033[32m[PASS]\033[0m %s\n" "$1"; PASSES=$((PASSES + 1)); }
log_fail() { printf "  \033[31m[FAIL]\033[0m %s\n" "$1"; FAILURES+=("$1"); }
log_skip() { printf "  \033[90m[SKIP]\033[0m %s (contenedor no levantado)\n" "$1"; SKIPS=$((SKIPS + 1)); }

assert() {
  local name="$1"
  shift
  if "$@" >/dev/null 2>&1; then
    log_pass "$name"
  else
    log_fail "$name"
  fi
}

assert_http_code() {
  local name="$1" url="$2" expected="$3"
  shift 3
  local code
  code=$(curl -s -o /dev/null -w "%{http_code}" "$@" "$url" 2>/dev/null) || code="000"
  if [ "$code" = "$expected" ]; then
    log_pass "$name"
  else
    log_fail "$name (got $code, expected $expected)"
  fi
}

assert_skippable() {
  local name="$1" container="$2"
  shift 2
  if printf '%s\n' "${RUNNING[@]}" | grep -qx "$container"; then
    assert "$name" "$@"
  else
    log_skip "$name"
  fi
}

get_token() {
  local realm="$1" username="$2" password="$3"
  curl -s -X POST \
    "http://localhost:8080/realms/${realm}/protocol/openid-connect/token" \
    -d "grant_type=password" \
    -d "client_id=admin-cli" \
    -d "client_secret=admin-cli-secret" \
    -d "username=${username}" \
    -d "password=${password}" | $PYTHON -c "import sys,json; print(json.load(sys.stdin)['access_token'])"
}

# --- Discover running services ---

RUNNING=()
while IFS= read -r svc; do
  [ -n "$svc" ] && RUNNING+=("$svc")
done < <(docker compose -f "$COMPOSE_FILE" ps --format json 2>/dev/null \
  | $PYTHON -c "import sys,json; [print(json.loads(l)['Service']) for l in sys.stdin]" 2>/dev/null || true)

echo "Smoke test del entorno de Passly"
echo ""

# --- Core checks ---

_check_core_healthy() {
  local ps
  ps=$(docker compose -f "$COMPOSE_FILE" ps --format json 2>/dev/null) || return 1
  local core=("postgres" "keycloak" "catalog-service" "gateway" "web")
  for svc in "${core[@]}"; do
    local status
    status=$(echo "$ps" | $PYTHON -c "
import sys,json
for l in sys.stdin:
  d=json.loads(l)
  if d['Service']=='$svc':
    print(d.get('Health',''))
    break
" 2>/dev/null) || status=""
    [ "$status" != "healthy" ] && return 1
  done
  return 0
}

assert "docker compose: nucleo healthy (postgres, keycloak, catalog, gateway, web)" _check_core_healthy

assert "Postgres: bases catalog, booking y notification" _check_postgres_dbs

_check_postgres_dbs() {
  local dbs
  dbs=$(docker compose -f "$COMPOSE_FILE" exec -T postgres psql -U passly -d postgres \
    -tAc "SELECT datname FROM pg_database WHERE datname IN ('catalog','booking','notification') ORDER BY datname;" 2>/dev/null)
  local count
  count=$(echo "$dbs" | grep -c '[^ ]' || true)
  [ "$count" -eq 3 ]
}

assert "Keycloak: realm 'passly' responde al well-known" _check_keycloak_realm

_check_keycloak_realm() {
  local body
  body=$(curl -s "http://localhost:8080/realms/passly/.well-known/openid-configuration" 2>/dev/null)
  echo "$body" | grep -q '"issuer".*"http://localhost:8080/realms/passly"'
}

assert "Keycloak: roles ADMIN y USER en el realm" _check_keycloak_roles

_check_keycloak_roles() {
  local token roles
  token=$(get_token "master" "admin" "admin")
  roles=$(curl -s "http://localhost:8080/admin/realms/passly/roles" \
    -H "Authorization: Bearer $token" 2>/dev/null)
  echo "$roles" | $PYTHON -c "
import sys,json
roles=json.load(sys.stdin)
names={r['name'] for r in roles}
assert 'ADMIN' in names and 'USER' in names
"
}

assert "Keycloak: usuario 'admin' existe en el realm" _check_keycloak_admin_user

_check_keycloak_admin_user() {
  local token users
  token=$(get_token "master" "admin" "admin")
  users=$(curl -s "http://localhost:8080/admin/realms/passly/users?username=admin" \
    -H "Authorization: Bearer $token" 2>/dev/null)
  local count
  count=$(echo "$users" | $PYTHON -c "
import sys,json
users=json.load(sys.stdin)
print(sum(1 for u in users if u.get('username')=='admin'))
")
  [ "$count" -ge 1 ]
}

assert "Keycloak: login del usuario admin (admin123)" _check_keycloak_admin_login

_check_keycloak_admin_login() {
  get_token "passly" "admin" "admin123" >/dev/null
}

assert_http_code "Catalog: /me sin token -> 401" "http://localhost:8081/me" "401"

assert "Catalog: /me con token -> 200 (admin)" _check_catalog_me_auth

_check_catalog_me_auth() {
  local token body
  token=$(get_token "passly" "admin" "admin123")
  body=$(curl -s "http://localhost:8081/me" -H "Authorization: Bearer $token" 2>/dev/null)
  echo "$body" | $PYTHON -c "
import sys,json
d=json.load(sys.stdin)
assert d.get('username')=='admin' and len(d.get('roles',[]))>=1
"
}

assert_http_code "Gateway: /api/catalog/me sin token -> 401" "http://localhost:8090/api/catalog/me" "401"

assert "Gateway: /api/catalog/me con token -> 200 (admin)" _check_gateway_me_auth

_check_gateway_me_auth() {
  local token body
  token=$(get_token "passly" "admin" "admin123")
  body=$(curl -s "http://localhost:8090/api/catalog/me" -H "Authorization: Bearer $token" 2>/dev/null)
  echo "$body" | $PYTHON -c "
import sys,json
d=json.load(sys.stdin)
assert d.get('username')=='admin' and len(d.get('roles',[]))>=1
"
}

assert "Web: /api/auth/providers responde" _check_web_providers

_check_web_providers() {
  local body
  body=$(curl -s "http://localhost:3000/api/auth/providers" 2>/dev/null)
  echo "$body" | grep -q "keycloak"
}

# --- Optional checks ---

assert_skippable "RabbitMQ: management API responde" "rabbitmq" _check_rabbitmq

_check_rabbitmq() {
  local auth
  auth=$(echo -n "passly:passly" | base64)
  local code
  code=$(curl -s -o /dev/null -w "%{http_code}" \
    -H "Authorization: Basic $auth" "http://localhost:15672/api/overview" 2>/dev/null)
  [ "$code" = "200" ]
}

assert_skippable "Mailhog: API responde" "mailhog" _check_mailhog

_check_mailhog() {
  local code
  code=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:8025/api/v2/messages" 2>/dev/null)
  [ "$code" = "200" ]
}

assert_skippable "Prometheus: /-/healthy responde" "prometheus" _check_prometheus

_check_prometheus() {
  local code
  code=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:9090/-/healthy" 2>/dev/null)
  [ "$code" = "200" ]
}

assert_skippable "Grafana: /api/health responde" "grafana" _check_grafana

_check_grafana() {
  local code
  code=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:3001/api/health" 2>/dev/null)
  [ "$code" = "200" ]
}

# --- Summary ---

echo ""
if [ ${#FAILURES[@]} -eq 0 ]; then
  skip_text=""
  [ "$SKIPS" -gt 0 ] && skip_text=" ($SKIPS opcionales omitidos)"
  printf "\033[32mSmoke test completado: %d verificaciones OK%s.\033[0m\n" "$PASSES" "$skip_text"
  exit 0
else
  printf "\033[31mSmoke test con %d fallo(s):\033[0m\n" "${#FAILURES[@]}"
  for f in "${FAILURES[@]}"; do
    printf "  \033[31m- %s\033[0m\n" "$f"
  done
  exit 1
fi
