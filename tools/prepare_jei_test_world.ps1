$ErrorActionPreference = 'Stop'
$taskRoot = [IO.Path]::GetFullPath((Split-Path $PSScriptRoot -Parent))
$taskSource = Join-Path $taskRoot 'run-gametest/world'
$taskDestination = [IO.Path]::GetFullPath((Join-Path $taskRoot 'run/saves/Mechanical JEI Test'))
if (!$taskDestination.StartsWith($taskRoot + [IO.Path]::DirectorySeparatorChar)) { throw 'Test world must stay inside the workspace' }
if (!(Test-Path (Join-Path $taskSource 'level.dat'))) { throw 'Run runGameTestServer first to generate the test world' }
if (!(Test-Path -LiteralPath $taskDestination)) {
    [IO.Directory]::CreateDirectory((Split-Path $taskDestination -Parent)) | Out-Null
    Copy-Item -LiteralPath $taskSource -Destination $taskDestination -Recurse
}
Write-Output "JEI test world ready: $taskDestination"
