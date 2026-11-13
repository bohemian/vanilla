SHELL := /bin/bash
GRADLEW = ./gradlew

.PHONY: all build clean run test format review

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
	@git diff main...HEAD | claude "Review this pull request diff. Focus on: bugs or logic errors, security issues, code quality and maintainability, Java/JPA/Spring best practices. Post a concise structured review."

package:
	@./scripts/package.sh
