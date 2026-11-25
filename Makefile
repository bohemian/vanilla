SHELL := /bin/bash -o pipefail
GRADLEW = ./gradlew

# Filter noisy Gradle informational lines (daemon lifecycle, deprecation hints, incubating notices).
# pipefail above ensures the pipeline exit code reflects Gradle's exit code, not grep's.
GRADLE_QUIET = 2>&1 | grep -Ev "^To honour the JVM|^Daemon will be stopped|\[Incubating\]|^Deprecated Gradle|--warning-mode all|^Consider enabling configuration cache|^For more on this"

# Target-specific defaults
KEEP_DAYS ?= 1
VERSION ?= local

.PHONY: all build clean run test bdd format review hooks audit mutate clean-gh-runs deploy setup branch release

# Ensure a brew-installable tool is present; installs it automatically if missing.
# Usage: $(call ensure-brew,executable,brew-package)
define ensure-brew
@command -v $(1) >/dev/null 2>&1 \
  || { echo "Installing $(2) via Homebrew..."; brew install $(2); }
endef

# Install all dependencies and activate git hooks.
# Requires Homebrew (https://brew.sh). Xcode Command Line Tools must be
# installed first manually: xcode-select --install
setup:
	@echo "==> Checking dependencies"
	@command -v docker >/dev/null 2>&1 \
	  || { echo "    Installing Docker..."; brew install --cask docker; }
	@/usr/libexec/java_home -v 21 >/dev/null 2>&1 \
	  || { echo "    Installing Java 21 (Temurin)..."; brew install --cask temurin@21; }
	$(call ensure-brew,gh,gh)
	$(call ensure-brew,jq,jq)
	@echo "==> Configuring JAVA_HOME"
	@if ! grep -q 'JAVA_HOME' ~/.zprofile 2>/dev/null; then \
	  echo "export JAVA_HOME=\$$(/usr/libexec/java_home)" >> ~/.zprofile; \
	  echo "    Added JAVA_HOME to ~/.zprofile"; \
	fi
	@echo "==> Activating git hooks"
	$(MAKE) hooks
	@echo ""
	@echo "✅ Setup complete."
	@echo "   Restart your terminal (or: source ~/.zprofile) for JAVA_HOME to take effect."
	@echo "   Then run: make deploy"

# Interactive prompt to create a Conventional Branch (https://conventionalbranch.org).
# Branch types are read from .branch-types (format: type|description).
# Paste a Jira/Linear title directly — spaces and special chars are converted to hyphens.
branch:
	@if [ -f .branch-types ]; then \
	  DATA=$$(cat .branch-types); \
	else \
	  DATA=$$(printf 'feat|new feature\nfix|bug fix\nchore|maintenance (deps, docs, config)\nhotfix|urgent production fix\nai|AI-assisted change'); \
	fi; \
	echo ""; \
	echo "Create a conventional branch"; \
	echo ""; \
	printf '%s\n' "$$DATA" | awk -F'|' '/^[[:space:]]*#/{next} /^[[:space:]]*$$/{next} \
	  {i++; printf "  %d) %-8s — %s\n", i, $$1, $$2}'; \
	COUNT=$$(printf '%s\n' "$$DATA" | awk -F'|' '/^[[:space:]]*#/{next} /^[[:space:]]*$$/{next} {i++} END{print i+0}'); \
	echo ""; \
	printf "Type [1-$$COUNT]: "; read -r n </dev/tty; \
	echo "$$n" | grep -qE '^[0-9]+$$' || { echo "Invalid selection '$$n'" >&2; exit 1; }; \
	[ "$$n" -ge 1 ] && [ "$$n" -le "$$COUNT" ] \
	  || { echo "Invalid selection '$$n'" >&2; exit 1; }; \
	type=$$(printf '%s\n' "$$DATA" | awk -F'|' -v sel="$$n" '/^[[:space:]]*#/{next} /^[[:space:]]*$$/{next} {i++} i==sel{print $$1; exit}'); \
	echo ""; \
	printf "Description: "; read -r desc </dev/tty; \
	slug=$$(echo "$$desc" \
	  | tr '[:upper:]' '[:lower:]' \
	  | tr -cs 'a-z0-9.' '-' \
	  | sed 's/^[-.]*//; s/[-.]*$$//'); \
	[ -z "$$slug" ] && echo "Description cannot be empty" >&2 && exit 1; \
	branch="$$type/$$slug"; \
	echo ""; \
	printf "Create branch \"%s\"? [Y/n] " "$$branch"; read -r confirm </dev/tty; \
	case "$$confirm" in [nN]*) echo "Aborted." && exit 0 ;; esac; \
	git checkout -b "$$branch"

# Analyse commits since last release tag, suggest a semver bump via Claude,
# confirm with the user, update build.gradle, commit, tag, and push.
# The release.yml CI workflow triggers on the pushed tag to publish all artefacts.
release:
	@echo "==> Analysing commits since last tag"
	@bash scripts/release.sh

all: build

format:
	@echo "==> Applying Spotless formatting"
	@$(GRADLEW) spotlessApply $(GRADLE_QUIET)

build: format
	@echo "==> Running full build"
	@$(GRADLEW) build $(GRADLE_QUIET)

clean:
	@echo "==> Cleaning build outputs"
	@$(GRADLEW) clean $(GRADLE_QUIET)

run:
	@echo "==> Starting app"
	@java -jar $$(find . -name "vanilla-*.jar") --spring.profiles.active=local

test:
	@echo "==> Running unit tests"
	@$(GRADLEW) test $(GRADLE_QUIET)
	$(MAKE) bdd

# Build the app, start it in Docker, run JBehave BDD tests, then tear down.
# Self-contained: no running app required beforehand.
bdd:
	@echo "==> Tearing down any existing services"
	docker compose down >/dev/null 2>&1; true
	@echo "==> Building JAR"
	@$(GRADLEW) bootJar $(GRADLE_QUIET)
	@echo "==> Building Docker image"
	docker build -t vanilla:bdd . -q
	@echo "==> Starting services"
	VANILLA_IMAGE=vanilla:bdd docker compose up --no-build --wait --wait-timeout 180 >/dev/null \
	  || { echo "==> App failed to become healthy — container logs:"; docker compose logs app; docker compose down >/dev/null 2>&1; exit 1; }
	@echo "==> Running BDD tests"
	@$(GRADLEW) bddTest $(GRADLE_QUIET); EXIT=$$?; docker compose down >/dev/null 2>&1; exit $$EXIT

review:
	@echo "==> Running local PR review"
	@./scripts/pr-review.sh

package:
	@echo "==> Packaging application"
	@./scripts/package.sh

# Activate hooks and ensure all hook scripts are executable
hooks:
	@echo "==> Activating git hooks"
	chmod +x .githooks/pre-commit .githooks/commit-msg .githooks/pre-push .githooks/post-commit .githooks/post-checkout
	git config core.hooksPath .githooks
	@echo "    pre-commit, commit-msg, pre-push, post-commit, post-checkout"

# Run OWASP dependency vulnerability scan (slow — downloads NVD data)
audit:
	@echo "==> Running OWASP dependency vulnerability scan"
	@$(GRADLEW) dependencyCheckAnalyze $(GRADLE_QUIET)

# Run PIT mutation testing (slow — reruns tests with code mutations)
mutate:
	@echo "==> Running PIT mutation tests"
	@$(GRADLEW) pitest $(GRADLE_QUIET)

# Deploy locally using Docker Compose.
# VERSION=local (default): builds the JAR and Docker image locally.
# VERSION=<tag>:           pulls ghcr.io/bohemian/vanilla:<tag> from GitHub Container Registry.
# Postgres → localhost:5432, App → http://localhost:8080, Debugger → localhost:5005.
# Ctrl+C to stop.
deploy:
	@if [ "$(VERSION)" = "local" ]; then \
	    echo "==> Building JAR and Docker image"; \
	    $(GRADLEW) bootJar $(GRADLE_QUIET); \
	    echo "==> Starting services (local build)"; \
	    VANILLA_IMAGE=vanilla:local docker compose up --build --wait; \
	  else \
	    echo "==> Starting services (image: ghcr.io/bohemian/vanilla:$(VERSION))"; \
	    VANILLA_IMAGE=ghcr.io/bohemian/vanilla:$(VERSION) docker compose up --no-build --wait; \
	  fi
	@echo ""
	@echo "Postgres → localhost:5432  (user: vanilla  pass: vanilla  db: vanilla)"
	@echo "App      → http://localhost:8080"
	@echo "Debugger → localhost:5005  (attach remote JVM debugger, e.g. IntelliJ Run > Attach to Process)"
	@echo ""
	@echo "Run 'docker compose down' to stop."

# Delete completed GitHub Actions runs older than KEEP_DAYS days (default: 1).
# Use KEEP_DAYS=0 or KEEP_DAYS=NONE to delete all completed runs.
clean-gh-runs:
	@echo "==> Deleting completed GitHub Actions runs older than $(KEEP_DAYS) day(s)"
	$(call ensure-brew,gh,gh)
	$(call ensure-brew,jq,jq)
	@days="$(KEEP_DAYS)"; \
	[ "$$days" = "NONE" ] && days=0; \
	cutoff=$$(($$days * 86400)); \
	gh run list --limit 100 --status completed --json databaseId,createdAt \
		| jq -r --argjson cutoff "$$cutoff" \
		  '.[] | select(.createdAt < (now - $$cutoff | todate)) | .databaseId' \
		| xargs -P 8 -I {} gh run delete {}
