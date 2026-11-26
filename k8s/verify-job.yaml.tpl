# Template for the per-pod JBehave verification Job.
# Processed by scripts/k8s-verify.sh using: envsubst '${POD_NAME} ${BB_IMAGE}'
#
# Variables substituted at runtime:
#   POD_NAME  — name of the candidate pod being verified (e.g. vanilla-6d7b9f4c8-xk2vp)
#   BB_IMAGE — fully-qualified BB runner image (e.g. ghcr.io/bohemian/vanilla-bb:sha-abc123)
#
# The Job targets the per-pod candidate Service (vanilla-candidate-${POD_NAME}) which
# selects only this pod and has publishNotReadyAddresses:true so the runner can
# reach the pod before its readiness gate is satisfied.
apiVersion: batch/v1
kind: Job
metadata:
  name: verify-${POD_NAME}
  labels:
    app: vanilla-verify
    target-pod: ${POD_NAME}
spec:
  # No retries — if verification fails the pod stays unverified and traffic is blocked.
  backoffLimit: 0
  # Auto-clean completed Jobs after 5 minutes to avoid object accumulation.
  ttlSecondsAfterFinished: 300
  template:
    spec:
      restartPolicy: Never
      serviceAccountName: vanilla-deploy
      containers:
        - name: bb-runner
          image: ${BB_IMAGE}
          imagePullPolicy: IfNotPresent
          env:
            - name: BASE_URL
              value: "http://vanilla-candidate-${POD_NAME}:8080"
          command:
            - ./gradlew
            - verifyTest
            - --no-daemon
            - -PbaseUrl=http://vanilla-candidate-${POD_NAME}:8080
          resources:
            requests:
              memory: 512Mi
              cpu: 200m
            limits:
              memory: 768Mi
              cpu: 500m
