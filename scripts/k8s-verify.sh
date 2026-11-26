#!/usr/bin/env bash
# k8s-verify.sh — per-pod JBehave readiness-gate verification for rolling deployments.
#
# For each new pod produced by a rolling update the script:
#   1. Detects pods where ContainersReady=True but Ready=False (app is up, gate is pending).
#   2. Creates a candidate Service targeting only that pod via its pod-template-hash label.
#      publishNotReadyAddresses:true lets the verification Job reach the pod before the gate is set.
#   3. Launches a Kubernetes Job running the black-box verification suite against that Service.
#   4. On Job success: patches the pod's status condition to True, unblocking the rollout.
#      On Job failure: leaves the condition False, blocks the rollout, and exits non-zero.
#   5. Cleans up the candidate Service and Job.
#
# Required env:
#   BB_IMAGE   — fully-qualified blackbox runner image tag (e.g. ghcr.io/bohemian/vanilla-bb:sha-abc)
#
# Optional env (with defaults):
#   NAMESPACE   — Kubernetes namespace (default: default)
#   DEPLOYMENT  — Deployment name (default: vanilla)
#   TIMEOUT     — seconds to wait for each Job / for new pods to appear (default: 300)

set -euo pipefail

NAMESPACE="${NAMESPACE:-default}"
DEPLOYMENT="${DEPLOYMENT:-vanilla}"
TIMEOUT="${TIMEOUT:-300}"
BB_IMAGE="${BB_IMAGE:?BB_IMAGE must be set}"
CONDITION_TYPE="deployment.example.com/verified"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JOB_TEMPLATE="${SCRIPT_DIR}/../k8s/verify-job.yaml.tpl"

log() { echo "[k8s-verify] $*"; }
err() { echo "[k8s-verify] ERROR: $*" >&2; }

# ---------------------------------------------------------------------------
# find_unverified_pods
# Returns pod names where all containers are ready but the Pod is not yet
# Ready — indicating the app readiness probe is passing but the custom
# readiness gate deployment.example.com/verified has not been set to True.
# ---------------------------------------------------------------------------
find_unverified_pods() {
  kubectl get pods -n "$NAMESPACE" \
    -l "app=$DEPLOYMENT" \
    -o json \
    | jq -r '
      .items[]
      | select(
          (.status.conditions // [] | map(select(.type == "ContainersReady")) | .[0].status) == "True"
          and
          (.status.conditions // [] | map(select(.type == "Ready"))           | .[0].status) == "False"
        )
      | .metadata.name'
}

# ---------------------------------------------------------------------------
# create_candidate_service POD_NAME TEMPLATE_HASH
# Creates a ClusterIP Service that selects only the pod with the given
# pod-template-hash. publishNotReadyAddresses:true ensures the Service
# endpoint is registered even though the pod is not yet Ready.
# ---------------------------------------------------------------------------
create_candidate_service() {
  local pod_name="$1"
  local hash="$2"
  local svc_name="vanilla-candidate-${pod_name}"

  if kubectl get service "$svc_name" -n "$NAMESPACE" &>/dev/null; then
    log "Candidate Service $svc_name already exists — reusing"
    return
  fi

  kubectl apply -n "$NAMESPACE" -f - <<EOF
apiVersion: v1
kind: Service
metadata:
  name: ${svc_name}
  labels:
    app: vanilla-candidate
    target-pod: ${pod_name}
spec:
  publishNotReadyAddresses: true
  selector:
    app: ${DEPLOYMENT}
    pod-template-hash: "${hash}"
  ports:
    - name: http
      port: 8080
      targetPort: 8080
EOF
  log "Created candidate Service $svc_name → pod-template-hash=$hash"
}

# ---------------------------------------------------------------------------
# run_verify_job POD_NAME
# Instantiates the Job template for the given pod and waits for completion.
# Returns 0 on success, 1 on failure or timeout.
# ---------------------------------------------------------------------------
run_verify_job() {
  local pod_name="$1"
  local job_name="verify-${pod_name}"

  # Remove any prior Job with the same name (idempotent re-runs).
  kubectl delete job "$job_name" -n "$NAMESPACE" --ignore-not-found=true --wait=true

  # Substitute only our two variables; leave any other $ expressions intact.
  export POD_NAME="$pod_name"
  export BB_IMAGE
  envsubst '${POD_NAME} ${BB_IMAGE}' < "$JOB_TEMPLATE" \
    | kubectl apply -n "$NAMESPACE" -f -
  log "Launched Job $job_name"

  if kubectl wait "job/$job_name" -n "$NAMESPACE" \
       --for=condition=complete \
       --timeout="${TIMEOUT}s" 2>/dev/null; then
    log "Job $job_name completed successfully"
    return 0
  else
    err "Job $job_name failed or timed out after ${TIMEOUT}s"
    # Print the last 100 lines from the runner pod for diagnosis.
    kubectl logs -n "$NAMESPACE" -l "job-name=$job_name" --tail=100 >&2 || true
    return 1
  fi
}

# ---------------------------------------------------------------------------
# patch_verified_condition POD_NAME STATUS
# Sets deployment.example.com/verified to STATUS ("True" or "False") on the
# pod's status.conditions via the /status subresource.
#
# Kubernetes initialises the custom readiness gate condition as absent;
# we may need to add it (first patch) or update it (re-patch).  The
# implementation reads the current conditions array, removes any prior
# entry for this type, then appends the new entry and applies the full
# updated array via a strategic merge patch.
# ---------------------------------------------------------------------------
patch_verified_condition() {
  local pod_name="$1"
  local status="$2"   # "True" or "False"
  local now
  now=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
  local message
  if [ "$status" = "True" ]; then
    message="JBehave post-deploy verification passed"
  else
    message="JBehave post-deploy verification failed"
  fi

  # Build the updated conditions array: remove any prior entry for our type,
  # then append the new condition object.
  local new_condition
  new_condition=$(jq -n \
    --arg type    "$CONDITION_TYPE" \
    --arg status  "$status" \
    --arg now     "$now" \
    --arg message "$message" \
    '{type: $type, status: $status, lastTransitionTime: $now,
      reason: "BlackboxVerification", message: $message}')

  local current_conditions
  current_conditions=$(kubectl get pod "$pod_name" -n "$NAMESPACE" \
    -o jsonpath='{.status.conditions}' || echo '[]')
  [ -z "$current_conditions" ] && current_conditions='[]'

  local updated_conditions
  updated_conditions=$(echo "$current_conditions" \
    | jq --argjson nc "$new_condition" \
        'map(select(.type != $nc.type)) + [$nc]')

  kubectl patch pod "$pod_name" -n "$NAMESPACE" \
    --subresource=status \
    --type=merge \
    -p "{\"status\":{\"conditions\":${updated_conditions}}}"

  log "Patched pod $pod_name: $CONDITION_TYPE=$status"
}

# ---------------------------------------------------------------------------
# cleanup POD_NAME
# Removes the candidate Service and Job created for this pod.
# Uses --ignore-not-found so cleanup is safe to call even if creation failed.
# ---------------------------------------------------------------------------
cleanup() {
  local pod_name="$1"
  kubectl delete service "vanilla-candidate-${pod_name}" \
    -n "$NAMESPACE" --ignore-not-found=true
  kubectl delete job "verify-${pod_name}" \
    -n "$NAMESPACE" --ignore-not-found=true
}

# ---------------------------------------------------------------------------
# Main loop — processes each unverified pod as it appears during rolling update.
#
# With maxSurge:1 / maxUnavailable:0 there is at most one new pod at a time,
# but the loop handles concurrent pods and re-entry after failures.
# Exits 0 when kubectl rollout status reports completion with no pending pods.
# Exits 1 if any pod fails verification.
# ---------------------------------------------------------------------------
main() {
  log "Watching deployment/$DEPLOYMENT in namespace $NAMESPACE for unverified pods..."
  log "Blackbox runner image: $BB_IMAGE"

  local deadline=$(( $(date +%s) + TIMEOUT ))

  while true; do
    mapfile -t PODS < <(find_unverified_pods)

    if [ "${#PODS[@]}" -eq 0 ]; then
      # No pending pods — check if the rollout has fully completed.
      if kubectl rollout status "deployment/$DEPLOYMENT" -n "$NAMESPACE" \
           --timeout=5s &>/dev/null; then
        log "Rollout complete. All pods verified and Ready."
        exit 0
      fi
      if [ "$(date +%s)" -gt "$deadline" ]; then
        err "Timed out after ${TIMEOUT}s waiting for new pods to verify."
        exit 1
      fi
      sleep 3
      continue
    fi

    for POD in "${PODS[@]}"; do
      log "Processing unverified pod: $POD"

      HASH=$(kubectl get pod "$POD" -n "$NAMESPACE" \
        -o jsonpath='{.metadata.labels.pod-template-hash}')

      create_candidate_service "$POD" "$HASH"

      if run_verify_job "$POD"; then
        patch_verified_condition "$POD" "True"
        cleanup "$POD"
        log "Pod $POD is now verified and Ready."
      else
        patch_verified_condition "$POD" "False"
        cleanup "$POD"
        err "Pod $POD failed verification — rolling update is blocked."
        err "Inspect the blackbox report or re-run: kubectl logs -n $NAMESPACE -l target-pod=$POD"
        exit 1
      fi
    done
  done
}

main "$@"
