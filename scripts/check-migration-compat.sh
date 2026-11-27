#!/usr/bin/env bash
# check-migration-compat.sh — verify the main branch app works with this branch's new migrations.
#
# Simulates the rolling-update window: this branch's migrations are applied first, then the
# main branch code (current production) is started against the new schema.  Fails if the old
# app returns a 5xx on any basic endpoint, indicating a schema incompatibility.
#
# No GHCR access required.  Run locally on any branch that adds new migration files.
#
# Usage:
#   scripts/check-migration-compat.sh
#
# Optional env:
#   SKIP_COMPOSE — set to 1 to skip postgres startup (assumes postgres already on localhost:5432)

set -euo pipefail

SKIP_COMPOSE="${SKIP_COMPOSE:-0}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
WORKTREE="/tmp/vanilla-main-compat-$$"
MAIN_JAR="/tmp/vanilla-main-$$.jar"
MAIN_PID=""

log() { echo "[migration-compat] $*"; }
err() { echo "[migration-compat] ERROR: $*" >&2; }

cleanup() {
  [ -n "$MAIN_PID" ] && kill "$MAIN_PID" 2>/dev/null || true
  if [ "$SKIP_COMPOSE" = "0" ]; then
    docker compose -f "$REPO_DIR/compose.yml" down -v >/dev/null 2>&1 || true
  fi
  git -C "$REPO_DIR" worktree remove --force "$WORKTREE" 2>/dev/null || true
  rm -f "$MAIN_JAR"
}
trap cleanup EXIT

# ── 1. Build main branch JAR ──────────────────────────────────────────────────
log "Fetching main branch..."
git -C "$REPO_DIR" fetch origin main --depth=1
git -C "$REPO_DIR" worktree add --detach "$WORKTREE" FETCH_HEAD

log "Building main branch JAR..."
(cd "$WORKTREE" && chmod +x gradlew && ./gradlew bootJar --no-daemon -q)

cp "$(find "$WORKTREE/build/libs" -name "vanilla-*.jar" ! -name "*-plain.jar" | head -1)" "$MAIN_JAR"
git -C "$REPO_DIR" worktree remove "$WORKTREE"

# ── 2. Find or build PR JAR ───────────────────────────────────────────────────
PR_JAR=$(find "$REPO_DIR/build/libs" -name "vanilla-*.jar" ! -name "*-plain.jar" 2>/dev/null | head -1 || true)
if [ -z "$PR_JAR" ]; then
  log "No JAR found in build/libs — building PR JAR..."
  (cd "$REPO_DIR" && ./gradlew bootJar --no-daemon -q)
  PR_JAR=$(find "$REPO_DIR/build/libs" -name "vanilla-*.jar" ! -name "*-plain.jar" | head -1)
fi
log "PR JAR: $PR_JAR"

# ── 3. Start postgres ─────────────────────────────────────────────────────────
if [ "$SKIP_COMPOSE" = "0" ]; then
  log "Starting postgres..."
  docker compose -f "$REPO_DIR/compose.yml" up postgres --wait >/dev/null
fi

# ── 4. Apply new migrations using PR code ─────────────────────────────────────
log "Applying migrations..."
java -jar "$PR_JAR" \
  --spring.profiles.active=migrate \
  --spring.datasource.url=jdbc:postgresql://localhost:5432/vanilla \
  --spring.datasource.username=vanilla \
  --spring.datasource.password=vanilla

# ── 5. Start main branch app against new schema ───────────────────────────────
# SPRING_FLYWAY_ENABLED=false: schema is already migrated.  Without this, Flyway
# in the old app sees unknown migration versions (from this branch) in the history
# table and fails validation before the app can even serve requests.
log "Starting main branch app against new schema..."
java -jar "$MAIN_JAR" \
  --spring.datasource.url=jdbc:postgresql://localhost:5432/vanilla \
  --spring.datasource.username=vanilla \
  --spring.datasource.password=vanilla \
  --spring.flyway.enabled=false \
  --spring.profiles.active=local \
  >/tmp/vanilla-main-app-$$.log 2>&1 &
MAIN_PID=$!

for i in $(seq 1 24); do
  if curl -sf http://localhost:8080/actuator/health/readiness >/dev/null 2>&1; then
    log "App is ready."
    break
  fi
  if ! kill -0 "$MAIN_PID" 2>/dev/null; then
    err "Main branch app exited unexpectedly. Logs:"
    cat /tmp/vanilla-main-app-$$.log >&2
    exit 1
  fi
  [ "$i" -eq 24 ] && { err "Timed out waiting for app to become ready."; exit 1; }
  sleep 5
done

# ── 6. Verify old app handles new schema ──────────────────────────────────────
log "Running compatibility checks..."

curl -sf http://localhost:8080/actuator/health/liveness  >/dev/null
curl -sf http://localhost:8080/actuator/health/readiness >/dev/null

# 200 = data exists; a 500 would indicate a schema error (missing column, type mismatch)
STATUS=$(curl -s -o /dev/null -w '%{http_code}' "http://localhost:8080/products?maxPriceCents=10000")
[ "$STATUS" = "200" ] || { err "GET /products returned $STATUS (expected 200)"; exit 1; }

# 200 or 404 both acceptable; 500 indicates schema error
STATUS=$(curl -s -o /dev/null -w '%{http_code}' "http://localhost:8080/products/HEALTH")
[[ "$STATUS" =~ ^(200|404)$ ]] || { err "GET /products/HEALTH returned $STATUS (expected 200 or 404)"; exit 1; }

log "✓ Main branch app is compatible with new schema."
rm -f /tmp/vanilla-main-app-$$.log
