#!/usr/bin/env bash
# Interactive release script.
# Analyses commits since the last release tag, asks Claude to recommend a
# semver bump, prompts the user to confirm or override, then updates
# build.gradle, commits, tags, and pushes.
set -euo pipefail

# ── locate claude binary (same approach as post-commit hook) ────────────────
CLAUDE_BIN=$(find "$HOME/.nvm/versions/node" -maxdepth 3 -name "claude" 2>/dev/null \
  | awk -F/ '{n=$(NF-2); gsub(/^v/,"",n); print n, $0}' \
  | sort -t. -k1,1n -k2,2n -k3,3n \
  | tail -1 | cut -d' ' -f2-)
[ -n "$CLAUDE_BIN" ] && export PATH="$(dirname "$CLAUDE_BIN"):$PATH"
export PATH="/usr/local/bin:/opt/homebrew/bin:$PATH"

if ! command -v claude &>/dev/null; then
  echo "❌ claude CLI not found — cannot run release advisor" >&2
  exit 1
fi
if ! command -v jq &>/dev/null; then
  echo "❌ jq not found — run 'make setup'" >&2
  exit 1
fi

# ── semver helpers ───────────────────────────────────────────────────────────
# next_version <current> <bump>  →  prints next version string
next_version() {
  local ver="${1#v}"
  local bump=$2
  local major minor patch
  IFS='.' read -r major minor patch <<< "$ver"
  case "$bump" in
    major) echo "$((major + 1)).0.0" ;;
    minor) echo "$major.$((minor + 1)).0" ;;
    patch) echo "$major.$minor.$((patch + 1))" ;;
  esac
}

# validate_input <input> <current>  →  prints bump category or exits with error message
validate_input() {
  local input="${1#v}"
  local current="${2#v}"
  local c_major c_minor c_patch i_major i_minor i_patch
  IFS='.' read -r c_major c_minor c_patch <<< "$current"
  IFS='.' read -r i_major i_minor i_patch <<< "$input"

  if ! [[ "$i_major" =~ ^[0-9]+$ && "$i_minor" =~ ^[0-9]+$ && "$i_patch" =~ ^[0-9]+$ ]]; then
    echo "❌ '$input' is not a valid semver (expected MAJOR.MINOR.PATCH)" >&2
    exit 1
  fi

  if [[ $i_major -gt $c_major ]]; then
    local exp
    exp="$((c_major + 1)).0.0"
    if [[ "$input" != "$exp" ]]; then
      echo "❌ Next major release must be v$exp" >&2; exit 1
    fi
    echo "major"
  elif [[ $i_major -eq $c_major && $i_minor -gt $c_minor ]]; then
    local exp
    exp="$c_major.$((c_minor + 1)).0"
    if [[ "$input" != "$exp" ]]; then
      echo "❌ Next minor release must be v$exp" >&2; exit 1
    fi
    echo "minor"
  elif [[ $i_major -eq $c_major && $i_minor -eq $c_minor && $i_patch -gt $c_patch ]]; then
    local exp
    exp="$c_major.$c_minor.$((c_patch + 1))"
    if [[ "$input" != "$exp" ]]; then
      echo "❌ Next patch release must be v$exp" >&2; exit 1
    fi
    echo "patch"
  else
    echo "❌ v$input is not greater than current v$current" >&2; exit 1
  fi
}

# ── determine current version ────────────────────────────────────────────────
LAST_TAG=$(git describe --tags --abbrev=0 2>/dev/null || echo "")
if [[ -z "$LAST_TAG" ]]; then
  CURRENT_VERSION="0.0.0"
  COMMIT_RANGE=""   # all commits
  echo ""
  echo "No previous releases found — this will be the first release."
else
  CURRENT_VERSION="${LAST_TAG#v}"
  COMMIT_RANGE="$LAST_TAG..HEAD"
fi

# ── collect commits ──────────────────────────────────────────────────────────
if [[ -n "$COMMIT_RANGE" ]]; then
  COMMITS=$(git log "$COMMIT_RANGE" --pretty=format:"%s" 2>/dev/null || true)
else
  COMMITS=$(git log --pretty=format:"%s" | head -50)
fi

if [[ -z "$COMMITS" ]]; then
  echo "Nothing to release — no commits since $LAST_TAG."
  exit 0
fi

echo ""
echo "Commits since ${LAST_TAG:-the beginning}:"
if [[ -n "$COMMIT_RANGE" ]]; then
  git log "$COMMIT_RANGE" --oneline
else
  git log --oneline | head -20
fi
echo ""

# ── ask Claude for a recommendation ─────────────────────────────────────────
echo "⏳ Analysing commits..."
SYSTEM=$(awk 'BEGIN{n=0} /^---/{n++; next} n>=2{print}' \
  .claude/agents/release-advisor/release-advisor.md)

RESPONSE=$(printf "Current version: %s\n\nCommits:\n%s" \
    "$CURRENT_VERSION" "$COMMITS" \
  | claude -p --model haiku \
      --system-prompt "$SYSTEM" 2>/dev/null \
  | grep -Eo '\{[^}]+\}' | head -1 || true)

if [[ -z "$RESPONSE" ]]; then
  echo "⚠️  Claude did not return a recommendation — defaulting to patch bump"
  BUMP="patch"
  REASON="(manual fallback)"
else
  BUMP=$(echo "$RESPONSE"   | jq -r '.bump'   2>/dev/null || echo "patch")
  REASON=$(echo "$RESPONSE" | jq -r '.reason' 2>/dev/null || echo "")
fi

SUGGESTED=$(next_version "$CURRENT_VERSION" "$BUMP")

# ── present recommendation ───────────────────────────────────────────────────
echo "Recommendation: $BUMP bump → v$SUGGESTED"
[[ -n "$REASON" ]] && echo "Reason: $REASON"
echo ""

# ── prompt user ──────────────────────────────────────────────────────────────
while true; do
  printf "Press Enter to release v%s, or type a version number: " "$SUGGESTED"
  read -r USER_INPUT </dev/tty

  if [[ -z "$USER_INPUT" ]]; then
    NEW_VERSION="$SUGGESTED"
    break
  fi

  # Strip optional 'v' prefix for validation
  CLEANED="${USER_INPUT#v}"
  # validate_input exits on error, prints bump category on success
  CATEGORY=$(validate_input "$CLEANED" "$CURRENT_VERSION") && {
    NEW_VERSION="$CLEANED"
    break
  }
done

TAG="v$NEW_VERSION"
echo ""
echo "Releasing $TAG..."

# ── update build.gradle ──────────────────────────────────────────────────────
# Strip any -SNAPSHOT suffix and set the release version
sed -i '' "s/^version = .*/version = '$NEW_VERSION'/" build.gradle
echo "  • build.gradle → version = '$NEW_VERSION'"

# ── commit, tag, push ────────────────────────────────────────────────────────
git add build.gradle
SKIP_BUG_SCAN=1 git commit -m "chore(release): $TAG"
git tag "$TAG"
git push origin HEAD
git push origin "$TAG"

echo ""
echo "✅ $TAG released."
echo "   The release workflow will publish Docker, JARs, and openapi.json."
