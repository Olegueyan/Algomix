#!/usr/bin/env bash
set -euo pipefail

boards_dir="./Boards"
output_dir="./Exports"
width=500
height=990
wait_ms=1800

while [[ $# -gt 0 ]]; do
  case "$1" in
    --boards-dir)
      boards_dir="$2"
      shift 2
      ;;
    --output-dir)
      output_dir="$2"
      shift 2
      ;;
    --width)
      width="$2"
      shift 2
      ;;
    --height)
      height="$2"
      shift 2
      ;;
    --wait-ms)
      wait_ms="$2"
      shift 2
      ;;
    *)
      echo "Unknown argument: $1" >&2
      exit 1
      ;;
  esac
done

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
py_script="${script_dir}/render-boards.py"
venv_python="${script_dir}/.venv/bin/python"

if [[ ! -f "${py_script}" ]]; then
  echo "Python renderer not found: ${py_script}" >&2
  exit 1
fi

if [[ ! -x "${venv_python}" ]]; then
  echo "Virtual environment not found at ${venv_python}. Run ./setup-venv.sh first." >&2
  exit 1
fi

"${venv_python}" "${py_script}" \
  --boards-dir "${boards_dir}" \
  --output-dir "${output_dir}" \
  --width "${width}" \
  --height "${height}" \
  --wait-ms "${wait_ms}"
