$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$taskRoot = Split-Path $PSScriptRoot -Parent
$taskModels = Join-Path $taskRoot 'src/main/resources/assets/alone_mechanical/models/block'
$taskPreview = [Drawing.Bitmap]::new(1000,575)
$taskCanvas = [Drawing.Graphics]::FromImage($taskPreview)
$taskCanvas.SmoothingMode = [Drawing.Drawing2D.SmoothingMode]::AntiAlias
$taskCanvas.InterpolationMode = [Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$taskCanvas.PixelOffsetMode = [Drawing.Drawing2D.PixelOffsetMode]::Half
$taskCanvas.Clear([Drawing.Color]::FromArgb(22,30,40))
$taskFont = [Drawing.Font]::new('Microsoft YaHei',18)
$taskSmallFont = [Drawing.Font]::new('Microsoft YaHei',11)
$taskWhiteBrush = [Drawing.SolidBrush]::new([Drawing.Color]::FromArgb(228,238,248))
function Model-Elements($name) {
    $model=Get-Content -Raw (Join-Path $taskModels "$name.json") | ConvertFrom-Json -AsHashtable
    foreach($element in $model.elements) { $element.textureMap=$model.textures; $element }
}
function Paint-Pipe($mode, $offset, $kind='item') {
    $elements = @(Model-Elements "${kind}_pipe_3") + @(Model-Elements "${kind}_pipe_3_arm")
    if ($mode -ne 'normal') { $elements += @(Model-Elements "pipe_${mode}_connector") }
    $polygons = [Collections.Generic.List[object]]::new()
    foreach ($element in $elements) {
        $x0=$element.from[0]; $y0=$element.from[1]; $z0=$element.from[2]
        $x1=$element.to[0]; $y1=$element.to[1]; $z1=$element.to[2]
        $faces = @(
            @{ points=@(@($x0,$y1,$z0),@($x1,$y1,$z0),@($x1,$y1,$z1),@($x0,$y1,$z1)); name='up'; shade=1.0 },
            @{ points=@(@($x0,$y0,$z0),@($x1,$y0,$z0),@($x1,$y1,$z0),@($x0,$y1,$z0)); name='north'; shade=.76 },
            @{ points=@(@($x1,$y0,$z0),@($x1,$y0,$z1),@($x1,$y1,$z1),@($x1,$y1,$z0)); name='east'; shade=.60 }
        )
        foreach ($face in $faces) {
            if(!$element.faces.Contains($face.name)){continue}
            $texture=$element.faces[$face.name].texture
            # Inset band's front face is hidden by the solid sleeve; its exposed side faces overlay steel.
            if($texture -eq '#mode' -and $face.name -eq 'north') {continue}
            $rgb = switch ($texture) {
                '#marker' { @(224,171,64) }
                '#mode' { if ($mode -eq 'pull') { @(66,146,228) } else { @(232,144,54) } }
                default { @(77,88,99) }
            }
            $points=[Collections.Generic.List[Drawing.PointF]]::new(); $depth=0
            foreach ($point in $face.points) {
                $points.Add([Drawing.PointF]::new(($offset + ($point[0]+$point[2]-16)*8.8),(242 + ($point[0]-$point[2])*4.5-$point[1]*8.8)))
                $depth += $point[0]+2*$point[1]-$point[2]
            }
            $priority = if($texture -eq '#mode') {1} else {0}
            $asset=$element.textureMap[$texture.TrimStart('#')].Split(':')[1]
            $texturePath=Join-Path $taskRoot "src/main/resources/assets/alone_mechanical/textures/$asset.png"
            $uv=$element.faces[$face.name].uv
            if(!$uv){$uv=switch($face.name){
                'up' { @($x0,$z0,$x1,$z1) }
                'north' { @((16-$x1),(16-$y1),(16-$x0),(16-$y0)) }
                'east' { @((16-$z1),(16-$y1),(16-$z0),(16-$y0)) }
            }}
            $polygons.Add(@{points=$points.ToArray();name=$face.name;uv=$uv;rotation=$element.faces[$face.name].rotation;texture=$texturePath;shade=$face.shade;priority=$priority;depth=$depth/4;color=[Drawing.Color]::FromArgb([int]($rgb[0]*$face.shade),[int]($rgb[1]*$face.shade),[int]($rgb[2]*$face.shade))})
        }
    }
    foreach ($polygon in ($polygons | Sort-Object priority,depth)) {
        $brush=[Drawing.SolidBrush]::new($polygon.color)
        $taskCanvas.FillPolygon($brush,$polygon.points)
        $panel=[Drawing.Image]::FromFile($polygon.texture)
        $indices=if($polygon.name -eq 'up') {@(0,1,3)} else {@(2,3,1)}
        $destinations=[Drawing.PointF[]]@($polygon.points[$indices[0]],$polygon.points[$indices[1]],$polygon.points[$indices[2]])
        $uv=$polygon.uv
        if($polygon.rotation -eq 90){$panel.RotateFlip([Drawing.RotateFlipType]::Rotate90FlipNone);$uv=@((16-$uv[3]),$uv[0],(16-$uv[1]),$uv[2])}
        $taskCanvas.DrawImage($panel,$destinations,[Drawing.RectangleF]::new(($uv[0]*$panel.Width/16),($uv[1]*$panel.Height/16),(($uv[2]-$uv[0])*$panel.Width/16),(($uv[3]-$uv[1])*$panel.Height/16)),[Drawing.GraphicsUnit]::Pixel);$panel.Dispose()
        $shadeBrush=[Drawing.SolidBrush]::new([Drawing.Color]::FromArgb([int](255*(1-$polygon.shade)),0,0,0))
        $taskCanvas.FillPolygon($shadeBrush,$polygon.points);$shadeBrush.Dispose()
        $brush.Dispose()
    }
}
$taskCanvas.DrawString('管道端面 · 模型结构预览',$taskFont,$taskWhiteBrush,26,18)
Paint-Pipe 'normal' 170
Paint-Pipe 'pull' 500
Paint-Pipe 'push' 830
foreach ($entry in @(@(75,'普通连接','6 像素管身 · 无额外端环'),@(395,'拉取 / 抽入','8 像素金属套环 · 蓝色内环'),@(725,'推送 / 抽出','8 像素金属套环 · 橙色外环'))) {
    $taskCanvas.DrawString($entry[1],$taskFont,$taskWhiteBrush,$entry[0],310)
    $taskCanvas.DrawString($entry[2],$taskSmallFont,$taskWhiteBrush,$entry[0],349)
}
$taskCanvas.DrawString('实际 JSON 几何与贴图预览（非游戏截图）；下方展示原始 8×8 材质。',$taskSmallFont,$taskWhiteBrush,26,383)
$taskCanvas.InterpolationMode = [Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$textureRoot = Join-Path $taskRoot 'src/main/resources/assets/alone_mechanical/textures/block'
$samples=@('item_pipe_1','item_pipe_2','item_pipe_3','item_pipe_4','item_pipe_5','pipe_connector_body','pipe_pull_mode','pipe_push_mode')
for($i=0;$i -lt $samples.Count;$i++) {
    $sample=[Drawing.Image]::FromFile((Join-Path $textureRoot "$($samples[$i]).png"))
    $taskCanvas.DrawImage($sample,[Drawing.Rectangle]::new((26+$i*121),423,96,96));$sample.Dispose()
    $label = if($i -lt 5) { "等级 $($i+1)" } else { @('套环','抽入','抽出')[$i-5] }
    $taskCanvas.DrawString($label,$taskSmallFont,$taskWhiteBrush,(26+$i*121),525)
}
$taskOutput = Join-Path $taskRoot 'build/pipe-model-preview.png'
$taskPreview.Save($taskOutput,[Drawing.Imaging.ImageFormat]::Png)
$taskCanvas.Dispose(); $taskPreview.Dispose()
Write-Output $taskOutput

# Compare all three resource types using the actual block-model geometry and sprites.
$taskPreview=[Drawing.Bitmap]::new(1000,850)
$taskCanvas=[Drawing.Graphics]::FromImage($taskPreview)
$taskCanvas.InterpolationMode=[Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$taskCanvas.PixelOffsetMode=[Drawing.Drawing2D.PixelOffsetMode]::Half
$taskCanvas.Clear([Drawing.Color]::FromArgb(22,30,40))
$taskCanvas.DrawString('三类管道 · 外观区分',$taskFont,$taskWhiteBrush,26,18)
$types=@('item','fluid','gas')
$typeNames=@('物品管道 · 双轨货包','流体管道 · 蓝色液流','气体管道 · 浅色压力壳')
for($i=0;$i -lt 3;$i++){
    Paint-Pipe 'normal' (170+$i*330) $types[$i]
    $taskCanvas.DrawString($typeNames[$i],$taskFont,$taskWhiteBrush,(45+$i*330),300)
}
$taskCanvas.DrawString('实际 JSON 模型预览（非游戏截图）；下方每行展示同一类型的五个等级，均为原生 8×8。',$taskSmallFont,$taskWhiteBrush,26,355)
for($type=0;$type -lt 3;$type++) {
    $y=404+$type*143
    $taskCanvas.DrawString(@('物品','流体','气体')[$type],$taskFont,$taskWhiteBrush,26,($y+27))
    for($tier=1;$tier -le 5;$tier++) {
        $x=175+($tier-1)*160
        $sample=[Drawing.Image]::FromFile((Join-Path $textureRoot "$($types[$type])_pipe_$tier.png"))
        $taskCanvas.DrawImage($sample,[Drawing.Rectangle]::new($x,$y,96,96));$sample.Dispose()
        $taskCanvas.DrawString(@('基础','强化','高级','精英','终极')[$tier-1],$taskSmallFont,$taskWhiteBrush,$x,($y+100))
    }
}
$taskOutput=Join-Path $taskRoot 'build/pipe-types-preview.png'
$taskPreview.Save($taskOutput,[Drawing.Imaging.ImageFormat]::Png)
$taskWhiteBrush.Dispose();$taskFont.Dispose();$taskSmallFont.Dispose();$taskCanvas.Dispose();$taskPreview.Dispose()
Write-Output $taskOutput
