# vanilla

Spring Boot 4 / Java service. Gradle build, Spock tests, Flyway migrations, Lombok, full CI/CD pipeline.

## Prerequisites

[Homebrew](https://brew.sh) is required. Everything else is installed by `make setup`.

Xcode Command Line Tools must be installed manually before running setup (required for `git`):

```
xcode-select --install
```

## Setup

```
make setup
```

Installs Docker, Java 21 (Temurin), `gh` CLI, and `jq` via Homebrew; writes `JAVA_HOME` to `~/.zprofile`; and activates git hooks. Restart your terminal after setup.

## Local development

| Command              | Description                                                     |
|----------------------|-----------------------------------------------------------------|
| `make deploy`        | Start the service with Docker Compose                           |
| `make test`          | Run the test suite                                              |
| `make build`         | Format and build                                                |
| `make format`        | Apply Spotless formatting                                       |
| `make hooks`         | (Re-)activate git hooks                                         |
| `make audit`         | OWASP dependency vulnerability scan (slow — downloads NVD data) |
| `make mutate`        | PIT mutation testing (slow)                                     |
| `make clean`         | Clean build outputs                                             |
| `make clean-gh-runs` | Delete old completed CI runs from GitHub                        |

`make deploy` starts three services and prints connection details:

| Service      | Address                                                            |
|--------------|--------------------------------------------------------------------|
| Postgres     | `localhost:5432` (user: `vanilla`, pass: `vanilla`, db: `vanilla`) |
| App          | `http://localhost:8080`                                            |
| JVM debugger | `localhost:5005`                                                   |

Run `docker compose down` to stop. To deploy a published image instead of building locally:

```
make deploy VERSION=sha-a1b2c3d
```

API docs (Swagger UI) are available at `http://localhost:8080/swagger-ui/index.html`.

## CI/CD pipeline

Every push and pull request runs the following steps in order:

| Step                    | What it does                                                                                                                                                                   |
|-------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Spotless**            | Applies Google Java Format; commits any changes back to the branch automatically                                                                                               |
| **Static analysis**     | SpotBugs + PMD on compiled bytecode — advisory findings only, never blocks the build                                                                                           |
| **Test**                | Full Spock test suite (unit + integration via Testcontainers/Postgres)                                                                                                         |
| **Coverage gate**       | JaCoCo line coverage must be ≥ 80%                                                                                                                                             |
| **Migration compat**    | PR-only; fires when the PR adds new migration files — caps Flyway at the base branch's highest version and verifies the app starts and handles requests against the old schema |
| **Contract tests**      | Spring Cloud Contract — `continue-on-error` while SCC 4.x / Spring Framework 7 incompatibility exists                                                                          |
| **Checklist auto-tick** | Automatically ticks the SpotBugs/PMD and Tests boxes in the PR description                                                                                                     |
| **Build**               | Assembles the bootJar                                                                                                                                                          |
| **OpenAPI spec**        | Boots the app against H2 in-memory DB, captures `/v3/api-docs` as `openapi.json`, and uploads it as the `openapi-spec` CI artifact                                             |

Additional jobs that run in parallel:

| Job                  | Trigger                              | What it does                                                            |
|----------------------|--------------------------------------|-------------------------------------------------------------------------|
| **describe-pr**      | PR opened                            | Generates What / Why / How / Test plan in the PR description via Claude |
| **pr-review**        | Every PR push                        | Posts automated code review comments via Claude                         |
| **dependency-audit** | Weekly + PRs labelled `dependencies` | OWASP NVD vulnerability scan                                            |

## Published artifacts

On every PR merge to `main` the publish pipeline runs and produces:

| Artifact        | Registry                | Coordinate                                                                |
|-----------------|-------------------------|---------------------------------------------------------------------------|
| Service bootJar | GitHub Packages (Maven) | `com.vanilla:vanilla:1.0.0-SNAPSHOT`                                      |
| Client JAR      | GitHub Packages (Maven) | `com.vanilla:vanilla-client:1.0.0-SNAPSHOT`                               |
| Docker image    | GHCR                    | `ghcr.io/bohemian/vanilla:latest` (also tagged by version and commit SHA) |
| OpenAPI spec    | GitHub Release asset    | `openapi.json` on release `v<version>-sha-<sha>`                          |

The client JAR is a generated typed Java client using `java.net.http.HttpClient` with no Spring dependency. Add it to any JVM project:

```groovy
implementation 'com.vanilla:vanilla-client:1.0.0-SNAPSHOT'
```

This gives you `ProductControllerApi`, `QuoteControllerApi`, and the model classes `Product`, `Quote`, `QuoteParams`.
