#!/usr/bin/env bash
# k8s-migrate.sh — run database migrations before a rolling deployment.
#
# Apply this script BEFORE updating the Deployment image. The deploy sequence is:
#   1. scripts/k8s-migrate.sh   ← migrations committed in a single transaction
#   2. kubectl set image ...    ← rolling update starts only after step 1 succeeds
#   3. scripts/k8s-verify.sh   ← per-pod BB verification gate
#
# If the migration Job fails the script exits non-zero and the caller must not
# proceed to step 2.  The database schema is left unchanged (Flyway group=true
# ensures full rollback on any error).
#
# Required env:
#   APP_IMAGE  — fully-qualified app image to migrate with
#                (e.g. ghcr.io/bohemian/vanilla:sha-abc123)
#
# Optional env:
#   NAMESPACE  — Kubernetes namespace (default: default)
#   TIMEOUT    — seconds to wait for the Job to complete (default: 120)

set -euo pipefail

NAMESPACE="${NAMESPACE:-default}"
TIMEOUT="${TIMEOUT:-120}"
APP_IMAGE="${APP_IMAGE:?APP_IMAGE must be set}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JOB_TEMPLATE="${SCRIPT_DIR}/../k8s/migrate-job.yaml.tpl"

# Derive a DNS-safe job name from the image tag (strip registry prefix, replace
# non-alphanumeric characters with hyphens, truncate to 63 chars).
TAG=$(echo "$APP_IMAGE" | sed 's/.*://')
JOB_NAME=$(echo "vanilla-migrate-${TAG}" | tr '[:upper:]' '[:lower:]' | tr -cs 'a-z0-9-' '-' | cut -c1-63 | sed 's/-$//')

log()  { echo "[k8s-migrate] $*"; }
err()  { echo "[k8s-migrate] ERROR: $*" >&2; }

cleanup() {
  kubectl delete job/"$JOB_NAME" -n "$NAMESPACE" --ignore-not-found >/dev/null 2>&1 || true
}

log "Applying migration Job: $JOB_NAME (image: $APP_IMAGE)"
export APP_IMAGE JOB_NAME
envsubst '${APP_IMAGE} ${JOB_NAME}' < "$JOB_TEMPLATE" \
  | kubectl apply -f - -n "$NAMESPACE"

log "Waiting up to ${TIMEOUT}s for migration to complete..."
if kubectl wait job/"$JOB_NAME" -n "$NAMESPACE" \
     --for=condition=complete "--timeout=${TIMEOUT}s" 2>/dev/null; then
  log "Migration succeeded."
  cleanup
  exit 0
else
  err "Migration Job failed or timed out."
  err "Inspect logs: kubectl logs -n $NAMESPACE -l job-name=$JOB_NAME"
  err "Schema is unchanged (Flyway group=true rolled back all migrations)."
  err "Fix the migration and re-run before deploying new code."
  cleanup
  exit 1
fi
