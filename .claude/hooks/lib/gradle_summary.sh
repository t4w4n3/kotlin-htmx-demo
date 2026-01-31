#!/usr/bin/env bash
# gradle_summary.sh - Shared library for extracting concise Gradle failure summaries
#
# Usage: source this file, then call extract_gradle_failure_summary "$output"
#
# Keeps: compilation errors, lint errors, FAILED test blocks, test summary, "What went wrong".
# Strips: task listing, application logs, and other noise.

extract_gradle_failure_summary() {
	local output="$1"
	local has_content=false

	# 1. Compilation errors (e: file.kt:line:col: message)
	local compile_errors
	compile_errors=$(echo "$output" | grep -E '^e: ' || true)
	if [[ -n "$compile_errors" ]]; then
		echo "=== Compilation errors ==="
		echo "$compile_errors"
		echo ""
		has_content=true
	fi

	# 2. ktlint / lint errors
	local lint_errors
	lint_errors=$(echo "$output" | grep -E '^\[standard:' || true)
	if [[ -n "$lint_errors" ]]; then
		echo "=== Lint errors ==="
		echo "$lint_errors"
		echo ""
		has_content=true
	fi

	# 3. Failed test blocks: FAILED line + indented detail lines that follow
	local failed_tests
	failed_tests=$(echo "$output" | awk '/FAILED$/{if(index($0,"> Task")==0){p=1;print;next}} p&&/^[[:space:]]/{print;next} p&&/^$/{print;next} {p=0}' || true)
	if [[ -n "$failed_tests" ]]; then
		echo "=== Failed tests ==="
		echo "$failed_tests"
		echo ""
		has_content=true
	fi

	# 4. Test summary line (N tests completed, M failed)
	local test_summary
	test_summary=$(echo "$output" | grep -E '[0-9]+ tests? completed' || true)
	if [[ -n "$test_summary" ]]; then
		echo "$test_summary"
		has_content=true
	fi

	# 5. "What went wrong" section (up to "Try:" line)
	local what_went_wrong
	what_went_wrong=$(echo "$output" | sed -n '/^\* What went wrong/,/^\* Try/{ /^\* Try/d; p; }' || true)
	if [[ -n "$what_went_wrong" ]]; then
		echo ""
		echo "$what_went_wrong"
		has_content=true
	fi

	# Fallback: if nothing was extracted, show last 20 lines
	if [[ "$has_content" == false ]]; then
		echo "=== Build output (last 20 lines) ==="
		echo "$output" | tail -20
	fi
}
