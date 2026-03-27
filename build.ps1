param(
    [switch]$SkipTests
)

$scriptPath = Join-Path $PSScriptRoot 'scripts\build\build.ps1'
if (-not (Test-Path $scriptPath)) {
    throw "未找到构建脚本: $scriptPath"
}

& $scriptPath @PSBoundParameters
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}
