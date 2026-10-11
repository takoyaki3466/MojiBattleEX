param(
    [ValidateSet('portable', 'installer', 'all')]
    [string]$Type = 'all'
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$task = switch ($Type) {
    'portable'  { ':lwjgl3:jpackagePortableZip' }
    'installer' { ':lwjgl3:jpackageWindowsExe' }
    'all'       { ':lwjgl3:packageRelease' }
}

Push-Location $projectRoot
try {
    & .\gradlew.bat $task
    if ($LASTEXITCODE -ne 0) { throw "Gradle packaging failed with exit code $LASTEXITCODE" }
} finally {
    Pop-Location
}

Write-Host "Packaging completed: lwjgl3\build\jpackage" -ForegroundColor Green
