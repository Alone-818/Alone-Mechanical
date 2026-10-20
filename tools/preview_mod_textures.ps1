$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
$taskRoot=Split-Path $PSScriptRoot -Parent
$assets=Join-Path $taskRoot 'src/main/resources/assets/alone_mechanical'
$labels=Get-Content -Raw (Join-Path $assets 'lang/zh_cn.json') | ConvertFrom-Json -AsHashtable
$entries=@($labels.Keys | Where-Object {$_ -match '^(block|item)\.alone_mechanical\.'} | Sort-Object)
$extra=@('pipe_connector_body','pipe_pull_mode','pipe_push_mode','item_port_0','item_port_1','item_port_2','item_port_3','fluid_port_0','fluid_port_1','fluid_port_2','fluid_port_3','gas_port_0','gas_port_1','gas_port_2','gas_port_3')
$tiles=@($entries | ForEach-Object {$pieces=$_.Split('.');@{kind=$pieces[0];name=$pieces[2];label=$labels[$_]}})
$tiles+=@($extra | ForEach-Object {@{kind='block';name=$_;label=$_}})
$font=[Drawing.Font]::new('Microsoft YaHei',9)
$heading=[Drawing.Font]::new('Microsoft YaHei',18)
$white=[Drawing.SolidBrush]::new([Drawing.Color]::FromArgb(220,233,241))
$muted=[Drawing.SolidBrush]::new([Drawing.Color]::FromArgb(140,163,179))
$checkA=[Drawing.SolidBrush]::new([Drawing.Color]::FromArgb(42,54,65))
$checkB=[Drawing.SolidBrush]::new([Drawing.Color]::FromArgb(54,68,80))
function Canvas($bitmap) {
    $g=[Drawing.Graphics]::FromImage($bitmap)
    $g.Clear([Drawing.Color]::FromArgb(20,29,38))
    $g.InterpolationMode=[Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
    $g.PixelOffsetMode=[Drawing.Drawing2D.PixelOffsetMode]::Half
    return $g
}
function Checkers($g,$x,$y,$w,$h) {
    for($cy=0;$cy -lt $h;$cy+=10){for($cx=0;$cx -lt $w;$cx+=10){
        $brush=if((([int]($cx/10)+[int]($cy/10))%2) -eq 0){$checkA}else{$checkB}
        $g.FillRectangle($brush,$x+$cx,$y+$cy,[Math]::Min(10,$w-$cx),[Math]::Min(10,$h-$cy))
    }}
}
function Sprite($g,$kind,$name,$x,$y,$size) {
    $sprite=[Drawing.Bitmap]::FromFile((Join-Path $assets "textures/$kind/$name.png"))
    $g.DrawImage($sprite,[Drawing.Rectangle]::new($x,$y,$size,$size));$sprite.Dispose()
}
$rows=[int][Math]::Ceiling($tiles.Count/8)
$atlas=[Drawing.Bitmap]::new(1200,(90+$rows*145))
$g=Canvas $atlas
$g.DrawString('孤独机械 · 全部材质',$heading,$white,24,15)
$g.DrawString('原生像素绘制：方块 / 物品 16×16，管道 / 接头 8×8；棋盘格表示透明区域。',$font,$muted,24,53)
for($i=0;$i -lt $tiles.Count;$i++) {
    $tile=$tiles[$i];$x=24+($i%8)*148;$y=86+[int][Math]::Floor($i/8)*145
    Checkers $g $x $y 80 80;Sprite $g $tile.kind $tile.name $x $y 80
    $g.DrawString($tile.label,$font,$white,[Drawing.RectangleF]::new($x,$y+84,140,40))
    $size=if($tile.name -match '(^((item|fluid|gas)_pipe_)|^pipe_)'){'8×8'}else{'16×16'}
    $g.DrawString($size,$font,$muted,$x,$y+121)
}
$out=Join-Path $taskRoot 'build/mod-texture-preview.png'
$atlas.Save($out,[Drawing.Imaging.ImageFormat]::Png);$g.Dispose();$atlas.Dispose()
Write-Output $out

$items=@($tiles | Where-Object {$_.kind -eq 'item'})
$itemAtlas=[Drawing.Bitmap]::new(1000,420);$g=Canvas $itemAtlas
$g.DrawString('物品材质 · 重绘',$heading,$white,24,15)
$g.DrawString('原生 16×16；材料颗粒、轮廓和工具结构分别绘制，棋盘格表示透明背景。',$font,$muted,24,53)
for($i=0;$i -lt $items.Count;$i++) {
    $x=24+($i%6)*163;$y=86+[int][Math]::Floor($i/6)*160
    Checkers $g $x $y 96 96;Sprite $g 'item' $items[$i].name $x $y 96
    $g.DrawString($items[$i].label,$font,$white,[Drawing.RectangleF]::new($x,$y+101,155,45))
}
$out=Join-Path $taskRoot 'build/items-preview.png'
$itemAtlas.Save($out,[Drawing.Imaging.ImageFormat]::Png);$g.Dispose();$itemAtlas.Dispose()
Write-Output $out

function Glass-Mask($points,$x,$y) {
    $mask=0;$edges=@(@(-1,0),@(1,0),@(0,-1),@(0,1))
    for($i=0;$i -lt 4;$i++){if($points.Contains("$($x+$edges[$i][0]),$($y+$edges[$i][1])")){$mask=$mask -bor (1 -shl $i)}}
    $corners=@(@(0,2),@(1,2),@(0,3),@(1,3))
    for($i=0;$i -lt 4;$i++){
        $a=$corners[$i][0];$b=$corners[$i][1]
        if(($mask -band (1 -shl $a)) -and ($mask -band (1 -shl $b)) -and !$points.Contains("$($x+$edges[$a][0]+$edges[$b][0]),$($y+$edges[$a][1]+$edges[$b][1])")){$mask=$mask -bor (1 -shl (4+$i))}
    };return $mask
}
$glass=[Drawing.Bitmap]::new(1120,445);$g=Canvas $glass
$g.DrawString('结构玻璃 · 透明与连接材质',$heading,$white,24,16)
$g.DrawString('使用生成的实际 PNG 拼接；中央不覆盖颜色，外缘保留金属细框和角铆钉。',$font,$muted,24,53)
$shapes=@(@(@(0,0)),@(@(0,0),@(1,0),@(2,0),@(0,1),@(1,1),@(2,1),@(0,2),@(1,2),@(2,2)),@(@(0,0),@(1,0),@(2,0),@(0,1),@(0,2)))
$captions=@('单块 · 透明中心','3×3 · 自动消除内部边框','凹角 · 保留缺角轮廓')
for($s=0;$s -lt 3;$s++) {
    $x=24+$s*365;$y=93;Checkers $g $x $y 288 288
    $g.DrawString('背景可见',$heading,$muted,$x+60,$y+125)
    $points=[Collections.Generic.HashSet[string]]::new()
    foreach($p in $shapes[$s]){[void]$points.Add("$($p[0]),$($p[1])")}
    foreach($p in $shapes[$s]){$mask=Glass-Mask $points $p[0] $p[1];Sprite $g 'block' "structural_glass_ct_$mask" ($x+$p[0]*96) ($y+$p[1]*96) 96}
    $g.DrawString($captions[$s],$font,$white,$x,397)
}
$out=Join-Path $taskRoot 'build/structural-glass-preview.png'
$glass.Save($out,[Drawing.Imaging.ImageFormat]::Png);$g.Dispose();$glass.Dispose()
foreach($disposable in @($font,$heading,$white,$muted,$checkA,$checkB)){$disposable.Dispose()}
Write-Output $out
