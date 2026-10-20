$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
$taskRoot=Split-Path $PSScriptRoot -Parent
$taskAssets=Join-Path $taskRoot 'src/main/resources/assets/alone_mechanical'
$taskFiles=@(Get-ChildItem -LiteralPath (Join-Path $taskAssets 'textures') -Filter '*.png' -Recurse)
$pipeCount=0;$glassCount=0
foreach($file in $taskFiles) {
    $pipe=$file.BaseName -match '^((item|fluid|gas)_pipe_|pipe_)'
    $armor=$file.FullName -like '*textures\models\armor\*'
    $expected=if($pipe){8}elseif($armor){64}else{16}
    $expectedHeight=if($armor){32}else{$expected}
    $image=[Drawing.Bitmap]::FromFile($file.FullName)
    try {
        if($image.Width -ne $expected -or $image.Height -ne $expectedHeight){throw "Wrong dimensions: $($file.Name) $($image.Width)x$($image.Height), expected ${expected}x$expectedHeight"}
        $colors=[Collections.Generic.HashSet[int]]::new();$visible=0
        for($y=0;$y -lt $expectedHeight;$y++){for($x=0;$x -lt $expected;$x++){
            $pixel=$image.GetPixel($x,$y);[void]$colors.Add($pixel.ToArgb());if($pixel.A -gt 0){$visible++}
        }}
        if($file.BaseName -eq 'structural_glass_ct_15') {
            if($visible -ne 0){throw 'Connected interior glass must be entirely transparent'}
        } elseif($visible -eq 0 -or $colors.Count -lt 2){throw "Blank placeholder: $($file.Name)"}
        if($file.BaseName -match '^structural_glass') {
            if($image.GetPixel(8,8).A -ne 0){throw "Glass center must remain transparent: $($file.Name)"}
            if($file.BaseName -match '_ct_'){$glassCount++}
        }
        if($pipe){$pipeCount++}
        if($file.BaseName -match '^(item|fluid|gas)_pipe_[1-5]_side$') {
            for($x=0;$x -lt 8;$x++){for($y=1;$y -lt 8;$y++){
                if($image.GetPixel($x,0).ToArgb() -ne $image.GetPixel($x,$y).ToArgb()){
                    throw "Longitudinal pipe rail is discontinuous: $($file.Name)"
                }
            }}
        }
    } finally {$image.Dispose()}
}
if($pipeCount -ne 36 -or $glassCount -ne 47){throw "Missing assets: pipes=$pipeCount, glass variants=$glassCount"}
foreach($file in (Get-ChildItem -LiteralPath (Join-Path $taskAssets 'models') -Filter '*.json' -Recurse)) {
    $model=Get-Content -LiteralPath $file.FullName -Raw | ConvertFrom-Json -AsHashtable
    foreach($reference in $model.textures.Values) {
        if($reference.StartsWith('alone_mechanical:')) {
            $path=Join-Path $taskAssets "textures/$($reference.Split(':')[1]).png"
            if(!(Test-Path -LiteralPath $path)){throw "Missing texture reference in $($file.Name): $reference"}
        }
    }
}
Write-Output "TEXTURE ASSETS PASSED: $($taskFiles.Count) PNGs, $pipeCount native 8x8 pipe sprites, native 16x16 item/block sprites and vanilla 64x32 armor UV sheets, $glassCount clear connected-glass variants; no blank placeholders or missing model textures."
