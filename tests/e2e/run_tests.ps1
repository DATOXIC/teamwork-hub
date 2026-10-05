<#
.SYNOPSIS
    TeamWork Hub - Automated E2E Test Suite PowerShell Runner
.DESCRIPTION
    Runs the 4-Tier E2E test suite across all 7 JSPs with UTF-8 encoding and zero hardcoded paths.
.EXAMPLE
    .\run_tests.ps1
    .\run_tests.ps1 -Tier 1
    .\run_tests.ps1 -Page login
    .\run_tests.ps1 -Strict
#>

[CmdletBinding()]
param (
    [Parameter(Mandatory=$false)]
    [ValidateSet(1, 2, 3, 4)]
    [int]$Tier,

    [Parameter(Mandatory=$false)]
    [string]$Page,

    [Parameter(Mandatory=$false)]
    [switch]$Strict,

    [Parameter(Mandatory=$false)]
    [switch]$VerboseOutput,

    [Parameter(Mandatory=$false)]
    [switch]$Json
)

# Synchronize UTF-8 console output per docs/ai-notes/TEAM_GUARDRAILS.md guardrail
[Console]::OutputEncoding = [System.Text.Encoding]::UTF-8
$OutputEncoding = [System.Text.Encoding]::UTF-8

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RunnerScript = Join-Path $ScriptDir "runner.js"

if (-not (Get-Command node -ErrorAction SilentlyContinue)) {
    Write-Error "Node.js was not found in PATH. Please install Node.js (v18+) to run the E2E test suite."
    exit 1
}

$Arguments = @($RunnerScript)

if ($PSBoundParameters.ContainsKey('Tier')) {
    $Arguments += @("--tier", $Tier)
}

if ($PSBoundParameters.ContainsKey('Page')) {
    $Arguments += @("--page", $Page)
}

if ($Strict) {
    $Arguments += "--strict"
}

if ($VerboseOutput) {
    $Arguments += "--verbose"
}

if ($Json) {
    $Arguments += "--json"
}

& node $Arguments
exit $LASTEXITCODE
