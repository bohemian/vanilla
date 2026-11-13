SHELL := /bin/bash
GRADLEW = ./gradlew

.PHONY: all build clean run test format

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

package:
	@./scripts/package.sh
