# Claude Code Hooks

This directory contains Claude Code hooks.

## file_processor.sh

PostToolUse hook that runs automatically after each file edit (Edit, Write, MultiEdit).

### How It Works

The hook applies **two steps without duplication**:

1. **Pre-commit hooks** (for ALL files):
   - `trailing-whitespace`: Removes trailing spaces
   - `end-of-file-fixer`: Adds a newline at end of file
   - `markdownlint-cli2 --fix`: Lints and fixes Markdown files
   - `check-yaml`: Validates YAML syntax
   - `check-added-large-files`: Checks file sizes

2. **Complementary tools** (only what is NOT in pre-commit):
   - `.sh/.bash`: `shfmt` (formatting) + `shellcheck` (validation)
   - `.md/.mdx`: `prettier` (additional formatting after markdownlint)

### Execution Flow

```text
Edited file
     │
     ├─→ STEP 1: Pre-commit (all files)
     │   ├─ trailing-whitespace ✓
     │   ├─ end-of-file-fixer ✓
     │   ├─ markdownlint-cli2 (if .md) ✓
     │   └─ check-yaml (if .yaml) ✓
     │
     └─→ STEP 2: Specific tools
         ├─ .sh/.bash → shfmt ✓ → shellcheck ✓
         ├─ .md/.mdx  → prettier ✓
         └─ others    → nothing (already processed)
```

**Benefits**: No duplication, each tool runs exactly once.

### Automatic Corrections

✅ **These issues are corrected automatically and silently**:

- Trailing spaces (trailing-whitespace)
- Missing newline at end of file (end-of-file-fixer)
- Shell formatting (shfmt)
- Markdown formatting (prettier + markdownlint --fix)

**Result**: The file is modified, Claude receives a discrete notification

### Blocking Errors

❌ **These issues block and require manual correction**:

- ShellCheck errors (unquoted variables, incorrect globs, etc.)
- Non-fixable markdownlint errors
- Invalid YAML (check-yaml)
- Files too large (check-added-large-files)

**Result**: The edit is blocked, Claude must fix the errors before continuing

### Behavior

#### Successful auto-fix

```text
✓ Pre-commit auto-fixed: /path/to/file.md

The following corrections were applied automatically:
end-of-file-fixer............................Failed
- hook id: end-of-file-fixer
- files were modified by this hook

Fixing /path/to/file.md
```

Claude receives this informational message but continues without being blocked.

#### Non-fixable errors

```text
✗ Pre-commit found issues in /path/to/file.yaml that cannot be auto-fixed:

check-yaml...................................Failed
- hook id: check-yaml
- exit code: 1

Invalid YAML syntax at line 10

Please fix these issues manually.
```

Claude is blocked and must fix the errors manually before continuing.

## Configuration

The hook is configured in `.claude/settings.json`:

```json
{
  "hooks": {
    "PostToolUse": [
      {
        "matcher": "Edit|Write|MultiEdit",
        "hooks": [
          {
            "type": "command",
            "command": "\"$CLAUDE_PROJECT_DIR\"/.claude/hooks/file_processor.sh"
          }
        ]
      }
    ]
  }
}
```

## Benefits

1. **Guaranteed quality**: All files edited by Claude respect project standards
2. **Automatic corrections**: No need to manually run formatters
3. **Immediate feedback**: Claude is notified instantly of issues
4. **Consistency**: Same rules as local Git pre-commit hooks and CI

## Troubleshooting

### The hook doesn't run

Check that:

- `pre-commit` is installed: `mise install`
- Hooks are installed: `pre-commit install`
- The script is executable: `chmod +x .claude/hooks/file_processor.sh`

### False positives

If the hook blocks incorrectly, check:

- The `.pre-commit-config.yaml` configuration
- Exclusions (e.g., `test/assets/`)
- Markdownlint rules in `.markdownlint.yaml`

### Temporary deactivation

To temporarily disable the hook, comment out the section in `.claude/settings.json`:

```json
{
  "hooks": {
    "PostToolUse": []
  }
}
```
