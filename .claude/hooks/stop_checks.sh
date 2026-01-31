#!/usr/bin/env bash
set -euo pipefail

# Stop hook: run ./gradlew build when Gradle modules have changed.
#
# Hook input (JSON via stdin):
#   stop_hook_active – true when Claude is already continuing from a prior Stop hook.
#                      We skip re-running to avoid infinite build loops.
#
# Exit codes (per Claude Code hooks contract):
#   0 – allow Claude to stop (no changes, or build succeeded)
#   2 – block stopping; stderr is shown to the user and fed back to Claude

cd "${CLAUDE_PROJECT_DIR:-.}"

# Load shared libraries
HOOKS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib/gradle_summary.sh disable=SC1091
source "$HOOKS_DIR/lib/gradle_summary.sh"
# shellcheck source=lib/hook_logger.sh disable=SC1091
source "$HOOKS_DIR/lib/hook_logger.sh"
setup_hook_logging "stop_checks"

# ── Read hook input ──────────────────────────────────────────────────
input=$(cat)
stop_hook_active=$(echo "$input" | jq -r '.stop_hook_active // false')

if [[ "$stop_hook_active" == "true" ]]; then
	exit 0
fi

# ── Detect Gradle-relevant changes ───────────────────────────────────
gradle_modules=("app-server" "buildSrc" "ui-library")

changed_files=$(git diff --name-only HEAD 2>/dev/null || true)
staged_files=$(git diff --name-only --cached 2>/dev/null || true)
untracked_files=$(git ls-files --others --exclude-standard 2>/dev/null || true)
all_changes=$(printf '%s\n%s\n%s' "$changed_files" "$staged_files" "$untracked_files" | sort -u)

if [[ -z "$all_changes" ]]; then
	exit 0
fi

module_changed=false
for module in "${gradle_modules[@]}"; do
	if echo "$all_changes" | grep -q "^${module}/"; then
		module_changed=true
		break
	fi
done

# Root build files also matter
if echo "$all_changes" | grep -qE '^(build\.gradle\.kts|settings\.gradle\.kts|gradle/libs\.versions\.toml|gradle\.properties)$'; then
	module_changed=true
fi

if [[ "$module_changed" != "true" ]]; then
	exit 0
fi

# ── Run Gradle build ─────────────────────────────────────────────────
if build_output=$(./gradlew build 2>&1); then
	echo "Gradle build passed."
else
	{
		echo "Gradle build failed. Fix the errors and try again:"
		echo ""
		extract_gradle_failure_summary "$build_output"
		echo ""
		echo "Please fix these issues before proceeding."
	} >&2
	exit 2
fi
