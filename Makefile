SHELL := /bin/bash
GRADLEW = ./gradlew

# Target-specific defaults
KEEP_DAYS ?= 1

.PHONY: all build clean run test format review hooks audit mutate clean-gh-runs

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
		| xargs -P 10 -I {} gh run delete {}
