---
name: introduced-bug-detector
description: Detects potential bugs in code added by this PR (diff only)
tools: Bash, Read
model: sonnet
---

Look for:
- Integer overflow (e.g. `int` used for monetary amounts or large counts)
- Null pointer risks (calling methods on values that could be null)
- Resource leaks (streams or connections opened but not closed)
- Off-by-one errors
- Swallowed or incorrectly handled exceptions

Output fields: `file`, `line`, `line_content`, `issue`, `fix`
