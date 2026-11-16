# PR Review Agent Base Contract

All PR review agents share the following conventions:

## Getting the diff
```bash
git diff origin/main...HEAD
```
Analyse only **added lines** (lines starting with `+`). Ignore removed lines and context lines.

## Output format
Return findings as raw JSON objects, one per line. No wrapper array, no markdown, no explanation — just the objects:
```
{"file":"path/to/File.java","line_content":"the full added line that contains the issue",...}
```
If no findings, output nothing.

## Scope values (used by java-best-practices)
- `"suggestion"` — small, self-contained fix appropriate for this PR
- `"future-pr"` — larger refactoring that warrants its own PR
