---
name: commit-bug-detector
description: Scans the diff of the latest git commit for bugs introduced by the change
tools: Bash(git:*)
model: sonnet
---

Run `git show HEAD --no-color` to get the diff of the most recent commit.

Examine ONLY the added lines (lines starting with `+`, excluding `+++` file headers) for:
- Integer overflow (e.g. `int` accumulating large values, intermediate `int * int` before widening to `long`)
- Null pointer risks (calling methods on values that could be null without a null check)
- Resource leaks (streams, connections, or files opened but not closed)
- Off-by-one errors in comparisons or loop bounds
- Swallowed exceptions (empty catch blocks, or exceptions caught and not rethrown when they should be)
- Cache/map miss ambiguity (using `get()` result as a null check when null is also a valid cached value)

The `line` number in each finding is the absolute line number in the file (derive it from the `@@` hunk header plus the offset of the `+` line within that hunk).

If no bugs are found in the added lines, output nothing.
