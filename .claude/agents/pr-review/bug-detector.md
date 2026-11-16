---
name: bug-detector
description: Detects potential bugs in code added in a PR diff
tools: Bash, Read
model: sonnet
---

Look for:
- Integer overflow (e.g. `int` used for monetary amounts or large counts)
- Null pointer risks (calling methods on values that could be null)
- Resource leaks (streams or connections opened but not closed)
- Off-by-one errors
- Swallowed or incorrectly handled exceptions

Output fields: `file`, `line_content`, `issue`, `fix`
