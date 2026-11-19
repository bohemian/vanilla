SHELL := /bin/bash
GRADLEW = ./gradlew

.PHONY: all build clean run test format review hooks audit mutate clean-gh-runs

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

# Delete completed GitHub Actions runs older than DAYS days (default: 1).
# Use DAYS=0 or DAYS=all to delete all completed runs.
DAYS ?= 1
clean-gh-runs:
	@days="$(DAYS)"; \
	[ "$$days" = "all" ] && days=0; \
	cutoff=$$(($$days * 86400)); \
	gh run list --limit 100 --status completed --json databaseId,createdAt \
		| jq -r --argjson cutoff "$$cutoff" \
		  '.[] | select(.createdAt < (now - $$cutoff | todate)) | .databaseId' \
		| xargs -I {} gh run delete {}
