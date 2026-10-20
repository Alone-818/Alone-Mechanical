$ErrorActionPreference='Stop'
$taskRoot=Split-Path $PSScriptRoot -Parent
$taskSource=Join-Path $taskRoot 'run-gametest/world'
$taskTarget=Join-Path $taskRoot 'run-pump-check/saves/Pump Preview'
if(!(Test-Path -LiteralPath (Join-Path $taskSource 'level.dat'))){throw 'Run GameTest first to generate the dedicated test world.'}
if(!(Test-Path -LiteralPath $taskTarget)){
    [IO.Directory]::CreateDirectory($taskTarget)|Out-Null
    foreach($entry in Get-ChildItem -LiteralPath $taskSource){if($entry.Name -ne 'session.lock'){Copy-Item -LiteralPath $entry.FullName -Destination $taskTarget -Recurse}}
}
[IO.File]::WriteAllText((Join-Path $taskRoot 'run-pump-check/options.txt'),"lang:zh_cn`nguiScale:3`nrenderDistance:4`nsimulationDistance:5`nsoundCategory_master:0.0`npauseOnLostFocus:false`nenableVsync:false`nmaxFps:30`n",[Text.UTF8Encoding]::new($false))
Write-Output "Pump preview world ready: $taskTarget"
