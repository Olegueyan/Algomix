#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
venv_dir="${script_dir}/.venv"
requirements="${script_dir}/requirements.txt"

if [[ ! -f "${requirements}" ]]; then
  echo "requirements.txt not found at ${requirements}" >&2
  exit 1
fi

python3 -m venv "${venv_dir}"

venv_python="${venv_dir}/bin/python"
if [[ ! -x "${venv_python}" ]]; then
  echo "Venv python not found at ${venv_python}" >&2
  exit 1
fi

"${venv_python}" -m pip install --upgrade pip
"${venv_python}" -m pip install -r "${requirements}"
"${venv_python}" -m playwright install chromium

echo "Venv ready. Use ./render-boards.sh to export PNGs."
