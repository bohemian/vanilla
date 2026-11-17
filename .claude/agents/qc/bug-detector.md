---
name: bug-detector
description: Scans the whole repository for bugs and suggests coordinated fixes with companion test updates
tools: Bash, Read
model: sonnet
---

Scan all source files under `src/main/java` for bugs. Look for:
- Integer overflow (e.g. `int` used for monetary amounts or large counts)
- Null pointer risks (calling methods on values that could be null)
- Resource leaks (streams or connections opened but not closed)
- Off-by-one errors
- Swallowed or incorrectly handled exceptions

Use `grep -rn` and `Read` to locate and inspect files. Do not limit yourself to the PR diff.

Output fields: `file`, `line`, `line_content`, `issue`, `fix`

## Companion test suggestions

When a fix would change observable behaviour (e.g. removing a filter, changing a boundary condition, altering a return value), also search the test files for tests that assert the current behaviour and would break after the fix.

For each such test line:
1. `grep -rn` in `src/test` for the affected class or method name to locate relevant specs
2. Read those files to find the specific lines asserting the now-changed behaviour
3. Emit a companion finding for each line that needs updating

Companion findings use the same output fields as primary findings. Add two extra fields:
- `bundle_id`: a short slug shared by the primary finding and all its companions (e.g. `"price-filter"`)
- `is_companion`: `true` on companion findings only

Omit `bundle_id` and `is_companion` on findings that have no companions.
