# PowerShell build and packaging script for WorkTracker

Write-Host "=============================================" -ForegroundColor Green
Write-Host "Building and Packaging WorkTracker Desktop App" -ForegroundColor Green
Write-Host "=============================================" -ForegroundColor Green

# 1. Setup JAVA_HOME
$env:JAVA_HOME = "C:\Program Files\Java\jdk-25"
$jpackagePath = "C:\Program Files\Java\jdk-25\bin\jpackage.exe"

# Verify jpackage exists
if (-not (Test-Path $jpackagePath)) {
    Write-Error "jpackage was not found at $jpackagePath. Please verify Java JDK installation."
    exit 1
}

# 2. Compile and package the Fat JAR using local Maven
Write-Host "Step 1: Running Maven Clean Package..." -ForegroundColor Cyan
.\tools\maven\apache-maven-3.9.8\bin\mvn.cmd clean package -DskipTests

if ($LASTEXITCODE -ne 0) {
    Write-Error "Maven package failed. Exiting build."
    exit $LASTEXITCODE
}

# Create dist directory
if (Test-Path "dist") {
    Remove-Item "dist" -Recurse -Force
}
New-Item -ItemType Directory -Path "dist" | Out-Null

# 3. Package Standalone App Image using jpackage
Write-Host "Step 2: Bundling Standalone Windows Executable via jpackage..." -ForegroundColor Cyan

& $jpackagePath --type app-image `
                --name "WorkTracker" `
                --input "target" `
                --main-jar "work-tracker-app-1.0.0-jar-with-dependencies.jar" `
                --main-class "com.worktracker.Launcher" `
                --dest "dist" `
                --vendor "DeepMind Pair Programmer" `
                --app-version "1.0.0"

if ($LASTEXITCODE -ne 0) {
    Write-Error "jpackage bundling failed. Exiting build."
    exit $LASTEXITCODE
}

Write-Host "=============================================" -ForegroundColor Green
Write-Host "SUCCESS! Portable App-Image built successfully." -ForegroundColor Green
Write-Host "Location: dist\WorkTracker" -ForegroundColor Green
Write-Host "Run dist\WorkTracker\WorkTracker.exe to start." -ForegroundColor Green
Write-Host "=============================================" -ForegroundColor Green
