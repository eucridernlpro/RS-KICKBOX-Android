$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

if (-not (Test-Path ".venv")) {
  python -m venv .venv
}
& ".\.venv\Scripts\Activate.ps1"
python -m pip install --upgrade pip
pip install -r requirements.txt

if (-not $env:RS3D_WORK_DIR) {
  $env:RS3D_WORK_DIR = Join-Path $Root "work"
}
Write-Host "Starting RS 3D API on http://127.0.0.1:8787" -ForegroundColor Cyan
uvicorn main:app --host 127.0.0.1 --port 8787
