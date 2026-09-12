#!/usr/bin/env bash
#
# Run dme_access_time.py using configuration from an environment file.
#
# The Python script reads every parameter from an environment variable (a CLI
# flag, if passed, overrides it). This wrapper simply loads the env file and runs
# the script, so secrets are passed via the environment and NEVER appear on the
# command line (nothing sensitive is visible in `ps aux` / /proc). Safe for cron.
#
# Usage:
#   ./dme_access_time.sh [extra args passed through to dme_access_time.py]
#
# Configuration is loaded from an env file (default: .dme_access_time.env in this
# directory). Override the location with DME_ENV_FILE:
#
#   DME_ENV_FILE=/etc/dme/.dme_access_time.env ./dme_access_time.sh
#
# Any extra arguments are forwarded to the Python script and override env vars,
# e.g. a one-off dry run:
#
#   ./dme_access_time.sh --dry-run
#
set -euo pipefail

# Move to the directory containing this script.
cd "$(dirname "$0")"

ENV_FILE="${DME_ENV_FILE:-.dme_access_time.env}"
VENV_DIR=".venv"

if [ ! -f "$ENV_FILE" ]; then
    echo "ERROR: environment file '$ENV_FILE' not found." >&2
    echo "Create it from the template:" >&2
    echo "  cp dme_access_time.env.example .dme_access_time.env" >&2
    echo "  chmod 600 .dme_access_time.env" >&2
    exit 1
fi

# Warn if the env file is more permissive than owner-only (it holds secrets).
if command -v stat >/dev/null 2>&1; then
    perms="$(stat -c '%a' "$ENV_FILE" 2>/dev/null || stat -f '%Lp' "$ENV_FILE" 2>/dev/null || echo '')"
    case "$perms" in
        600|400|"") : ;;
        *) echo "WARNING: '$ENV_FILE' permissions are $perms; recommend 'chmod 600 $ENV_FILE'." >&2 ;;
    esac
fi

if [ ! -d "$VENV_DIR" ]; then
    echo "ERROR: virtual environment '$VENV_DIR' not found. Run ./setup_venv.sh first." >&2
    exit 1
fi

# Load configuration (the env file uses 'export', so the variables reach the
# Python process). Secrets stay in the environment and are not passed as CLI args.
# shellcheck disable=SC1090
set -a
source "$ENV_FILE"
set +a

# shellcheck disable=SC1091
source "$VENV_DIR/bin/activate"

exec python dme_access_time.py "$@"
