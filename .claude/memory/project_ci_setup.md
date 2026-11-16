---
name: CI and developer tooling setup
description: Summary of the CI pipeline, automated PR review, and developer tooling configured for this project
type: project
---

The following was set up across the project:

## GitHub Actions CI pipeline (`.github/workflows/build.yml`)
- **Apply formatting** — runs `spotlessApply` under Java 21 (google-java-format incompatible with Java 25), auto-commits formatting fixes to the branch if any files changed
- **Test** — runs `./gradlew test` under Java 25
- **Build** — runs `./gradlew assemble` under Java 25
- **Upload artifact** — uploads `build/libs/*.jar`, retained 14 days
- **PR review** — runs on `pull_request` events only; loads prompt from `.claude/agents/pr-review.md` (strips YAML frontmatter via awk), passes to `anthropics/claude-code-action@v1` with `ANTHROPIC_API_KEY` secret

## Automated Claude PR review
- Configured via `anthropics/claude-code-action@v1`
- Auth: static `ANTHROPIC_API_KEY` stored as GitHub Actions secret (WIF was attempted but incompatible with the action's own OIDC flow)
- Prompt lives in `.claude/agents/pr-review.md` — single source of truth for both CI and local `make review`
- **Auto-commits** trivial unambiguous fixes (System.out.println, typos) directly to the PR branch
- **Posts inline suggestion blocks** for significant changes (refactoring, style, judgment calls) using GitHub suggestion syntax for one-click apply
- Requires `contents: write`, `id-token: write`, `pull-requests: write` permissions

## Code formatting (Spotless)
- Plugin: `com.diffplug.spotless` version `7.0.3`
- Formatter: `googleJavaFormat()` (default version)
- Must run under Java 21 — google-java-format uses internal JDK APIs removed in Java 25
- JVM args added to `gradle.properties` with `--add-opens` for jdk.compiler internals

## Branch protection
- Main branch requires a PR before merging
- Required approvals: set to 0 (user is sole developer, can't self-approve)
- Force push to main is allowed — use `git push --force-with-lease origin main` when needed

## Makefile targets
- `make format` — runs `spotlessApply`
- `make build` — runs format then `./gradlew build`
- `make test` — runs `./gradlew test`
- `make review` — local PR review via `scripts/pr-review.sh` (curl to Anthropic API, reads prompt from agent file, requires `ANTHROPIC_API_KEY` env var)
- `make run` — runs the jar with `--spring.profiles.active=local`
- `make package` — runs `scripts/package.sh`

## Local `make review` implementation
- Uses curl + jq (at `/usr/local/bin/jq`) to call Anthropic API directly
- Required because Claude Code native binary requires macOS 13+; this machine is on macOS 11 (Big Sur)
- Script: `scripts/pr-review.sh`
- PATH explicitly set in script to include `/usr/local/bin` for jq

## Key files
- `.claude/agents/pr-review.md` — Claude PR review agent (prompt source of truth)
- `.github/workflows/build.yml` — CI pipeline
- `scripts/pr-review.sh` — local review script
- `build.gradle` — Spotless config
- `gradle.properties` — JVM args for Spotless + dependency versions
