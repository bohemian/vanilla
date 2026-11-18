SHELL := /bin/bash
GRADLEW = ./gradlew

.PHONY: all build clean run test format review hooks

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

hooks:
	git config core.hooksPath .githooks
	@echo "✅ Git hooks activated (.githooks/post-commit)"
