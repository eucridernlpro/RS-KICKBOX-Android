$ErrorActionPreference = "Stop"
Write-Host "RS 3D Tower Hardware Probe" -ForegroundColor Cyan

function Show-Cmd($cmd) {
  try {
    & $cmd --version
  } catch {
    Write-Host "$cmd not found"
  }
}

Write-Host ""
Write-Host "Windows:"
Get-ComputerInfo | Select-Object WindowsProductName,WindowsVersion,OsArchitecture,CsTotalPhysicalMemory

Write-Host ""
Write-Host "GPU:"
try {
  nvidia-smi --query-gpu=name,memory.total,driver_version --format=csv,noheader
} catch {
  Write-Host "nvidia-smi not found. NVIDIA driver/GPU not detected through CLI." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Python:"
try { python --version } catch { Write-Host "Python not found" -ForegroundColor Yellow }

Write-Host ""
Write-Host "Git:"
try { git --version } catch { Write-Host "Git not found" -ForegroundColor Yellow }

Write-Host ""
Write-Host "Blender:"
$blenderCandidates = @(
  "C:\Program Files\Blender Foundation\Blender 4.5\blender.exe",
  "C:\Program Files\Blender Foundation\Blender 4.4\blender.exe",
  "C:\Program Files\Blender Foundation\Blender 4.3\blender.exe"
)
$found = $false
foreach ($b in $blenderCandidates) {
  if (Test-Path $b) {
    & $b --version | Select-Object -First 1
    $found = $true
    break
  }
}
if (-not $found) { Write-Host "Blender not detected in standard paths" -ForegroundColor Yellow }

Write-Host ""
Write-Host "Copy the full output of this window back to ChatGPT when requested." -ForegroundColor Green
Read-Host "Press Enter to close"
