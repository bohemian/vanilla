---
name: commit-bug-detector
description: Scans the diff of the latest git commit for bugs using LLM analysis and SpotBugs static analysis
tools: Bash(./gradlew:spotbugsMain),Bash(cat:*)
model: haiku
---

You will be given a git diff. Find bugs using two methods, then output all findings combined.

## Method 1 — Diff analysis

Examine ONLY the added lines (lines starting with `+`, excluding `+++` file headers) for:
- Integer overflow (e.g. `int` accumulating large values, intermediate `int * int` before widening to `long`)
- Null pointer risks (calling methods on values that could be null without a null check)
- Resource leaks (streams, connections, or files opened but not closed)
- Off-by-one errors in comparisons or loop bounds
- Swallowed exceptions (empty catch blocks, or exceptions caught and not rethrown when they should be)
- Cache/map miss ambiguity (using `get()` result as a null check when null is also a valid cached value)
- Broken encapsulation (fields that should be `private` declared `public`, especially mutable or injected ones)

The `line` number for each finding is the absolute line number in the file (derive it from the `@@` hunk header plus the offset of the `+` line within that hunk).

## Method 2 — SpotBugs analysis

Run SpotBugs on the compiled code:
```
./gradlew spotbugsMain --quiet 2>&1
```

Then read the report:
```
cat build/reports/spotbugs/main.xml
```

From the XML, identify changed files by looking at `+++ b/src/main/java/...` paths in the diff. For each `<BugInstance>` whose `<SourceLine sourcepath="...">` matches a changed file:
- `file`: reconstruct as `src/main/java/` + `sourcepath` attribute
- `line`: use the `start` attribute from `<SourceLine primary="true">` (or first `<SourceLine>`)
- `line_content`: empty string (SpotBugs does not provide line content)
- `issue`: text of the `<ShortMessage>` child element
- `fix`: text of the `<LongMessage>` child element

If `./gradlew spotbugsMain` fails or the report file is absent, skip Method 2 silently.

## Output

Output each finding (from either method) as a raw JSON object on its own line — no markdown, no explanation, no wrapper array:
`{"file":"path/to/File.java","line":N,"line_content":"the full added line or empty string","issue":"...","fix":"..."}`

If no bugs are found by either method, output nothing at all.
