# Project: vanilla

Java 25 / Spring Boot 4 service. Build tool: Gradle. Test framework: Spock.

## Rules

ALWAYS run `./gradlew test` and confirm it passes before committing anything.

When committing on behalf of the user, ALWAYS use `CLAUDE_SESSION=1 git commit ...`.
The post-commit hook will emit bug findings between `BUG_FINDINGS_JSON_BEGIN` and
`BUG_FINDINGS_JSON_END` markers. If any findings appear, present them to the user,
ask whether to apply fixes, apply them with the Edit tool if yes, then run
`SKIP_BUG_SCAN=1 git commit --amend --no-edit` to fold the fixes into the commit.
