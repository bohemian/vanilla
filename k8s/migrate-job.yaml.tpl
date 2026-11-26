# Pre-deploy database migration Job.
# Processed by scripts/k8s-migrate.sh using: envsubst '${APP_IMAGE} ${JOB_NAME}'
#
# Variables substituted at runtime:
#   APP_IMAGE — fully-qualified app image (e.g. ghcr.io/bohemian/vanilla:sha-abc123)
#   JOB_NAME  — unique name derived from the image tag (e.g. vanilla-migrate-sha-abc123)
#
# Deploy order enforced by scripts/k8s-migrate.sh:
#   1. Apply this Job and wait for completion  ← migrations committed here
#   2. kubectl apply k8s/app.yaml             ← rolling update starts only after step 1
#   3. scripts/k8s-verify.sh                  ← per-pod BB verification gate
#
# Failure policy: backoffLimit:0 — a migration failure aborts the deploy immediately.
# All migrations run in a single JDBC transaction (FlywayConfig group=true): if any
# migration fails the entire set rolls back and the schema is unchanged.
#
# Roll-forward policy: a successful migration that later proves harmful is corrected by
# a new forward migration, never by reverting to a previous schema version.
apiVersion: batch/v1
kind: Job
metadata:
  name: ${JOB_NAME}
  labels:
    app: vanilla-migrate
spec:
  # No retries — a failed migration must be investigated before retrying.
  backoffLimit: 0
  # Auto-clean completed Jobs after 10 minutes.
  ttlSecondsAfterFinished: 600
  template:
    spec:
      restartPolicy: Never
      containers:
        - name: migrate
          image: ${APP_IMAGE}
          imagePullPolicy: IfNotPresent
          args:
            - --spring.profiles.active=migrate
          env:
            - name: SPRING_DATASOURCE_URL
              value: jdbc:postgresql://postgres:5432/vanilla
            - name: SPRING_DATASOURCE_USERNAME
              valueFrom:
                secretKeyRef:
                  name: db-credentials
                  key: username
            - name: SPRING_DATASOURCE_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: db-credentials
                  key: password
          resources:
            requests:
              memory: 256Mi
              cpu: 100m
            limits:
              memory: 512Mi
