$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$venvDir = Join-Path $scriptDir ".venv"
$requirements = Join-Path $scriptDir "requirements.txt"

if (-not (Test-Path $requirements)) {
  throw "requirements.txt not found at $requirements"
}

python -m venv $venvDir

$venvPython = Join-Path $venvDir "Scripts\python.exe"
if (-not (Test-Path $venvPython)) {
  throw "Venv python not found at $venvPython"
}

& $venvPython -m pip install --upgrade pip
& $venvPython -m pip install -r $requirements
& $venvPython -m playwright install chromium

Write-Output "Venv ready. Use .\render-boards.ps1 to export PNGs."
