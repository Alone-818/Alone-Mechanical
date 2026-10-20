$ErrorActionPreference='Stop'
$taskRoot=[IO.Path]::GetFullPath((Split-Path $PSScriptRoot -Parent))
$taskSource=Join-Path $taskRoot 'run-gametest/world'
$taskPreview=Join-Path $taskRoot 'run-separator-check'
$taskDestination=Join-Path $taskPreview 'saves/Separator Preview'
if(!(Test-Path -LiteralPath (Join-Path $taskSource 'level.dat'))){throw 'Run runGameTestServer first to prepare the dedicated test world.'}
if(!(Test-Path -LiteralPath $taskDestination)){
    [IO.Directory]::CreateDirectory($taskDestination)|Out-Null
    foreach($entry in Get-ChildItem -LiteralPath $taskSource){if($entry.Name -ne 'session.lock'){Copy-Item -LiteralPath $entry.FullName -Destination $taskDestination -Recurse}}
}
[IO.File]::WriteAllText((Join-Path $taskPreview 'options.txt'),"lang:zh_cn`nguiScale:3`nrenderDistance:4`nsimulationDistance:5`nsoundCategory_master:0.0`npauseOnLostFocus:false`nenableVsync:false`nmaxFps:30`n",[Text.UTF8Encoding]::new($false))
Write-Output "Separator preview world ready: $taskDestination"
