#!/usr/bin/env bash
#
# Recreate the Python virtual environment (.venv) for dme_access_time.py
# on a target server.
#
# Usage:
#   ./setup_venv.sh
#
# Optional environment variables:
#   PYTHON   Python interpreter to use (default: python3)
#
# The .venv directory is intentionally NOT checked into git; this script
# rebuilds it from requirements.txt.

set -euo pipefail

# Move to the directory containing this script.
cd "$(dirname "$0")"

PYTHON="${PYTHON:-python3}"
VENV_DIR=".venv"

print_install_hint() {
    echo "ERROR: '$PYTHON' not found on PATH." >&2
    echo >&2
    echo "Python 3 is required but does not appear to be installed." >&2
    echo "Install it, then re-run this script. Suggested command for your OS:" >&2
    echo >&2

    local os
    os="$(uname -s)"

    if [ "$os" = "Darwin" ]; then
        echo "  brew install python3" >&2
    elif [ "$os" = "Linux" ]; then
        if [ -r /etc/os-release ]; then
            # shellcheck disable=SC1091
            . /etc/os-release
        fi
        case "${ID:-}${ID_LIKE:-}" in
            *rhel*|*fedora*|*centos*|*rocky*|*almalinux*)
                if command -v dnf >/dev/null 2>&1; then
                    echo "  sudo dnf install -y python3" >&2
                else
                    echo "  sudo yum install -y python3" >&2
                fi
                ;;
            *debian*|*ubuntu*)
                echo "  sudo apt-get update && sudo apt-get install -y python3 python3-venv" >&2
                ;;
            *suse*)
                echo "  sudo zypper install -y python3" >&2
                ;;
            *)
                echo "  Use your distribution's package manager to install 'python3' (and the venv module)." >&2
                ;;
        esac
    else
        echo "  Install Python 3 from https://www.python.org/downloads/" >&2
    fi

    echo >&2
    echo "If Python is installed under a different name/path, set PYTHON, e.g.:" >&2
    echo "  PYTHON=python3.14 ./setup_venv.sh" >&2
}

if ! command -v "$PYTHON" >/dev/null 2>&1; then
    print_install_hint
    exit 1
fi

echo "Using interpreter: $("$PYTHON" --version 2>&1) ($(command -v "$PYTHON"))"

if [ -d "$VENV_DIR" ]; then
    echo "Existing $VENV_DIR found; removing it for a clean rebuild."
    rm -rf "$VENV_DIR"
fi

echo "Creating virtual environment in $VENV_DIR ..."
"$PYTHON" -m venv "$VENV_DIR"

# shellcheck disable=SC1091
source "$VENV_DIR/bin/activate"

echo "Upgrading pip ..."
python -m pip install --upgrade pip

echo "Installing dependencies from requirements.txt ..."
python -m pip install -r requirements.txt

echo
echo "Virtual environment is ready."
echo "  Activate with : source $VENV_DIR/bin/activate"
echo "  Run script    : python dme_access_time.py --help"
