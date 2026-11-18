# QC Agent Base Contract

## Output format
Return findings as raw JSON objects, one per line. No wrapper array, no markdown, no explanation:
```
{"file":"path/to/File.java","line":42,"line_content":"the full line containing the issue",...}
```
To find the line number, use `grep -n` on the file for the line content.
If no findings, output nothing.
