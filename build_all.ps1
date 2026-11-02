# ==============================================================================
# HyperCharge Control Hub - Unified Project Build Script
# Builds APK + KernelSU All-In-One ZIP + Module-Only ZIP in a single step
# Author: Rohit Sharma
# ==============================================================================

$root = $PSScriptRoot
if (-not $root) { $root = Get-Location }

$distDir = Join-Path $root "dist"
if (!(Test-Path $distDir)) {
    New-Item -ItemType Directory -Force -Path $distDir | Out-Null
}

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  HYPERCHARGE CONTROL HUB - UNIFIED BUILD SYSTEM" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# Step 1: Compile Android App
Write-Host "`n[1/3] Compiling Android Companion App..." -ForegroundColor Yellow
Push-Location $root
$gradleResult = .\gradlew.bat assembleDebug
if ($LASTEXITCODE -ne 0) {
    Write-Error "Gradle build failed!"
    Pop-Location
    exit 1
}
Pop-Location

$apkPath = Join-Path $root "app\build\outputs\apk\debug\app-debug.apk"
if (!(Test-Path $apkPath)) {
    Write-Error "Compiled APK not found at $apkPath"
    exit 1
}

$destApk = Join-Path $distDir "HyperCharge_Control_Hub_Build-08.apk"
Copy-Item -Path $apkPath -Destination $destApk -Force
Write-Host "  [OK] APK compiled: HyperCharge_Control_Hub_Build-08.apk" -ForegroundColor Green

# Step 2: Package KernelSU All-In-One Module (with embedded APK)
Write-Host "`n[2/3] Packaging All-In-One Module ZIP..." -ForegroundColor Yellow
$moduleDir = Join-Path $root "module"
$allInOneZip = Join-Path $distDir "HyperCharge_AllInOne_Module_Build-08.zip"
if (Test-Path $allInOneZip) { Remove-Item -Force $allInOneZip }

Copy-Item -Path $apkPath -Destination (Join-Path $moduleDir "HyperCharge_Control_Hub.apk") -Force

Push-Location $moduleDir
tar.exe -a -c -f $allInOneZip META-INF customize.sh service.sh rohit_daemon.sh module.prop mode.conf HyperCharge_Control_Hub.apk
Pop-Location

Remove-Item -Path (Join-Path $moduleDir "HyperCharge_Control_Hub.apk") -Force
Write-Host "  [OK] All-In-One ZIP created: HyperCharge_AllInOne_Module_Build-08.zip" -ForegroundColor Green

# Step 3: Package Module-Only ZIP (without embedded APK)
Write-Host "`n[3/3] Packaging Module-Only ZIP..." -ForegroundColor Yellow
$moduleOnlyZip = Join-Path $distDir "HyperCharge_ModuleOnly_Build-08.zip"
if (Test-Path $moduleOnlyZip) { Remove-Item -Force $moduleOnlyZip }

Push-Location $moduleDir
tar.exe -a -c -f $moduleOnlyZip META-INF customize.sh service.sh rohit_daemon.sh module.prop mode.conf
Pop-Location
Write-Host "  [OK] Module-Only ZIP created: HyperCharge_ModuleOnly_Build-08.zip" -ForegroundColor Green

Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host "  UNIFIED BUILD COMPLETED SUCCESSFULLY!" -ForegroundColor Green
Write-Host "  Output Directory: $distDir" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
Get-ChildItem $distDir | Select-Object Name, Length, LastWriteTime | Format-Table -AutoSize
