SHELL := /bin/bash
GRADLEW = ./gradlew

# Target-specific defaults
KEEP_DAYS ?= 1
VERSION ?= local

.PHONY: all build clean run test format review hooks audit mutate clean-gh-runs deploy setup branch

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
	@echo "==> Installing dependencies via Homebrew..."
	@command -v docker >/dev/null 2>&1 \
	  || { echo "Installing Docker..."; brew install --cask docker; }
	@/usr/libexec/java_home -v 21 >/dev/null 2>&1 \
	  || { echo "Installing Java 21 (Temurin)..."; brew install --cask temurin@21; }
	$(call ensure-brew,gh,gh)
	$(call ensure-brew,jq,jq)
	@if ! grep -q 'JAVA_HOME' ~/.zprofile 2>/dev/null; then \
	  echo "export JAVA_HOME=\$$(/usr/libexec/java_home)" >> ~/.zprofile; \
	  echo "✅ Added JAVA_HOME to ~/.zprofile"; \
	fi
	$(MAKE) hooks
	@echo ""
	@echo "✅ Setup complete."
	@echo "   Restart your terminal (or: source ~/.zprofile) for JAVA_HOME to take effect."
	@echo "   Then run: make deploy"

# Interactive prompt to create a Conventional Branch (https://conventionalbranch.org).
# Paste a Jira/Linear title directly — spaces and special chars are converted to hyphens.
branch:
	@echo ""; \
	echo "Create a conventional branch"; \
	echo ""; \
	echo "  1) feat   — new feature"; \
	echo "  2) fix    — bug fix"; \
	echo "  3) chore  — maintenance (deps, docs, config)"; \
	echo "  4) hotfix — urgent production fix"; \
	echo "  5) ai     — AI-assisted change"; \
	echo ""; \
	printf "Type [1-5]: "; read -r n </dev/tty; \
	case $$n in \
	  1) type=feat   ;; \
	  2) type=fix    ;; \
	  3) type=chore  ;; \
	  4) type=hotfix ;; \
	  5) type=ai     ;; \
	  *) echo "Invalid selection '$$n'" >&2; exit 1 ;; \
	esac; \
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
	chmod +x .githooks/pre-commit .githooks/commit-msg .githooks/pre-push .githooks/post-commit .githooks/post-checkout
	git config core.hooksPath .githooks
	@echo "✅ Git hooks activated (pre-commit, commit-msg, pre-push, post-commit, post-checkout)"

# Run OWASP dependency vulnerability scan (slow — downloads NVD data)
audit:
	$(GRADLEW) dependencyCheckAnalyze

# Run PIT mutation testing (slow — reruns tests with code mutations)
mutate:
	$(GRADLEW) pitest

# Deploy locally using Docker Compose.
# VERSION=local (default): builds the JAR and Docker image locally.
# VERSION=<tag>:           pulls ghcr.io/bohemian/vanilla:<tag> from GitHub Container Registry.
# Postgres → localhost:5432, App → http://localhost:8080, Debugger → localhost:5005.
# Ctrl+C to stop.
deploy:
	@if [ "$(VERSION)" = "local" ]; then \
	    echo "Building JAR and Docker image..."; \
	    $(GRADLEW) bootJar; \
	    VANILLA_IMAGE=vanilla:local docker compose up --build --wait; \
	  else \
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
	$(call ensure-brew,gh,gh)
	$(call ensure-brew,jq,jq)
	@days="$(KEEP_DAYS)"; \
	[ "$$days" = "NONE" ] && days=0; \
	cutoff=$$(($$days * 86400)); \
	gh run list --limit 100 --status completed --json databaseId,createdAt \
		| jq -r --argjson cutoff "$$cutoff" \
		  '.[] | select(.createdAt < (now - $$cutoff | todate)) | .databaseId' \
		| xargs -P 8 -I {} gh run delete {}
