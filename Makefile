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
	@git diff main...HEAD | claude --agent pr-review

package:
	@./scripts/package.sh
