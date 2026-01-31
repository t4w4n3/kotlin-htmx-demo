#!/usr/bin/env bash
# file_processor.sh - PostToolUse hook for processing edited files
#
# Triggers on: Edit, Write, MultiEdit
# Processes files based on extension:
#   - .sh: Format with shfmt, validate with shellcheck (issues fed back to Claude)
#   - .md: Format with prettier and markdownlint-cli2 --fix
#   - .yml/.yaml: Validate with yamllint
#   - .kt/.kts: Validate with gradle build (includes ktlintCheck, -Werror, -Wextra, tests)
#   - .ts/.tsx/.js/.jsx: Validate with bun run check (typecheck, test, lint, format)
#   - Dockerfile*/*Dockerfile*: Validate with trivy config (via mise)
#
# Exit codes:
#   0 - Success or no action needed
#   2 - Blocking error (shellcheck/ktlint issues) - stderr shown to Claude

set -euo pipefail

# Load shared libraries
HOOKS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib/gradle_summary.sh disable=SC1091
source "$HOOKS_DIR/lib/gradle_summary.sh"
# shellcheck source=lib/hook_logger.sh disable=SC1091
source "$HOOKS_DIR/lib/hook_logger.sh"
setup_hook_logging "file_processor"

# Track tools used for success message
TOOLS_USED=()

# Activate mise to ensure tools like shellcheck, shfmt, prettier are available
if command -v mise &>/dev/null; then
	eval "$(mise activate bash 2>/dev/null)" || true
fi

main() {
	local input file_path extension

	# Read JSON input from stdin
	input=$(cat)

	# Extract file path from JSON using jq
	file_path=$(echo "$input" | jq -r '.tool_input.file_path // empty')
	log_context "file_path=$file_path"

	# Exit silently if no file path provided
	if [[ -z "$file_path" ]]; then
		exit 0
	fi

	# Exit silently if file doesn't exist
	if [[ ! -f "$file_path" ]]; then
		exit 0
	fi

	# Get project root (git repository root)
	local project_root
	project_root=$(git rev-parse --show-toplevel 2>/dev/null) || exit 0

	# Resolve file path to absolute path
	local resolved_path
	resolved_path=$(cd "$(dirname "$file_path")" && pwd)/$(basename "$file_path")

	# Exit silently if file is outside the project
	if [[ "$resolved_path" != "$project_root"/* ]]; then
		exit 0
	fi

	# Get file extension (lowercase for case-insensitive matching)
	extension="${file_path##*.}"
	extension="${extension,,}"

	# Check if file is a Dockerfile (by basename pattern, not extension)
	local basename
	basename=$(basename "$file_path")

	if [[ "$basename" == *Dockerfile* ]] || [[ "$basename" == *dockerfile* ]]; then
		process_dockerfile "$file_path"
	else
		case "$extension" in
		sh | bash)
			process_shell_file "$file_path"
			;;
		md | mdx)
			process_markdown_file "$file_path"
			;;
		yml | yaml)
			process_yaml_file "$file_path"
			;;
		kt | kts)
			process_kotlin_file "$file_path"
			;;
		*)
			exit 0
			;;
		esac
	fi

	# Print success message with tools used
	if [[ ${#TOOLS_USED[@]} -gt 0 ]]; then
		local IFS=", "
		echo "Verified with ${TOOLS_USED[*]}"
	else
		echo "No verification tools available"
	fi

	exit 0
}

process_shell_file() {
	local file_path="$1"
	local shellcheck_output
	local shellcheck_exit

	# Format with shfmt (in-place, ignore errors if shfmt not available)
	if command -v shfmt &>/dev/null; then
		shfmt -w "$file_path" 2>/dev/null || true
		TOOLS_USED+=("shfmt")
	fi

	# Validate with shellcheck
	if command -v shellcheck &>/dev/null; then
		# Run shellcheck and capture output
		# Using || true to prevent set -e from exiting on shellcheck failure
		shellcheck_output=$(shellcheck --format=tty "$file_path" 2>&1) || shellcheck_exit=$?

		if [[ -n "${shellcheck_exit:-}" ]] && [[ "$shellcheck_exit" -ne 0 ]]; then
			# Output issues to stderr so Claude sees them and can fix them
			{
				echo "ShellCheck found issues in $file_path:"
				echo ""
				echo "$shellcheck_output"
				echo ""
				echo "Please fix these issues before proceeding."
			} >&2
			exit 2
		fi
		TOOLS_USED+=("shellcheck")
	fi
}

process_markdown_file() {
	local file_path="$1"
	local lint_output
	local lint_exit

	# Format with prettier (in-place)
	if command -v prettier &>/dev/null; then
		prettier --write "$file_path" 2>/dev/null || true
		TOOLS_USED+=("prettier")
	fi

	# Fix with markdownlint-cli2 (in-place), then check for remaining issues
	if command -v markdownlint-cli2 &>/dev/null; then
		# First pass: auto-fix what can be fixed
		markdownlint-cli2 --fix "$file_path" >/dev/null 2>&1 || true

		# Second pass: check for remaining issues that couldn't be auto-fixed
		lint_output=$(markdownlint-cli2 "$file_path" 2>&1) || lint_exit=$?

		if [[ -n "${lint_exit:-}" ]] && [[ "$lint_exit" -ne 0 ]]; then
			# Output issues to stderr so Claude sees them and can fix them
			{
				echo "markdownlint-cli2 found issues in $file_path that could not be auto-fixed:"
				echo ""
				echo "$lint_output"
				echo ""
				echo "Please fix these issues manually."
			} >&2
			exit 2
		fi
		TOOLS_USED+=("markdownlint-cli2")
	fi
}

process_yaml_file() {
	local file_path="$1"
	local yamllint_output
	local yamllint_exit
	local config_dir
	local config_args=()

	# Find .yamllint.yaml config by walking up from file's directory
	config_dir=$(
		cd "$(dirname "$file_path")" &&
			while [[ ! -f ".yamllint.yaml" ]] && [[ "$PWD" != "/" ]]; do
				cd ..
			done &&
			[[ -f ".yamllint.yaml" ]] && pwd
	)

	if [[ -n "$config_dir" ]]; then
		config_args=(-c "$config_dir/.yamllint.yaml")
	fi

	# Validate with yamllint
	if command -v yamllint &>/dev/null; then
		yamllint_output=$(yamllint "${config_args[@]}" --strict "$file_path" 2>&1) || yamllint_exit=$?

		if [[ -n "${yamllint_exit:-}" ]] && [[ "$yamllint_exit" -ne 0 ]]; then
			# Output issues to stderr so Claude sees them and can fix them
			{
				echo "yamllint found issues in $file_path:"
				echo ""
				echo "$yamllint_output"
				echo ""
				echo "Please fix these issues before proceeding."
			} >&2
			exit 2
		fi
		TOOLS_USED+=("yamllint")
	fi
}

process_kotlin_file() {
	local file_path="$1"
	local backend_dir
	local build_output
	local build_exit

	# Find the backend directory by looking for gradlew from the file's location
	backend_dir=$(cd "$(dirname "$file_path")" && while [[ ! -f "gradlew" ]] && [[ "$PWD" != "/" ]]; do cd ..; done && [[ -f "gradlew" ]] && pwd)

	if [[ -z "$backend_dir" ]]; then
		# No gradlew found, skip silently
		return 0
	fi

	# Run gradle build (includes ktlintCheck, compilation with -Werror/-Wextra, and tests)
	build_output=$(cd "$backend_dir" && ./gradlew build 2>&1) || build_exit=$?

	if [[ -n "${build_exit:-}" ]] && [[ "$build_exit" -ne 0 ]]; then
		# Quiet mode: extract only actionable information to avoid cluttering context
		{
			echo "Gradle build failed for $file_path:"
			echo ""
			extract_gradle_failure_summary "$build_output"
			echo ""
			echo "Please fix these issues before proceeding."
		} >&2
		exit 2
	fi
	TOOLS_USED+=("gradle")
}

process_dockerfile() {
	local file_path="$1"
	local trivy_output
	local trivy_exit

	# Check if mise is available to run trivy
	if ! command -v mise &>/dev/null; then
		return 0
	fi

	# Validate with trivy config (misconfiguration scanning)
	trivy_output=$(mise exec trivy -- trivy config --exit-code 1 "$file_path" 2>&1) || trivy_exit=$?

	if [[ -n "${trivy_exit:-}" ]] && [[ "$trivy_exit" -ne 0 ]]; then
		# Output issues to stderr so Claude sees them and can fix them
		{
			echo "Trivy found misconfigurations in $file_path:"
			echo ""
			echo "$trivy_output"
			echo ""
			echo "Please fix these issues before proceeding."
		} >&2
		exit 2
	fi
	TOOLS_USED+=("trivy")
}

main
