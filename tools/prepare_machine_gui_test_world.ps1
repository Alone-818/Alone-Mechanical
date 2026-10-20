$ErrorActionPreference='Stop'
$taskRoot=[IO.Path]::GetFullPath((Split-Path $PSScriptRoot -Parent))
$taskSource=Join-Path $taskRoot 'run-gametest/world'
$taskPreviewRoot=Join-Path $taskRoot 'run-gui-check'
$taskDestination=Join-Path $taskPreviewRoot 'saves/Machine GUI Preview'
if(!(Test-Path -LiteralPath (Join-Path $taskSource 'level.dat'))){throw 'Run runGameTestServer first to generate the dedicated test world.'}
if(!(Test-Path -LiteralPath $taskDestination)) {
    [IO.Directory]::CreateDirectory($taskDestination)|Out-Null
    # Exclude the runtime lock; the copied test world acquires its own lock when opened.
    foreach($entry in Get-ChildItem -LiteralPath $taskSource) {
        if($entry.Name -ne 'session.lock'){Copy-Item -LiteralPath $entry.FullName -Destination $taskDestination -Recurse}
    }
}
[IO.File]::WriteAllText((Join-Path $taskPreviewRoot 'options.txt'),"lang:zh_cn`nguiScale:3`nrenderDistance:4`nsimulationDistance:4`nsoundCategory_master:0.0`npauseOnLostFocus:false`nenableVsync:false`nmaxFps:30`n",[Text.UTF8Encoding]::new($false))
Write-Output "Machine GUI preview world ready: $taskDestination"
