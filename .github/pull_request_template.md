## What

<!-- One or two sentences describing what this PR does. -->

## Why

<!-- The motivation or ticket link. Why is this change needed? -->

## How

<!-- Key design decisions or implementation notes. Skip if obvious from the diff. -->

## Test plan

<!-- How did you verify this works? Unit tests, manual steps, screenshots, etc. -->

## Checklist

- [ ] Tests pass (`./gradlew test`)
- [ ] No new SpotBugs / PMD warnings introduced
- [ ] API changes are reflected in OpenAPI (Springdoc auto-generates from annotations)
- [ ] DB migrations are backward-compatible (old app can still run against new schema)
