#!/bin/bash
set -e

export PATH="/usr/local/bin:/opt/homebrew/bin:$PATH"

if [ -z "$ANTHROPIC_API_KEY" ]; then
  echo "Error: ANTHROPIC_API_KEY is not set" >&2
  exit 1
fi

DIFF=$(git diff main...HEAD)

if [ -z "$DIFF" ]; then
  echo "No changes compared to main."
  exit 0
fi

# Strip YAML frontmatter from agent file to get the system prompt
SYSTEM=$(awk 'BEGIN{n=0} /^---/{n++; next} n>=2{print}' .claude/agents/pr-review.md)

curl -s https://api.anthropic.com/v1/messages \
  -H "x-api-key: $ANTHROPIC_API_KEY" \
  -H "anthropic-version: 2023-06-01" \
  -H "content-type: application/json" \
  -d "$(jq -n \
    --arg system "$SYSTEM" \
    --arg diff "$DIFF" \
    '{
      model: "claude-sonnet-4-6",
      max_tokens: 2048,
      system: $system,
      messages: [{role: "user", content: ("Review this pull request diff:\n\n" + $diff)}]
    }')" | jq -r '.content[0].text'
