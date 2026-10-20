# Native 16x16 pixel artwork. Nuclear ore bases use the user's supplied reference textures.
function Art-Color($hex, $alpha=255) {
    $number=[Convert]::ToInt32($hex.TrimStart('#'),16)
    [Drawing.Color]::FromArgb($alpha,($number -shr 16 -band 255),($number -shr 8 -band 255),($number -band 255))
}
function Art-Rect($image,$x,$y,$width,$height,$hex) {
    $color=Art-Color $hex
    for($py=$y;$py -lt $y+$height;$py++){for($px=$x;$px -lt $x+$width;$px++){
        if($px -ge 0 -and $py -ge 0 -and $px -lt $image.Width -and $py -lt $image.Height){$image.SetPixel($px,$py,$color)}
    }}
}
function Art-Line($image,$x0,$y0,$x1,$y1,$hex) {
    $dx=[Math]::Abs($x1-$x0);$dy=-[Math]::Abs($y1-$y0)
    $sx=if($x0 -lt $x1){1}else{-1};$sy=if($y0 -lt $y1){1}else{-1};$lineError=$dx+$dy
    while($true){Art-Rect $image $x0 $y0 1 1 $hex;if($x0 -eq $x1 -and $y0 -eq $y1){break}
        $twice=2*$lineError;if($twice -ge $dy){$lineError+=$dy;$x0+=$sx};if($twice -le $dx){$lineError+=$dx;$y0+=$sy}}
}
function Art-Grid($image,$rows,$palette) {
    for($y=0;$y -lt $rows.Count;$y++){for($x=0;$x -lt $rows[$y].Length;$x++){
        $key=[string]$rows[$y][$x];if($palette.ContainsKey($key)){Art-Rect $image $x $y 1 1 $palette[$key]}
    }}
}
function Art-Metal($image,$dark='#424d59') {
    Art-Rect $image 0 0 16 16 '#242c35';Art-Rect $image 1 1 14 14 $dark
    Art-Line $image 1 0 14 0 '#a5b3be';Art-Line $image 0 1 0 14 '#7c8e9e'
    Art-Line $image 1 1 14 1 '#647787';Art-Line $image 1 14 14 14 '#303b47'
    Art-Line $image 1 15 14 15 '#171e27';Art-Line $image 15 1 15 14 '#171e27'
    foreach($point in @(@(2,2),@(13,2),@(2,13),@(13,13))){Art-Rect $image $point[0] $point[1] 1 1 '#c1cbd2';Art-Rect $image ($point[0]+1) $point[1] 1 1 '#28313b'}
}
function Art-Glass($image,$mask) {
    $edges=@(($mask -band 1),($mask -band 2),($mask -band 4),($mask -band 8))
    if(!$edges[0]){Art-Line $image 0 0 0 15 '#8da0a7'}
    if(!$edges[1]){Art-Line $image 15 0 15 15 '#5d727d'}
    if(!$edges[2]){Art-Line $image 0 0 15 0 '#bacad0'}
    if(!$edges[3]){Art-Line $image 0 15 15 15 '#5d727d'}
    $corners=@(@(0,0,0,2),@(15,0,1,2),@(0,15,0,3),@(15,15,1,3))
    for($corner=0;$corner -lt 4;$corner++){
        $c=$corners[$corner];if((!$edges[$c[2]] -and !$edges[$c[3]]) -or ($mask -band (1 -shl (4+$corner)))){
            Art-Rect $image $c[0] $c[1] 1 1 '#c8a16c'
        }
    }
    # Sparse reflections at exterior corners; a fully connected center has no opaque pixels.
    if(!$edges[0] -and !$edges[2]){Art-Rect $image 3 3 1 1 '#dfedf0';Art-Rect $image 4 4 1 1 '#b7d4dc'}
    if(!$edges[1] -and !$edges[3]){Art-Rect $image 11 11 1 1 '#b7d4dc';Art-Rect $image 12 12 1 1 '#dfedf0'}
}
function Art-Storage($image,$vault,$top,$mask) {
    Art-Metal $image '#465562'
    if($top){
        Art-Rect $image 0 0 16 16 '#465562'
        if(!($mask -band 1)){Art-Rect $image 0 0 2 16 '#8597a2';Art-Rect $image 2 0 1 16 '#283943'}
        if(!($mask -band 2)){Art-Rect $image 14 0 2 16 '#283943';Art-Rect $image 13 0 1 16 '#8597a2'}
        if(!($mask -band 4)){Art-Rect $image 0 0 16 2 '#aab9bf';Art-Rect $image 0 2 16 1 '#283943'}
        if(!($mask -band 8)){Art-Rect $image 0 14 16 2 '#283943';Art-Rect $image 0 13 16 1 '#8597a2'}
        Art-Rect $image 6 6 4 4 '#3c4b57';Art-Line $image 6 6 9 6 '#718590';return
    }
    $above=($mask -band 1) -ne 0;$below=($mask -band 2) -ne 0
    Art-Rect $image 0 0 16 16 '#465562'
    Art-Rect $image 0 0 1 16 '#7c8e9e';Art-Rect $image 1 0 1 16 '#647787'
    Art-Rect $image 14 0 1 16 '#303b47';Art-Rect $image 15 0 1 16 '#171e27'
    $panelTop=if($above){0}else{3};$panelBottom=if($below){16}else{13}
    $windowTop=if($above){0}else{4};$windowBottom=if($below){16}else{12}
    Art-Rect $image 4 $panelTop 8 ($panelBottom-$panelTop) '#202c35'
    Art-Rect $image 5 $windowTop 6 ($windowBottom-$windowTop) '#1c2932'
    if($vault){Art-Rect $image 6 7 4 3 '#99815f';Art-Line $image 6 8 9 8 '#d0b68b';Art-Rect $image 8 7 1 3 '#675a48'}
    else{for($y=$windowTop;$y -lt $windowBottom;$y++){for($x=5;$x -le 10;$x++){$image.SetPixel($x,$y,[Drawing.Color]::Transparent)}};if(!$above){Art-Rect $image 5 5 1 2 '#b9d0d5'}}
    if(!$above){Art-Rect $image 0 0 16 1 '#a6b6bf';Art-Rect $image 1 1 14 1 '#647787';foreach($x in @(2,13)){Art-Rect $image $x 2 1 1 '#c1cbd2'}}
    if(!$below){Art-Rect $image 0 15 16 1 '#25313a';Art-Rect $image 1 14 14 1 '#303b47';foreach($x in @(2,13)){Art-Rect $image $x 13 1 1 '#c1cbd2'}}
}
function Art-UraniumOre($image,$deep) {
    $baseName=if($deep){'deepslate'}else{'stone'}
    $basePath=Join-Path $PSScriptRoot "art_sources/nuclear/$baseName.png"
    $base=[Drawing.Bitmap]::FromFile($basePath)
    try{
        if($base.Width -ne 16 -or $base.Height -ne 16){throw "Expected a native 16x16 reference: $basePath"}
        # Keep every host-rock pixel unchanged outside the newly drawn mineral veins.
        for($y=0;$y -lt 16;$y++){for($x=0;$x -lt 16;$x++){$image.SetPixel($x,$y,$base.GetPixel($x,$y))}}
    }finally{$base.Dispose()}
    $rows=@(
        '................',
        '......abc.......',
        '..ab..cde.......',
        '..cd..bdc.......',
        '..be...a........',
        '...a.......ab...',
        '..........bcda..',
        '.....ab...cdeb..',
        '....bcda...bca..',
        '....cdeb........',
        '....aca.....ab..',
        '...........bcda.',
        '.abca......cdeb.',
        '.bcdeb......ab..',
        '..bca...........',
        '................'
    )
    if(@($rows|Where-Object {$_.Length -ne 16}).Count){throw 'Uranium vein grid must be 16x16'}
    $edge=if($deep){'#303b2f'}else{'#52604b'}
    Art-Grid $image $rows @{a=$edge;b='#506d39';c='#7e9e4f';d='#b0ca75';e='#d7e4a0'}
}
function Art-RawUranium($image) {
    $basePath=Join-Path $PSScriptRoot 'art_sources/nuclear/raw_iron.png'
    $base=[Drawing.Bitmap]::FromFile($basePath)
    $palette=@{
        '#3b3429'='#2e362d';'#574d39'='#49523c';'#887455'='#68764b';'#af8e77'='#7e9b50'
        '#d8af93'='#9bbc66';'#e9c8b1'='#b3d67d';'#fedec8'='#cce699';'#fef4ed'='#e6efbd'
    }
    try{
        if($base.Width -ne 16 -or $base.Height -ne 16){throw 'Raw mineral reference must be 16x16'}
        # Preserve the reference's angular silhouette, clustered mineral faces and transparent pixels.
        for($y=0;$y -lt 16;$y++){for($x=0;$x -lt 16;$x++){
            $pixel=$base.GetPixel($x,$y)
            if($pixel.A -eq 0){$image.SetPixel($x,$y,$pixel);continue}
            $hex='#{0:x2}{1:x2}{2:x2}' -f $pixel.R,$pixel.G,$pixel.B
            if(!$palette.ContainsKey($hex)){throw "Unknown raw mineral reference color: $hex"}
            $image.SetPixel($x,$y,(Art-Color $palette[$hex] $pixel.A))
        }}
    }finally{$base.Dispose()}
}
function Art-Block($image,$name,$mode=-1) {
    if($name -eq 'reactor_fuel_rod'){
        Art-Metal $image '#42545e'
        Art-Rect $image 2 2 12 12 '#15252c'
        foreach($x in @(3,7,11)){Art-Rect $image $x 3 2 10 '#98adb0';Art-Line $image $x 4 $x 11 '#d0d9d1'}
        Art-Rect $image 2 2 12 2 '#6b8188';Art-Rect $image 2 12 12 2 '#3c515c'
        Art-Line $image 3 3 12 3 '#b5c5b4';Art-Rect $image 1 6 1 4 '#b0a461';Art-Rect $image 14 6 1 4 '#b0a461'
        return
    }
    if($name -in @('uranium_ore','deepslate_uranium_ore')){Art-UraniumOre $image ($name -eq 'deepslate_uranium_ore');return}
    if($name -in @('solar_panel','lunar_panel')) {
        Art-Metal $image '#a2b6c3'
        $dark=if($name -eq 'lunar_panel'){'#332652'}else{'#173b65'}
        $light=if($name -eq 'lunar_panel'){'#ad9bdb'}else{'#62a6cf'}
        Art-Rect $image 1 1 14 14 $dark
        foreach($x in @(1,5,10,14)){Art-Line $image $x 1 $x 14 $light}
        foreach($y in @(1,5,10,14)){Art-Line $image 1 $y 14 $y $light}
        foreach($x in @(2,6,11)){Art-Line $image $x 2 $x 4 '#a7c5df';Art-Line $image $x 6 $x 9 '#739bca';Art-Line $image $x 11 $x 13 '#739bca'}
        return
    }
    if($name -in @('solar_generator','lunar_generator','thermal_generator')) {
        Art-Metal $image '#3d505d'
        Art-Rect $image 3 4 10 8 '#1e2d38'
        if($name -eq 'thermal_generator') {
            Art-Rect $image 4 5 8 6 '#3e302b';Art-Rect $image 5 7 6 3 '#a85532';Art-Rect $image 7 6 2 4 '#efb14f'
            foreach($x in @(4,7,10)){Art-Line $image $x 5 $x 10 '#a8b1ae'}
        } else {
            $accent=if($name -eq 'lunar_generator'){'#b6a0df'}else{'#77c7e0'}
            Art-Rect $image 5 6 6 4 $accent;Art-Line $image 6 5 9 5 '#d0e6ee';Art-Line $image 6 10 9 10 '#536d82'
        }
        Art-Line $image 4 13 11 13 '#c2ac7c';return
    }
    if($name -match '^(storage_tank|item_vault)_ct_(side|top)_(\d+)$'){Art-Storage $image ($Matches[1] -eq 'item_vault') ($Matches[2] -eq 'top') ([int]$Matches[3]);return}
    if($name -eq 'storage_tank'){Art-Storage $image $false $false 0;return}
    if($name -eq 'tank_gas'){for($y=0;$y -lt 16;$y++){for($x=0;$x -lt 16;$x++){Art-Rect $image $x $y 1 1 $(if(($x*13+$y*7)%5 -eq 0){'#b0b9bf'}else{'#e0e6e8'})}};return}
    if($name -match '^structural_glass(?:_ct_(\d+))?$'){
        $mask=if($Matches[1]){[int]$Matches[1]}else{0};Art-Glass $image $mask;return
    }
    $face='front';if($name -match '_(side|top|bottom)$'){$face=$Matches[1];$name=$name -replace '_(side|top|bottom)$',''}
    $name=$name -replace '_[0-3]$',''
    Art-Metal $image
    if($name -in @('turbine_core','turbine_rotor')){
        $accent=if($name -eq 'turbine_core'){'#d9e6ed'}else{'#d2a85f'}
        Art-Rect $image 3 3 10 10 '#202b35';Art-Line $image 3 3 12 3 $accent
        if($name -eq 'turbine_core'){Art-Rect $image 5 5 6 6 '#86b8cb';Art-Rect $image 7 7 2 2 '#eaf7fa';Art-Line $image 4 12 11 12 '#8da7b2'}
        else {Art-Rect $image 7 3 2 10 $accent;Art-Line $image 4 5 11 5 '#f2d79c';Art-Line $image 4 10 11 10 '#8f6d38';Art-Rect $image 5 7 6 2 '#a57b3b'}
        return
    }
    if($name -in @('slaughter_component','breeding_component','harvest_component','repeller_component')){
        $accent=@{slaughter_component='#d77672';breeding_component='#dfa48b';harvest_component='#92c471';repeller_component='#c3a4e5'}[$name]
        Art-Rect $image 3 3 10 10 '#202d36';Art-Line $image 3 3 12 3 $accent
        switch($name){
            'slaughter_component'{Art-Line $image 5 4 10 11 $accent;Art-Line $image 10 4 5 11 '#d7dfe0';Art-Rect $image 4 10 3 2 $accent}
            'breeding_component'{Art-Rect $image 4 5 3 3 $accent;Art-Rect $image 9 5 3 3 $accent;Art-Rect $image 5 7 6 3 $accent;Art-Rect $image 7 10 2 2 $accent}
            'harvest_component'{Art-Line $image 8 4 8 12 '#d5ba76';foreach($y in @(5,8)){Art-Line $image 5 ($y-1) 8 ($y+1) $accent;Art-Line $image 11 ($y-1) 8 ($y+1) $accent}}
            'repeller_component'{Art-Line $image 4 5 11 5 $accent;Art-Line $image 4 5 5 10 $accent;Art-Line $image 11 5 10 10 $accent;Art-Line $image 5 10 8 12 $accent;Art-Line $image 10 10 8 12 $accent;Art-Rect $image 7 6 2 4 '#e8d8f0'}
        };return
    }
    if($name -in @('electric_pump','electric_pump_active')){
        if($face -in @('top','bottom')){
            Art-Rect $image 3 3 10 10 '#293a43';Art-Rect $image 4 4 8 8 '#7f9eac';Art-Rect $image 5 5 6 6 '#182d39';Art-Rect $image 6 6 4 4 '#3c849c';Art-Line $image 6 6 9 6 '#92cbd4'
            $color=if($face -eq 'bottom'){'#73bac6'}else{'#d4ac69'};Art-Rect $image 7 2 2 1 $color;Art-Rect $image 7 13 2 1 $color
        }elseif($face -eq 'side'){
            Art-Rect $image 3 4 10 8 '#26343e';foreach($x in @(4,7,10)){Art-Rect $image $x 5 2 6 '#a16d47';Art-Rect $image $x 5 1 5 '#d0a16a'}
        }else{
            Art-Rect $image 3 3 7 10 '#273c4c';Art-Rect $image 4 8 5 4 '#377fa3';Art-Line $image 4 8 8 8 '#87c9de'
            Art-Rect $image 5 4 3 4 '#b9c9d2';Art-Rect $image 6 5 1 6 '#566e81';Art-Rect $image 4 11 5 1 '#7bb4c8'
            Art-Rect $image 11 4 2 5 '#26333a';$indicator=if($name -eq 'electric_pump_active'){'#83d9a2'}else{'#aa7755'};Art-Rect $image 11 4 2 2 $indicator;Art-Rect $image 11 10 2 2 '#ad8a58'
        };return
    }
    if($face -eq 'top'){
        Art-Rect $image 4 4 8 8 '#303b45';Art-Line $image 4 4 11 4 '#8494a0'
        Art-Line $image 4 11 11 11 '#171e27';Art-Rect $image 7 7 2 2 '#b79660';return
    }
    if($face -eq 'side'){
        for($x=4;$x -le 11;$x+=3){Art-Rect $image $x 3 1 10 '#27323c';Art-Rect $image ($x+1) 3 1 10 '#73828e'};return
    }
    if($name -in @('forming_component','infusion_component','industrial_core','forging_component','calcination_component','alloy_component','compound_component','decomposition_component')){
        $accent=@{forming_component='#d7b58a';infusion_component='#8fcfce';industrial_core='#c6a267';forging_component='#dcb474';calcination_component='#ed8b58';alloy_component='#91b5ce';compound_component='#77c7a3';decomposition_component='#bf97d2'}[$name]
        Art-Rect $image 3 3 10 10 '#202b35';Art-Line $image 3 3 12 3 $accent
        switch($name){
            'forming_component'{Art-Rect $image 4 4 8 2 $accent;Art-Rect $image 4 10 8 2 $accent;Art-Rect $image 6 6 4 4 '#cbd7df';Art-Rect $image 7 7 2 2 '#556673'}
            'infusion_component'{Art-Rect $image 4 4 4 3 $accent;Art-Rect $image 9 8 3 4 '#dbe6e9';Art-Line $image 6 7 10 10 $accent;Art-Rect $image 10 5 1 2 '#a5e7e1'}
            'industrial_core'{Art-Rect $image 5 5 6 6 $accent;Art-Rect $image 7 7 2 2 '#334653'}
            'forging_component'{Art-Rect $image 5 4 6 3 $accent;Art-Rect $image 7 7 2 3 '#a6b5bd';Art-Rect $image 4 10 8 2 $accent}
            'calcination_component'{Art-Line $image 5 10 8 5 $accent;Art-Line $image 8 5 11 10 $accent;Art-Line $image 5 10 11 10 '#ffd69c';Art-Rect $image 8 8 1 2 '#f5b86e'}
            'alloy_component'{Art-Rect $image 4 6 3 4 $accent;Art-Rect $image 9 6 3 4 '#d5ab72';Art-Rect $image 7 7 2 2 '#dce7df'}
            'compound_component'{Art-Line $image 4 5 8 8 $accent;Art-Line $image 11 5 8 8 $accent;Art-Rect $image 7 8 3 4 '#b6e8ce'}
            'decomposition_component'{Art-Rect $image 7 4 3 3 '#e1cced';Art-Line $image 8 7 4 11 $accent;Art-Line $image 8 7 12 11 $accent;Art-Rect $image 4 10 2 2 $accent;Art-Rect $image 11 10 2 2 $accent}
        }
        return
    }
    switch($name){
        'machine_casing' {
            Art-Rect $image 3 3 10 10 '#4c5966';Art-Line $image 3 3 12 3 '#8193a2'
            Art-Line $image 3 8 12 8 '#303d48';Art-Line $image 3 9 12 9 '#596b7b'
        }
        'reinforced_casing' {
            Art-Rect $image 3 3 10 10 '#303743';Art-Line $image 3 3 12 12 '#87929e';Art-Line $image 4 3 12 11 '#4d5a67'
            Art-Line $image 12 3 3 12 '#7a8793';Art-Line $image 11 3 3 11 '#4d5a67'
            foreach($p in @(@(3,3),@(12,3),@(3,12),@(12,12))){Art-Rect $image $p[0] $p[1] 1 1 '#cfaa6b'}
        }
        'energy_coil' {
            Art-Rect $image 3 3 10 10 '#2b3540';Art-Rect $image 7 3 2 10 '#407889'
            foreach($y in @(4,6,8,10)){Art-Rect $image 3 $y 10 1 '#dd9958';Art-Rect $image 3 ($y+1) 10 1 '#925533'}
            Art-Rect $image 4 3 1 10 '#c77945';Art-Rect $image 11 3 1 10 '#754b32'
            Art-Rect $image 7 3 2 1 '#84d1de';Art-Rect $image 7 12 2 1 '#84d1de'
        }
        'efficiency_coil' {
            Art-Rect $image 3 3 10 10 '#29353d'
            foreach($p in @(@(6,3,4,2),@(6,11,4,2),@(3,6,2,4),@(11,6,2,4),@(4,4,2,2),@(10,4,2,2),@(4,10,2,2),@(10,10,2,2))){Art-Rect $image $p[0] $p[1] $p[2] $p[3] '#d6ad55'}
            Art-Rect $image 5 5 6 6 '#ad7439';Art-Rect $image 6 6 4 4 '#3b7883';Art-Rect $image 7 7 2 2 '#9ae3df'
        }
        'energy_matrix' {
            Art-Rect $image 3 3 10 10 '#25333d'
            foreach($x in @(4,7,10)){Art-Rect $image $x 4 2 8 '#2a7487';Art-Rect $image $x 5 1 6 '#77d2e4';Art-Rect $image $x 3 2 1 '#c5ac78';Art-Rect $image $x 12 2 1 '#537483'}
        }
        'processing_matrix' {
            # Four parallel processor dies with shared copper buses, distinct from battery cells.
            Art-Rect $image 3 3 10 10 '#292840'
            Art-Line $image 3 7 12 7 '#baa06c';Art-Line $image 7 3 7 12 '#baa06c'
            foreach($point in @(@(4,4),@(9,4),@(4,9),@(9,9))){
                Art-Rect $image $point[0] $point[1] 3 3 '#685393'
                Art-Rect $image $point[0] $point[1] 2 1 '#c5a9e6'
                Art-Rect $image ($point[0]+1) ($point[1]+1) 1 1 '#e6def3'
            }
        }
        'boiler_core' {
            Art-Rect $image 3 3 10 10 '#20364b';Art-Rect $image 4 4 3 3 '#4c8eb5';Art-Rect $image 9 4 3 3 '#17304a'
            Art-Line $image 5 7 8 10 '#6aaed0';Art-Line $image 10 7 8 10 '#315d89';Art-Rect $image 6 10 5 2 '#2d6a9d';Art-Rect $image 7 10 3 1 '#a4d4e8'
        }
        'crusher_core' {
            Art-Rect $image 3 3 10 10 '#232a31'
            foreach($x in @(4,9)){Art-Rect $image $x 4 3 8 '#6d7b87';Art-Rect $image $x 4 1 8 '#b4c0c9';foreach($y in @(5,8,11)){Art-Rect $image ($x-1) $y 5 1 '#8998a4'}}
            Art-Rect $image 7 7 2 2 '#bb9661';Art-Rect $image 7 10 1 1 '#e4c689'
            foreach($x in @(3,7,11)){Art-Rect $image $x 13 2 1 '#d0a556'}
        }
        'smelting_core' {
            Art-Rect $image 3 3 10 10 '#4c3932';Art-Rect $image 4 4 8 7 '#251f25'
            Art-Rect $image 5 7 6 4 '#b55030';Art-Rect $image 6 6 4 4 '#e48b37';Art-Rect $image 7 7 2 2 '#ffdc83'
            foreach($x in @(4,7,10)){Art-Rect $image $x 12 2 1 '#b28462'}
            Art-Line $image 4 3 11 3 '#d3bd9b'
        }
        'separation_core' {
            Art-Rect $image 3 3 10 10 '#26353e'
            Art-Rect $image 7 3 2 3 '#98dbdc';Art-Rect $image 7 3 1 2 '#def3e7'
            Art-Line $image 7 6 4 9 '#75b8df';Art-Line $image 8 6 11 9 '#e3b575'
            Art-Rect $image 3 9 3 3 '#416d91';Art-Rect $image 3 9 2 1 '#add8ec'
            Art-Rect $image 10 9 3 3 '#9e733f';Art-Rect $image 10 9 2 1 '#f1d8a0'
            Art-Rect $image 7 7 2 2 '#d9e7db';Art-Rect $image 6 12 4 1 '#6b8491'
        }
        'alloy_core' {
            # Two feed channels merge into a hot crucible.
            Art-Rect $image 3 3 10 10 '#302e35'
            Art-Rect $image 4 3 3 3 '#a9bdcc';Art-Rect $image 9 3 3 3 '#646573'
            Art-Line $image 5 6 7 8 '#8197aa';Art-Line $image 10 6 8 8 '#c19765'
            Art-Rect $image 4 8 8 4 '#824c38';Art-Rect $image 5 8 6 2 '#f2b15b'
            Art-Rect $image 6 8 4 1 '#ffe2a0';Art-Rect $image 5 11 6 1 '#b1774a'
            Art-Rect $image 6 12 4 1 '#abbccb'
        }
        'reaction_core' {
            Art-Rect $image 3 3 10 10 '#263a42'
            Art-Rect $image 5 3 2 3 '#6dcabb';Art-Rect $image 9 3 2 3 '#a0c2e8'
            Art-Line $image 5 6 7 8 '#6dcabb';Art-Line $image 10 6 8 8 '#a0c2e8'
            Art-Rect $image 6 8 4 4 '#54969e';Art-Rect $image 7 9 2 2 '#c2e8dc'
            Art-Rect $image 4 13 8 1 '#c6ab72'
        }
        'conversion_core' {
            Art-Rect $image 3 3 10 10 '#263640'
            Art-Line $image 4 5 11 5 '#80ced9';Art-Line $image 9 3 11 5 '#80ced9';Art-Line $image 11 5 9 7 '#80ced9'
            Art-Line $image 11 10 4 10 '#c3a9e4';Art-Line $image 6 8 4 10 '#c3a9e4';Art-Line $image 4 10 6 12 '#c3a9e4'
            Art-Rect $image 3 7 2 2 '#6cb4cd';Art-Rect $image 11 7 2 2 '#b496d6'
            Art-Rect $image 7 7 2 2 '#dce8dc'
        }
        'machine_controller' {
            Art-Rect $image 3 3 10 8 '#172b31';Art-Line $image 3 3 12 3 '#7b959f'
            Art-Line $image 4 8 6 6 '#67ced2';Art-Line $image 6 6 8 8 '#67ced2';Art-Line $image 8 8 11 5 '#67ced2'
            Art-Rect $image 4 10 7 1 '#6daa80';Art-Rect $image 4 12 2 1 '#d5ae69';Art-Rect $image 8 12 2 1 '#70b6c2';Art-Rect $image 11 12 1 1 '#789b6b'
        }
        'item_vault' {
            Art-Rect $image 3 3 10 10 '#625b4e'
            foreach($y in @(4,7,10)){Art-Rect $image 3 $y 10 1 '#958674';Art-Rect $image 3 ($y+1) 10 1 '#393b3d'}
            Art-Rect $image 6 7 4 2 '#b7a480';Art-Rect $image 7 8 2 1 '#252c33'
        }
        'fluid_tank' {
            Art-Rect $image 3 3 9 10 '#243a47';Art-Rect $image 4 4 7 8 '#46758b';Art-Rect $image 4 9 7 3 '#428ebd'
            Art-Line $image 5 4 5 8 '#a8c5cf';Art-Line $image 4 3 10 3 '#9bb4bd'
            foreach($y in @(4,6,8,10,12)){Art-Rect $image 12 $y 1 1 '#c8d1ca'}
            # Transparent observation window exposes the actual animated fluid renderer.
            for($py=4;$py -le 11;$py++){for($px=4;$px -le 10;$px++){$image.SetPixel($px,$py,[Drawing.Color]::Transparent)}}
            Art-Rect $image 5 4 1 2 '#a8c5cf'
        }
        'gas_tank' {
            Art-Rect $image 3 3 9 10 '#244745';Art-Rect $image 4 4 7 8 '#36726e';Art-Line $image 5 4 5 11 '#89c6b8'
            foreach($p in @(@(7,5),@(9,7),@(7,10))){Art-Rect $image $p[0] $p[1] 1 1 '#b7dfcd'}
            foreach($y in @(4,6,8,10,12)){Art-Rect $image 12 $y 1 1 '#c8d1ca'}
            Art-Rect $image 7 2 2 1 '#c3a16d'
        }
        'power_core' {
            Art-Rect $image 3 3 10 10 '#243b36';Art-Line $image 4 4 6 4 '#5b927c';Art-Line $image 10 11 12 11 '#5b927c'
            Art-Line $image 9 3 6 8 '#98d4c5';Art-Line $image 6 8 10 8 '#98d4c5';Art-Line $image 10 8 7 12 '#98d4c5'
            Art-Rect $image 4 11 1 1 '#c7b37d';Art-Rect $image 11 4 1 1 '#c7b37d'
        }
        'signal_pole' {
            Art-Rect $image 0 0 16 16 '#3c4753';Art-Rect $image 5 0 2 16 '#9aa8b3';Art-Rect $image 7 0 2 16 '#637482';Art-Rect $image 9 0 2 16 '#313d48'
            foreach($y in @(3,12)){Art-Rect $image 3 $y 10 1 '#ad8f67';Art-Rect $image 3 ($y+1) 10 1 '#62503d'}
            Art-Rect $image 7 8 2 1 '#d1d9d9'
        }
        'power_pylon' {
            Art-Rect $image 0 0 16 16 '#3b4750';Art-Rect $image 4 0 2 16 '#889eaa';Art-Rect $image 10 0 2 16 '#455863'
            foreach($y in @(2,5,10,13)){Art-Rect $image 3 $y 10 1 '#c28651';Art-Rect $image 3 ($y+1) 10 1 '#755035'}
            Art-Rect $image 7 0 2 16 '#617884'
        }
        'creative_power_source' {
            Art-Rect $image 3 3 10 10 '#423457'
            Art-Line $image 5 5 10 10 '#b690da';Art-Line $image 10 5 5 10 '#b690da';Art-Line $image 8 3 8 12 '#8c70ac';Art-Line $image 3 8 12 8 '#8c70ac'
            Art-Rect $image 6 6 4 4 '#ccaddf';Art-Rect $image 7 7 2 2 '#eff0e5'
        }
    }
    if($name -match '^(item|fluid|gas|universal)_port$') {
        $accent=@{item='#d6aa64';fluid='#62a6ce';gas='#69bba8';universal='#b7b3d8'}[$Matches[1]]
        Art-Rect $image 3 3 10 10 '#29353f';Art-Rect $image 4 2 8 1 $accent
        if($mode -lt 0){
            switch($Matches[1]){
                'item'{Art-Rect $image 5 5 6 6 $accent;Art-Line $image 5 7 10 7 '#665334';Art-Line $image 7 5 7 10 '#665334'}
                'fluid'{Art-Rect $image 7 4 2 2 $accent;Art-Rect $image 6 6 4 4 $accent;Art-Rect $image 7 10 2 1 $accent;Art-Rect $image 7 6 1 2 '#c6e8eb'}
                'universal'{Art-Rect $image 5 5 6 6 '#76748c';Art-Line $image 5 8 10 8 '#d0cbe6';Art-Line $image 8 5 8 10 '#d0cbe6'}
                'gas'{foreach($p in @(@(6,5),@(9,7),@(6,10))){Art-Rect $image $p[0] $p[1] 2 2 $accent}}
            }
        }
    }
    if($mode -ge 0){
        $color=if($mode -lt 2){'#68b2e5'}else{'#eca35c'}
        Art-Rect $image 4 10 8 1 '#a7b5bd';Art-Rect $image 4 8 1 3 '#a7b5bd';Art-Rect $image 11 8 1 3 '#a7b5bd'
        Art-Rect $image 7 4 2 5 $color
        if($mode -lt 2){Art-Rect $image 5 6 6 1 $color;Art-Rect $image 6 7 4 1 $color}
        else{Art-Rect $image 6 3 4 1 $color;Art-Rect $image 5 4 6 1 $color}
        Art-Rect $image 6 12 2 1 '#d7d8cc';if(($mode%2)-eq 1){Art-Rect $image 9 12 2 1 '#d7d8cc'}
    }
}
function Art-Powder($image,$name) {
    # Larger cohesive piles, distinct contours, grouped highlights and a clean one-pixel outline.
    $profiles=@{
        iron_dust=@{y=3;colors=@('#34434f','#607b8c','#a7bfce','#d7e6ef','#f5fafc');rows=@(@(8,9),@(7,10),@(6,11),@(4,11),@(3,12),@(2,13),@(1,14),@(2,13),@(2,12),@(3,12),@(5,10))}
        copper_dust=@{y=3;colors=@('#542719','#9c4b29','#df833f','#ffc482','#ffe5b1');rows=@(@(5,6),@(5,7),@(4,8),@(4,10),@(3,12),@(2,13),@(1,13),@(2,14),@(3,13),@(3,12),@(5,10))}
        coal_dust=@{y=5;colors=@('#10181f','#293a47','#526574','#8d9fae','#bac7d0');rows=@(@(7,9),@(5,11),@(3,12),@(2,13),@(1,14),@(1,14),@(2,13),@(3,12),@(5,10))}
        steel_dust=@{y=4;colors=@('#28323b','#4f5d68','#8796a0','#cbd5d9','#eef4f4');rows=@(@(7,8),@(5,10),@(4,11),@(2,13),@(1,14),@(2,13),@(3,12),@(5,10),@(6,9))}
        gold_dust=@{y=3;colors=@('#674118','#b87823','#eeb737','#ffdf75','#fff3b4');rows=@(@(9,10),@(8,11),@(6,11),@(5,12),@(3,13),@(2,13),@(1,14),@(1,14),@(2,13),@(3,12),@(5,10))}
        diamond_dust=@{y=2;colors=@('#174953','#247f92','#42c7d8','#a4edf4','#f0ffff');rows=@(@(9,9),@(8,10),@(7,10),@(7,11),@(5,11),@(4,12),@(3,13),@(2,13),@(1,14),@(2,13),@(3,12),@(5,11))}
        netherite_dust=@{y=4;colors=@('#211e29','#403b50','#6c637b','#aaa1b6','#d8b3a0');rows=@(@(5,7),@(4,8),@(4,10),@(3,12),@(2,13),@(1,14),@(1,13),@(2,13),@(3,12),@(5,10))}
        quartz_sand=@{y=4;colors=@('#726852','#b6a77f','#e3d6ae','#fff0c9','#fffdf2');rows=@(@(7,9),@(6,10),@(5,11),@(3,12),@(2,13),@(1,14),@(1,14),@(2,13),@(3,12),@(5,10))}
    }
    $profiles['carbon_dust']=@{y=6;colors=@('#16191f','#292e35','#454d56','#737e88','#a5afb6');rows=@(@(6,7),@(5,9),@(4,11),@(2,12),@(1,14),@(2,13),@(3,12),@(5,10))}
    $profiles['uranium_dust']=@{y=4;colors=@('#263e24','#426537','#7ca74d','#bedb7c','#e6f4b3');rows=@(@(7,9),@(5,10),@(4,11),@(3,12),@(2,13),@(1,14),@(2,13),@(3,12),@(5,10),@(6,9))}
    $profile=$profiles[$name];$pixels=@{};$bottom=$profile.y+$profile.rows.Count-1
    for($row=0;$row -lt $profile.rows.Count;$row++){
        $bounds=$profile.rows[$row];$y=$row+$profile.y
        for($x=$bounds[0];$x -le $bounds[1];$x++){$pixels["$x,$y"]=@($x,$y)}
    }
    foreach($pixel in $pixels.Values){
        $x=$pixel[0];$y=$pixel[1]
        $edge=!$pixels.ContainsKey("$($x-1),$y") -or !$pixels.ContainsKey("$($x+1),$y") -or !$pixels.ContainsKey("$x,$($y-1)") -or !$pixels.ContainsKey("$x,$($y+1)")
        $shade=if($edge){0}elseif($y -ge $bottom-2){1}else{2}
        $image.SetPixel($x,$y,(Art-Color $profile.colors[$shade]))
    }
    foreach($grain in @(@(7,5),@(5,7),@(9,8),@(3,9),@(6,10),@(10,11))){
        $x=$grain[0];$y=$grain[1]
        if($pixels.ContainsKey("$x,$y") -and $pixels.ContainsKey("$($x+1),$y")){
            Art-Rect $image $x $y 2 1 $profile.colors[3]
            if($pixels.ContainsKey("$x,$($y+1)")){Art-Rect $image $x ($y+1) 1 1 $profile.colors[1]}
        }
    }
    foreach($grain in @(@(7,6),@(4,8),@(8,10))){if($pixels.ContainsKey("$($grain[0]),$($grain[1])")){Art-Rect $image $grain[0] $grain[1] 1 1 $profile.colors[4]}}
    Art-Rect $image 2 3 1 1 $profile.colors[2];Art-Rect $image 13 5 1 1 $profile.colors[3]
}
function Art-Item($image,$name) {
    if($name -eq 'raw_uranium'){Art-RawUranium $image;return}
    if($name -match '_battery$') {
        $accent=@{basic_battery='#63b4ad';intermediate_battery='#dfb85e';advanced_battery='#88b9e5';high_power_battery='#f29a55';long_life_battery='#ae91d4';emergency_battery='#e0a34f'}[$name]
        Art-Rect $image 6 1 4 2 '#b9c9cd';Art-Rect $image 4 3 8 11 '#24323f';Art-Rect $image 5 4 6 9 '#7f929f'
        Art-Line $image 5 4 5 12 '#ccd6d4';Art-Line $image 11 4 11 13 '#172736';Art-Rect $image 6 5 4 6 $accent
        Art-Rect $image 7 6 2 1 '#edf4df';Art-Rect $image 7 8 2 1 '#edf4df';Art-Rect $image 8 7 1 3 '#edf4df'
        $marks=@{basic_battery=1;intermediate_battery=2;advanced_battery=3;high_power_battery=3;long_life_battery=3;emergency_battery=2}[$name]
        for($i=0;$i -lt $marks;$i++){Art-Rect $image (5+$i*2) 12 1 1 $accent}
        if($name -eq 'high_power_battery'){Art-Rect $image 12 4 2 1 '#f29a55';Art-Rect $image 13 3 1 3 '#f29a55'}
        if($name -eq 'long_life_battery'){Art-Rect $image 12 4 2 3 '#ae91d4';Art-Rect $image 13 5 1 1 '#edf4df'}
        return
    }
    if($name -match '_dust$|^quartz_sand$'){Art-Powder $image $name;return}
    $art=@{
        fluid_sampling_gun=@{
            colors=@('#20303a','#476778','#73c3d1','#d7eaeb','#b8794b')
            rows=@('................','................','....ssssss......','...shhhhhs......','ssshbbbbbhssss..','shhhbgggbhhhhs..','sdddbgggbdddds..','.sssbgggbssss...','....shhhhs......','.....sgbss......','.....sgbs.......','....sgbbs.......','....sgbds.......','....ssss........','................','................')
        }
        uranium_ingot=@{
            colors=@('#253e27','#486c35','#83ac51','#c9e58b','#e6f4b3')
            rows=@('................','................','................','.......ssssss...','......shhhhggs..','.....shhhhggbbs.','....shhhhggbbbbs','...shhhhggbbbbds','..shhhhggbbbbdds','.shhhhggbbbbddds','.sgggggbbbbdddds','..sbbbbbbbdddds.','...sddddddddds..','....sssssssss...','................','................')
        }
        steel_ingot=@{
            colors=@('#202a37','#3e5065','#71869b','#cad9e5','#96adbf')
            rows=@('................','................','................','.......ssssss...','......shhhhggs..','.....shhhhggbbs.','....shhhhggbbbbs','...shhhhggbbbbds','..shhhhggbbbbdds','.shhhhggbbbbddds','.sgggggbbbbdddds','..sbbbbbbbdddds.','...sddddddddds..','....sssssssss...','................','................')
        }
        wrench=@{
            colors=@('#172938','#416073','#9cbac9','#edf7f9','#e8ac51')
            rows=@('.........ss.....','........shs..ss.','.......shbs.shhs','.......shbs.shbs','.......shbbhhbbs','........shbbdds.','.......shbbbds..','......shgbdds...','.....shggdds....','....shgbdds.....','...shhbdds......','..shhbdds.......','.shhbdds........','.shhbds.........','..sdds..........','...ss...........')
        }
        tutorial_book=@{
            colors=@('#20303e','#244654','#347b8b','#d6d9bb','#dba560')
            rows=@('................','...ssssssssss...','..sghhhhhhhhds..','..sgbbbbbbbbds..','..sgbbbbbbbbds..','..sgbbsssbbbds..','..sgbshhhsbbds..','..sgbshghsbbds..','..sgbshhhsbbds..','..sgbbsssbbbds..','..sgbbbbbbbbds..','..sgbbbbbbbbds..','..sghhhhhhhhds..','...sdddddddds...','....ssssssss....','................')
        }
        engineering_hammer=@{
            colors=@('#1e2b36','#765035','#be874d','#c5d9e3','#f3fafb')
            rows=@('....ssssssssss..','...shhggghhhhss.','..shhhhhhhhhhds.','..shhhhghhbbbds.','...sdddddddddds.','....ssssbbssss..','.......shbs.....','......shbds.....','.....shbbds.....','....shbbds......','...shbbds.......','..shbbds........','.shbbds.........','.shbds..........','..sss...........','................')
        }
    }
    if(!$art.ContainsKey($name)){throw "Missing item artwork: $name"}
    $sprite=$art[$name]
    if($sprite.rows.Count -ne 16 -or @($sprite.rows | Where-Object {$_.Length -ne 16}).Count){throw "Item grid must be exactly 16x16: $name"}
    Art-Grid $image $sprite.rows @{s=$sprite.colors[0];d=$sprite.colors[1];b=$sprite.colors[2];h=$sprite.colors[3];g=$sprite.colors[4]}
}
function Texture($kind,$name,$mode=-1,$blank=$false) {
    $image=[Drawing.Bitmap]::new(16,16)
    try{
        if($kind -eq 'item'){Art-Item $image $name}else{Art-Block $image $name $mode}
        $path=Join-Path $resourceRoot "assets/$mod/textures/$kind/$name.png"
        [IO.Directory]::CreateDirectory((Split-Path $path -Parent))|Out-Null
        $image.Save($path,[Drawing.Imaging.ImageFormat]::Png)
    }finally{$image.Dispose()}
}
function Glass-MaskValid($mask) {
    $corners=@(@(0,2),@(1,2),@(0,3),@(1,3))
    for($corner=0;$corner -lt 4;$corner++){
        if(($mask -band (1 -shl (4+$corner))) -and
            (!(($mask -band (1 -shl $corners[$corner][0]))) -or !(($mask -band (1 -shl $corners[$corner][1]))))){return $false}
    }
    return $true
}
