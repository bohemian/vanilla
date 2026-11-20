SHELL := /bin/bash
GRADLEW = ./gradlew

# Target-specific defaults
KEEP_DAYS ?= 1
CLUSTER_NAME ?= vanilla
VERSION ?= local

.PHONY: all build clean run test format review hooks audit mutate clean-gh-runs deploy

# Ensure a brew-installable tool is present; installs it automatically if missing.
# Usage: $(call ensure-brew,executable,brew-package)
define ensure-brew
@command -v $(1) >/dev/null 2>&1 \
  || { echo "Installing $(2) via Homebrew..."; brew install $(2); }
endef

all: build

format:
	$(GRADLEW) spotlessApply

build: format
	$(GRADLEW) build

clean:
	$(GRADLEW) clean

run:
	@java -jar $$(find . -name "vanilla-*.jar") --spring.profiles.active=local

test:
	$(GRADLEW) test

review:
	@./scripts/pr-review.sh

package:
	@./scripts/package.sh

# Activate hooks and ensure all hook scripts are executable
hooks:
	chmod +x .githooks/pre-commit .githooks/commit-msg .githooks/pre-push .githooks/post-commit
	git config core.hooksPath .githooks
	@echo "✅ Git hooks activated (pre-commit, commit-msg, pre-push, post-commit)"

# Run OWASP dependency vulnerability scan (slow — downloads NVD data)
audit:
	$(GRADLEW) dependencyCheckAnalyze

# Run PIT mutation testing (slow — reruns tests with code mutations)
mutate:
	$(GRADLEW) pitest

# Deploy to a local k3d (k3s-in-Docker) Kubernetes cluster.
# VERSION=local (default): builds the JAR + Docker image locally and imports it into k3d.
# VERSION=<tag>:           pulls ghcr.io/bohemian/vanilla:<tag> from GitHub Container Registry.
# Postgres is exposed permanently at localhost:5432 via a k3d port mapping (no port-forward needed).
# The app is forwarded to http://localhost:8080 — Ctrl+C to stop.
deploy:
	$(call ensure-brew,k3d,k3d)
	$(call ensure-brew,kubectl,kubectl)
	@k3d cluster get $(CLUSTER_NAME) >/dev/null 2>&1 \
	  || k3d cluster create $(CLUSTER_NAME) \
	       --port "5432:30432@loadbalancer" \
	       --k3s-arg "--disable=traefik@server:0"
	@if [ "$(VERSION)" = "local" ]; then \
	    echo "Building JAR and Docker image..."; \
	    $(GRADLEW) bootJar; \
	    docker build -t vanilla:local .; \
	    k3d image import vanilla:local -c $(CLUSTER_NAME); \
	    IMAGE="vanilla:local"; \
	  else \
	    IMAGE="ghcr.io/bohemian/vanilla:$(VERSION)"; \
	  fi; \
	  kubectl apply -f k8s/postgres.yaml; \
	  kubectl rollout status deployment/postgres --timeout=60s; \
	  sed "s|VANILLA_IMAGE|$$IMAGE|" k8s/app.yaml | kubectl apply -f -; \
	  kubectl rollout status deployment/vanilla --timeout=120s
	@echo ""
	@echo "Postgres → localhost:5432  (user: vanilla  pass: vanilla  db: vanilla)"
	@echo "App      → http://localhost:8080"
	@echo "Debugger → localhost:5005  (attach remote JVM debugger, e.g. IntelliJ Run > Attach to Process)"
	@echo "Ctrl+C to stop forwarding"
	@echo ""
	@kubectl port-forward svc/vanilla 8080:8080 5005:5005

# Delete completed GitHub Actions runs older than KEEP_DAYS days (default: 1).
# Use KEEP_DAYS=0 or KEEP_DAYS=NONE to delete all completed runs.
clean-gh-runs:
	$(call ensure-brew,gh,gh)
	$(call ensure-brew,jq,jq)
	@days="$(KEEP_DAYS)"; \
	[ "$$days" = "NONE" ] && days=0; \
	cutoff=$$(($$days * 86400)); \
	gh run list --limit 100 --status completed --json databaseId,createdAt \
		| jq -r --argjson cutoff "$$cutoff" \
		  '.[] | select(.createdAt < (now - $$cutoff | todate)) | .databaseId' \
		| xargs -P 8 -I {} gh run delete {}
