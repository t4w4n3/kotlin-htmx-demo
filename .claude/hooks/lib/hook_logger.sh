#!/usr/bin/env bash
# hook_logger.sh - Shared tee logging for Claude Code hooks.
#
# Usage (at the top of a hook script, after HOOKS_DIR is set):
#   source "$HOOKS_DIR/lib/hook_logger.sh"
#   setup_hook_logging "my_hook_name"
#
# This mirrors stdout and stderr to .claude/hooks/logs/<name>.log
# with timestamped invocation headers and 512 KB log rotation.

setup_hook_logging() {
	local hook_name="$1"
	local log_dir="$HOOKS_DIR/logs"
	local log_file="$log_dir/${hook_name}.log"
	local max_log_size=$((512 * 1024))

	mkdir -p "$log_dir"

	# Rotate log when it exceeds max size (keep one backup)
	if [[ -f "$log_file" ]] && [[ "$(stat -f%z "$log_file" 2>/dev/null || stat -c%s "$log_file" 2>/dev/null || echo 0)" -ge "$max_log_size" ]]; then
		mv "$log_file" "$log_file.1"
	fi

	# Write a header for this invocation
	{
		echo "---"
		echo "[$(date -u '+%Y-%m-%dT%H:%M:%SZ')] $hook_name invoked"
	} >>"$log_file"

	# Save log file path for log_context calls
	_HOOK_LOG_FILE="$log_file"

	# Duplicate stdout and stderr: originals stay on fd 1/2, copies go to the log.
	# Uses process substitution so tee runs in a subshell; the main script is unaffected.
	exec \
		1> >(tee -a "$log_file") \
		2> >(tee -a "$log_file" >&2)
}

# Append extra context lines to the log without printing to stdout/stderr.
# Usage: log_context "file_path=/some/file.kt"
log_context() {
	echo "  $1" >>"${_HOOK_LOG_FILE:?setup_hook_logging not called}"
}
