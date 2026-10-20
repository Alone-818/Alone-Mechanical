$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
$taskRoot=Split-Path $PSScriptRoot -Parent
$taskAssets=Join-Path $taskRoot 'src/main/resources/assets/alone_mechanical'
$taskDirections=@('down','up','north','south','west','east')
function Matches($condition,$state){
    if(!$condition){return $true}
    foreach($key in $condition.Keys){
        if($key -eq 'OR'){
            $matched=$false
            foreach($child in $condition.OR){if(Matches $child $state){$matched=$true;break}}
            if(!$matched){return $false}
        }elseif($key -eq 'AND'){
            foreach($child in $condition.AND){if(!(Matches $child $state)){return $false}}
        }elseif($state[$key] -ne $condition[$key]){return $false}
    }
    return $true
}
function Active-Parts($parts,$state){foreach($part in $parts){if(Matches $part.when $state){$part.apply}}}
# Exercise the actual generated conditions before using them in the preview.
$checked=0
foreach($kind in @('item','fluid','gas')){for($tier=1;$tier -le 5;$tier++){
    $id="${kind}_pipe_$tier"
    $parts=(Get-Content -Raw (Join-Path $taskAssets "blockstates/$id.json") | ConvertFrom-Json -AsHashtable).multipart
    for($mask=0;$mask -lt 64;$mask++){
        $state=@{};$count=0
        for($bit=0;$bit -lt 6;$bit++){$connected=($mask -band (1 -shl $bit)) -ne 0;$state[$taskDirections[$bit]]=if($connected){'true'}else{'false'};if($connected){$count++}}
        $straight=$mask -in @(3,12,48)
        $active=@(Active-Parts $parts $state)
        if($active.Count -ne $(if($straight){1}else{1+$count})){throw "Wrong topology selection: $id mask=$mask"}
        if($straight -and !$active[0].model.EndsWith('_straight')){throw "Straight run still uses a fixed hub: $id mask=$mask"}
        if(!$straight -and @($active | Where-Object {$_.model -eq "alone_mechanical:block/$id"}).Count -ne 1){throw "Missing junction: $id mask=$mask"}
        foreach($part in $active){if($part.model -match '_(straight|arm)$' -and $part.uvlock){throw "Directional rail texture must rotate with geometry: $id mask=$mask"}}
        $checked++
    }
}}
Write-Output "PIPE TOPOLOGY PASSED: $checked connection states, all three straight axes, ends, bends and branches."
$taskImage=[Drawing.Bitmap]::new(1100,350)
$taskGraphics=[Drawing.Graphics]::FromImage($taskImage)
$taskGraphics.Clear([Drawing.Color]::FromArgb(22,30,40))
$taskGraphics.InterpolationMode=[Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$taskGraphics.PixelOffsetMode=[Drawing.Drawing2D.PixelOffsetMode]::Half
$taskTitle=[Drawing.Font]::new('Microsoft YaHei',18)
$taskFont=[Drawing.Font]::new('Microsoft YaHei',11)
$taskBrush=[Drawing.SolidBrush]::new([Drawing.Color]::FromArgb(230,240,250))
$taskGraphics.DrawString('管道材质随连接走向变化',$taskTitle,$taskBrush,18,12)
$taskPositions=@{}
for($x=0;$x -le 8;$x++){$taskPositions["$x,2"]=@($x,2)}
for($z=0;$z -le 4;$z++){$taskPositions["4,$z"]=@(4,$z)}
foreach($z in @(3,4)){$taskPositions["8,$z"]=@(8,$z)}
function Draw-Top($part,$offsetX,$offsetZ,$origin){
    $modelPath=Join-Path $taskAssets "models/$($part.model.Split(':')[1]).json"
    $model=Get-Content -Raw $modelPath | ConvertFrom-Json -AsHashtable
    $radians=[double]$part.y*[Math]::PI/180
    foreach($element in $model.elements){
        $face=$element.faces.up;if(!$face){continue}
        $uv=$face.uv;if(!$uv){$uv=@($element.from[0],$element.from[2],$element.to[0],$element.to[2])}
        $vertices=@(@($element.from[0],$element.from[2]),@($element.to[0],$element.from[2]),@($element.from[0],$element.to[2]))
        $points=[Collections.Generic.List[Drawing.PointF]]::new()
        foreach($vertex in $vertices){
            $vx=$vertex[0]-8;$vz=$vertex[1]-8
            $rotatedX=8+$vx*[Math]::Cos($radians)-$vz*[Math]::Sin($radians)
            $rotatedZ=8+$vx*[Math]::Sin($radians)+$vz*[Math]::Cos($radians)
            $points.Add([Drawing.PointF]::new(($origin+$offsetX*38+$rotatedX*38/16),(86+$offsetZ*38+$rotatedZ*38/16)))
        }
        $texture=$model.textures[$face.texture.TrimStart('#')]
        $sprite=[Drawing.Bitmap]::FromFile((Join-Path $taskAssets "textures/$($texture.Split(':')[1]).png"))
        try{$taskGraphics.DrawImage($sprite,$points.ToArray(),[Drawing.RectangleF]::new(($uv[0]*$sprite.Width/16),($uv[1]*$sprite.Height/16),(($uv[2]-$uv[0])*$sprite.Width/16),(($uv[3]-$uv[1])*$sprite.Height/16)),[Drawing.GraphicsUnit]::Pixel)}finally{$sprite.Dispose()}
    }
}
$index=0
foreach($kind in @('item','fluid','gas')){
    $origin=16+$index*365
    $taskGraphics.DrawString(@('物品管道','流体管道','气体管道')[$index],$taskTitle,$taskBrush,$origin,51)
    $parts=(Get-Content -Raw (Join-Path $taskAssets "blockstates/${kind}_pipe_5.json") | ConvertFrom-Json -AsHashtable).multipart
    foreach($position in $taskPositions.Values){
        $x=$position[0];$z=$position[1]
        $state=@{up='false';down='false'}
        foreach($step in @(@('north',0,-1),@('south',0,1),@('west',-1,0),@('east',1,0))){
            $key="$($x+$step[1]),$($z+$step[2])"
            $state[$step[0]]=if($taskPositions.ContainsKey($key)){'true'}else{'false'}
        }
        foreach($part in @(Active-Parts $parts $state)){Draw-Top $part $x $z $origin}
    }
    $index++
}
$taskGraphics.DrawString('实际 JSON 模型和贴图的俯视预览（非游戏截图）：连续直管、十字分支、转角和端点。',$taskFont,$taskBrush,18,306)
$taskOutput=Join-Path $taskRoot 'build/pipe-connections-preview.png'
$taskImage.Save($taskOutput,[Drawing.Imaging.ImageFormat]::Png)
$taskBrush.Dispose();$taskFont.Dispose();$taskTitle.Dispose();$taskGraphics.Dispose();$taskImage.Dispose()
Write-Output $taskOutput
