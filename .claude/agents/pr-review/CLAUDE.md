# PR Review Agent Base Contract

## Getting the diff
```bash
git diff origin/main...HEAD
```
Analyse only **added lines** (lines starting with `+`). Ignore removed lines and context lines.

## Output format
Return findings as raw JSON objects, one per line. No wrapper array, no markdown, no explanation:
```
{"file":"path/to/File.java","line":42,"line_content":"the full added line containing the issue",...}
```
To find the line number, use `grep -n` on the file for the line content, or read it from the diff hunk headers (`@@ -a,b +c,d @@` where `c` is the starting line).
If no findings, output nothing.

## Scope values (used by java-best-practices)
- `"suggestion"` — small, self-contained fix appropriate for this PR
- `"future-pr"` — larger refactoring that warrants its own PR
