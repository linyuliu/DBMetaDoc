param(
    [switch]$SkipTests
)

$ErrorActionPreference = 'Stop'

function Invoke-NativeCommand {
    param(
        [scriptblock]$Command,
        [string]$Description
    )

    & $Command
    if ($LASTEXITCODE -ne 0) {
        throw "$Description failed with exit code $LASTEXITCODE"
    }
}

function Enable-Utf8ConsoleIfNeeded {
    $isWindowsPlatform = $env:OS -eq 'Windows_NT'
    if (-not $isWindowsPlatform) {
        return
    }

    $currentCodePage = (& cmd /c chcp) -replace '[^\d]', ''
    if ($currentCodePage -ne '65001') {
        chcp 65001 > $null
    }

    [Console]::InputEncoding = [System.Text.UTF8Encoding]::new($false)
    [Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)

    $env:JAVA_TOOL_OPTIONS = (($env:JAVA_TOOL_OPTIONS, '-Dfile.encoding=UTF-8') -join ' ').Trim()
    $env:MAVEN_OPTS = (($env:MAVEN_OPTS, '-Dfile.encoding=UTF-8') -join ' ').Trim()
}

Enable-Utf8ConsoleIfNeeded

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path

Push-Location (Join-Path $repoRoot 'dbmetadoc-web')
try {
    Invoke-NativeCommand -Description 'Frontend build' -Command { npm run build }
} finally {
    Pop-Location
}

$mvnArgs = @('-q', '-pl', 'dbmetadoc-app', '-am', 'clean', 'package')
if ($SkipTests) {
    $mvnArgs += '-DskipTests'
}
Invoke-NativeCommand -Description 'Maven package' -Command { mvn @mvnArgs } 
