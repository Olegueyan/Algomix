param(
  [string]$BoardsDir = "./Boards",
  [string]$OutputDir = "./Exports",
  [int]$Width = 500,
  [int]$Height = 990,
  [int]$WaitMs = 1800
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$pyScript = Join-Path $scriptDir "render-boards.py"
$venvPython = Join-Path $scriptDir ".venv\Scripts\python.exe"

if (-not (Test-Path $pyScript)) {
  throw "Python renderer not found: $pyScript"
}

if (-not (Test-Path $venvPython)) {
  throw "Virtual environment not found at $venvPython. Run .\setup-venv.ps1 first."
}

& $venvPython $pyScript --boards-dir $BoardsDir --output-dir $OutputDir --width $Width --height $Height --wait-ms $WaitMs
