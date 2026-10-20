# Original, deterministic pixel art; this file defines only pipe artwork.
# Inspired by industrial framed transmitters, without importing Mekanism assets.
function Pipe-Element($from, $to, $texture = '#all') {
    return (Element $from $to $texture)
}
function Pipe-Panel($from, $to, $texture = '#all') {
    $element = Element $from $to $texture
    # Mode collars are separate panels; only these deliberately use a full sprite per face.
    foreach ($face in $element.faces.Values) { $face.uv = @(0,0,16,16) }
    return $element
}
function Pipe-Run($start, $end) {
    # Canonical run follows Z. U always crosses the pipe; V follows its length.
    # Side faces rotate their UVs too, so the colored rails follow the actual arm.
    $faces=[ordered]@{}
    foreach($face in @('up','down')){$faces[$face]=@{texture='#side';uv=@(0,$start,16,$end)}}
    foreach($face in @('east','west')){$faces[$face]=@{texture='#side';uv=@(0,$start,16,$end);rotation=90}}
    # Both axial ends touch a hub, another pipe or a container. No internal caps.
    return @{from=@(5,5,$start);to=@(11,11,$end);faces=$faces}
}
function Pipe-Texture($name, $accent, $style = 'body') {
    $path = Join-Path $resourceRoot "assets/$mod/textures/block/$name.png"
    [IO.Directory]::CreateDirectory((Split-Path $path -Parent)) | Out-Null
    $bitmap = [Drawing.Bitmap]::new(8,8)
    $accentHex='#{0:x2}{1:x2}{2:x2}' -f $accent[0],$accent[1],$accent[2]
    # Authored directly on the 8x8 grid: steel rails, recessed resource channel and tier tabs.
    Art-Rect $bitmap 0 0 8 8 '#25303b'
    Art-Line $bitmap 0 0 7 0 '#a3b4c0';Art-Line $bitmap 0 1 0 6 '#778c9a'
    Art-Line $bitmap 7 1 7 7 '#151f29';Art-Line $bitmap 1 7 6 7 '#151f29'
    Art-Line $bitmap 1 2 6 2 '#718695';Art-Line $bitmap 1 5 6 5 '#425563'
    Art-Rect $bitmap 2 3 4 2 '#15232e'
    if($style -in @('body','side')) {
        $kind=$name.Split('_')[0]
        $core=@{item='#c79b58';fluid='#306fba';gas='#68a8b3'}[$kind]
        if($style -eq 'side'){
            # A longitudinal strip, not a square panel repeated on every block.
            # Every row is identical: straight runs remain continuous at any length.
            $columns=@('#141b24','#71818e',$accentHex,$core,$core,$accentHex,'#71818e','#141b24')
            for($x=0;$x -lt 8;$x++){Art-Line $bitmap $x 0 $x 7 $columns[$x]}
        }else{
            # Framed junction plate only appears at ends, bends and branches.
            Art-Rect $bitmap 0 0 8 8 '#141b24'
            Art-Rect $bitmap 1 1 6 6 '#71818e'
            Art-Rect $bitmap 2 2 4 4 $accentHex
            Art-Rect $bitmap 3 3 2 2 $core
        }
    } elseif($style -eq 'collar') {
        Art-Rect $bitmap 1 1 6 6 '#465d6d';Art-Line $bitmap 1 1 6 1 '#aebdc6';Art-Line $bitmap 1 6 6 6 '#223541'
        Art-Line $bitmap 2 2 2 5 '#7e95a3';Art-Line $bitmap 5 2 5 5 '#324654'
    } else {
        Art-Rect $bitmap 1 1 6 6 $accentHex
        $light='#{0:x2}{1:x2}{2:x2}' -f ([Math]::Min(255,$accent[0]+45)),([Math]::Min(255,$accent[1]+45)),([Math]::Min(255,$accent[2]+45))
        Art-Line $bitmap 1 1 6 1 $light;Art-Line $bitmap 1 2 1 6 $light
        if($style -eq 'mode'){foreach($x in @(2,5)){Art-Rect $bitmap $x 2 1 4 '#304556'}}
    }
    $bitmap.Save($path,[Drawing.Imaging.ImageFormat]::Png); $bitmap.Dispose()
}
function Pipe-Art($id) {
    $tier=[int]$id.Split('_')[-1]
    $tierColors=@(@(91,175,111),@(213,167,65),@(203,81,84),@(71,178,202),@(166,103,213))
    Pipe-Texture $id $tierColors[$tier-1]
    Pipe-Texture "${id}_side" $tierColors[$tier-1] 'side'
    $kind=$id.Split('_')[0]
    $resourceColor=@{item=@(224,171,64);fluid=@(67,149,220);gas=@(75,195,174)}[$kind]
    Pipe-Texture "${kind}_pipe_marker" $resourceColor 'marker'
}
function Pipe-Models($id) {
    Pipe-Art $id
    $texture="${mod}:block/$id"
    $textures=@{all=$texture;particle=$texture;side="${texture}_side"}
    Model $id @{ textures=$textures;elements=@((Pipe-Panel @(5,5,5) @(11,11,11))) }
    Model "${id}_arm" @{ textures=$textures;elements=@((Pipe-Run 0 5)) }
    Model "${id}_straight" @{ textures=$textures;elements=@((Pipe-Run 0 16)) }
    # Rotate geometry AND artwork; flow stripes must follow the selected axis.
    $rotations=@{north=@(0,0);south=@(0,180);east=@(0,90);west=@(0,270);up=@(270,0);down=@(90,0)}
    # Non-straight states: a mismatched opposite pair, no connections, or two axes.
    $hub=@{OR=@(
        @{down='false';up='false';north='false';south='false';west='false';east='false'},
        @{down='true';up='false'},@{down='false';up='true'},
        @{north='true';south='false'},@{north='false';south='true'},
        @{west='true';east='false'},@{west='false';east='true'},
        @{up='true';north='true'},@{up='true';east='true'},@{north='true';east='true'}
    )}
    $multipart=@(@{when=$hub;apply=@{model="${mod}:block/$id"}})
    foreach($pair in @(@('north','south'),@('east','west'),@('up','down'))){
        $when=@{};foreach($d in $rotations.Keys){$when[$d]=if($d -in $pair){'true'}else{'false'}}
        $rotation=$rotations[$pair[0]]
        $multipart+=@{when=$when;apply=@{model="${mod}:block/${id}_straight";x=$rotation[0];y=$rotation[1];uvlock=$false}}
    }
    foreach($d in $rotations.Keys){$when=@{};$when[$d]='true';$multipart+=@{when=@{AND=@($when,$hub)};apply=@{model="${mod}:block/${id}_arm";x=$rotations[$d][0];y=$rotations[$d][1];uvlock=$false}}}
    # The item preview also uses a rail run and two framed end plates.
    Model "${id}_inventory" @{ textures=$textures;elements=@((Pipe-Run 0 16),(Pipe-Panel @(4,4,0) @(12,12,2)),(Pipe-Panel @(4,4,14) @(12,12,16))) }
    return $multipart
}
