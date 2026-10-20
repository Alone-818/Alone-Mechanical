param([switch]$PipesOnly,[switch]$ItemsOnly,[switch]$HelpOnly)
$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path $PSScriptRoot -Parent
$resourceRoot = Join-Path $taskRoot 'src/main/resources'
$mod = 'alone_mechanical'
function Write-Json($relative, $value) {
    $path = Join-Path $resourceRoot $relative
    [IO.Directory]::CreateDirectory((Split-Path $path -Parent)) | Out-Null
    [IO.File]::WriteAllText($path, ($value | ConvertTo-Json -Depth 40) + "`n", [Text.UTF8Encoding]::new($false))
}
function Model($name, $value) { Write-Json "assets/$mod/models/block/$name.json" $value }
function Emergency-BatteryRecipe {
    Write-Json 'data/alone_mechanical/recipes/emergency_battery.json' ([ordered]@{
        type='minecraft:crafting_shapeless'
        category='misc'
        ingredients=@(
            @{item='minecraft:potato'}
            @{item='minecraft:redstone'}
            @{item='minecraft:copper_ingot'}
        )
        result=@{item='alone_mechanical:emergency_battery';count=2}
    })
}
if($HelpOnly){
    Emergency-BatteryRecipe
    . (Join-Path $PSScriptRoot 'generate_engineering_resources.ps1')
    Engineering-Resources
    . (Join-Path $PSScriptRoot 'generate_energy_matrix_resources.ps1')
    . (Join-Path $PSScriptRoot 'generate_tutorial_resources.ps1')
    . (Join-Path $PSScriptRoot 'generate_item_descriptions.ps1')
    . (Join-Path $PSScriptRoot 'generate_separator_resources.ps1')
    . (Join-Path $PSScriptRoot 'generate_pump_resources.ps1')
    . (Join-Path $PSScriptRoot 'generate_machine_component_resources.ps1')
    . (Join-Path $PSScriptRoot 'generate_generator_resources.ps1')
    . (Join-Path $PSScriptRoot 'generate_field_resources.ps1')
    . (Join-Path $PSScriptRoot 'generate_boiler_resources.ps1')
. (Join-Path $PSScriptRoot 'generate_nuclear_resources.ps1')
    . (Join-Path $PSScriptRoot 'generate_dimension_conversion_resources.ps1')
    . (Join-Path $PSScriptRoot 'generate_fluid_tool_resources.ps1')
    $help=[ordered]@{}
    foreach($table in @((Tutorial-Translations),(Item-DescriptionTranslations),(Separator-Translations),(Pump-Translations),(Machine-ComponentTranslations),(Generator-Translations))){foreach($entry in $table.GetEnumerator()){$help[$entry.Key]=$entry.Value}}
    foreach($entry in (Energy-MatrixTranslations).GetEnumerator()){$help[$entry.Key]=$entry.Value}
    foreach($entry in (Engineering-Translations).GetEnumerator()){$help[$entry.Key]=$entry.Value}
    foreach($entry in (Field-Translations).GetEnumerator()){$help[$entry.Key]=$entry.Value}
    foreach($entry in (Boiler-Translations).GetEnumerator()){$help[$entry.Key]=$entry.Value}
    foreach($entry in (Nuclear-Translations).GetEnumerator()){$help[$entry.Key]=$entry.Value}
    foreach($entry in (Dimension-ConversionTranslations).GetEnumerator()){$help[$entry.Key]=$entry.Value}
    foreach($entry in (Fluid-ToolTranslations).GetEnumerator()){$help[$entry.Key]=$entry.Value}
    for($i=0;$i -lt 2;$i++){
        $language=@('zh_cn','en_us')[$i]
        $current=Get-Content -Raw (Join-Path $resourceRoot "assets/$mod/lang/$language.json") | ConvertFrom-Json -AsHashtable
        foreach($entry in $help.GetEnumerator()){$current[$entry.Key]=$entry.Value[$i]}
        Write-Json "assets/$mod/lang/$language.json" $current
    }
    . (Join-Path $PSScriptRoot 'update_unified_core_resources.ps1')
    . (Join-Path $PSScriptRoot 'generate_tower_resources.ps1')
    . (Join-Path $PSScriptRoot 'generate_sulfuric_acid_resources.ps1')
    . (Join-Path $PSScriptRoot 'generate_air_separation_resources.ps1')
. (Join-Path $PSScriptRoot 'generate_machine_settings_resources.ps1')
    . (Join-Path $PSScriptRoot 'generate_heavy_water_resources.ps1')
    . (Join-Path $PSScriptRoot 'generate_creative_tank_resources.ps1')
    . (Join-Path $PSScriptRoot 'generate_fuel_agriculture_resources.ps1')
    . (Join-Path $PSScriptRoot 'generate_dimension_fluid_resources.ps1')
    . (Join-Path $PSScriptRoot 'generate_ore_processing_resources.ps1')
    . (Join-Path $PSScriptRoot 'generate_nonmetal_processing_resources.ps1')
    . (Join-Path $PSScriptRoot 'generate_reactor_safety_resources.ps1')
. (Join-Path $PSScriptRoot 'generate_uranium_worldgen.ps1')
    . (Join-Path $PSScriptRoot 'remove_machine_block_recipes.ps1')
    . (Join-Path $PSScriptRoot 'generate_manufacturing_resources.ps1')
    Write-Output 'Updated tutorial chapters and uniform Shift descriptions in Chinese and English.'
    return
}
# Original native-resolution pixel artwork, including connected transparent glass.
Add-Type -AssemblyName System.Drawing
. (Join-Path $PSScriptRoot 'generate_mod_textures.ps1')
. (Join-Path $PSScriptRoot 'generate_pipe_textures.ps1')
function Element($from, $to, $texture = '#all') {
    $faces = [ordered]@{}
    foreach ($face in @('down','up','north','south','west','east')) {
        $faces[$face] = @{ texture = $texture }
    }
    return @{ from = $from; to = $to; faces = $faces }
}
if($ItemsOnly){
    Texture 'item' 'fluid_sampling_gun'
    foreach($name in @('raw_uranium','uranium_ingot','uranium_dust')){Texture 'item' $name}
    foreach($name in @('iron_dust','copper_dust','coal_dust','carbon_dust','steel_dust','steel_ingot','gold_dust','diamond_dust','netherite_dust','quartz_sand','basic_battery','intermediate_battery','advanced_battery','high_power_battery','long_life_battery','emergency_battery','wrench','engineering_hammer','tutorial_book')){Texture 'item' $name}
    Write-Output 'Regenerated item sprites at native 16x16 resolution.'
    return
}
if($PipesOnly){
    foreach($kind in @('item','fluid','gas')){for($tier=1;$tier -le 5;$tier++){
        $id="${kind}_pipe_$tier"
        Write-Json "assets/$mod/blockstates/$id.json" @{multipart=@(Pipe-Models $id)}
        Write-Json "assets/$mod/models/item/$id.json" @{parent="${mod}:block/${id}_inventory"}
    }}
    Write-Output 'Regenerated 15 pipe models and their seamless textures.'
    return
}
$blocks = [ordered]@{
    uranium_ore = @('铀矿石','Uranium Ore','stone')
    deepslate_uranium_ore = @('深层铀矿石','Deepslate Uranium Ore','deepslate')
    machine_casing = @('机器外壳','Machine Casing','iron_block')
    reinforced_casing = @('强化外壳','Reinforced Casing','netherite_block')
    energy_coil = @('能量线圈','Energy Coil','copper_block')
    energy_matrix = @('能量矩阵','Energy Matrix','lapis_block')
    processing_matrix = @('处理矩阵','Processing Matrix','amethyst_block')
    efficiency_coil = @('效率线圈','Efficiency Coil','gold_block')
    structural_glass = @('结构玻璃','Structural Glass','glass')
    item_port = @('通用输入输出口','Universal I/O Port','orange_concrete')
    fluid_port = @('通用输入输出口','Universal I/O Port','blue_concrete')
    gas_port = @('通用输入输出口','Universal I/O Port','cyan_concrete')
    universal_port = @('通用输入输出口','Universal I/O Port','')
    machine_controller = @('通用机器控制器','Machine Controller','observer_top')
    crusher_core = @('粉碎核心','Crusher Core','')
    smelting_core = @('熔炼核心','Smelting Core','')
    industrial_core = @('工业核心','Industrial Core','')
    boiler_core = @('锅炉核心','Boiler Core','')
    turbine_core = @('涡轮核心','Turbine Core','')
    turbine_rotor = @('涡轮转子','Turbine Rotor','')
    forging_component = @('锻压组件','Forging Component','')
    forming_component = @('塑形组件','Forming Component','')
    infusion_component = @('灌注组件','Infusion Component','')
    slaughter_component = @('屠杀组件','Slaughter Component','')
    breeding_component = @('养殖组件','Breeding Component','')
    harvest_component = @('收割组件','Harvest Component','')
    repeller_component = @('驱逐组件','Repeller Component','')
    calcination_component = @('煅烧组件','Calcination Component','')
    alloy_component = @('合金组件','Alloy Component','')
    compound_component = @('化合组件','Combination Component','')
    decomposition_component = @('分解组件','Decomposition Component','')
    alloy_core = @('合金核心','Alloy Core','')
    separation_core = @('分离核心','Separation Core','')
    reaction_core = @('反应核心','Reaction Core','')
    conversion_core = @('转换核心','Conversion Core','')
    fluid_tank = @('储罐','Storage Tank','light_blue_concrete')
    gas_tank = @('储罐','Storage Tank','cyan_concrete')
    storage_tank = @('储罐','Storage Tank','')
    item_vault = @('物品仓','Item Vault','barrel_side')
    power_core = @('电网核心','Power Grid Core','emerald_block')
    signal_pole = @('信号杆','Signal Pole','iron_block')
    power_pylon = @('供电桩','Power Pylon','copper_block')
    creative_power_source = @('创造电源','Creative Power Source','diamond_block')
    solar_generator = @('基础光能发电机','Basic Solar Generator','')
    lunar_generator = @('夜光发电机','Luminous Solar Generator','')
    thermal_generator = @('热能发电机','Thermal Generator','')
    electric_pump = @('电动泵','Electric Pump','')
}
foreach ($kind in @('item','fluid','gas')) {
    $cn = @{ item = '物品'; fluid = '流体'; gas = '气体' }[$kind]
    $enKind = @{ item = 'Item'; fluid = 'Fluid'; gas = 'Gas' }[$kind]
    for ($i = 1; $i -le 5; $i++) { $blocks["${kind}_pipe_$i"] = @("$(@('赤铜','殷钢','共振','超导','量子')[$i-1])${cn}管道", "$(@('Red Copper','Invar','Resonant','Superconducting','Quantum')[$i-1]) $enKind Pipe", @('iron_block','copper_block','gold_block','diamond_block','netherite_block')[$i-1]) }
}
$zh = [ordered]@{}; $en = [ordered]@{}
foreach ($entry in $blocks.GetEnumerator()) {
    $id = $entry.Key; $texture = "${mod}:block/$id"; Texture 'block' $id
    $zh["block.$mod.$id"] = $entry.Value[0]; $en["block.$mod.$id"] = $entry.Value[1]
    Model $id @{ parent = 'minecraft:block/cube_all'; textures = @{ all = $texture } }
    $variants = [ordered]@{}
    if ($id -in @('signal_pole','power_pylon')) {
        for ($i = 0; $i -lt 5; $i++) {
            $parts = @((Element @(6,0,6) @(10,16,10)))
            if ($i -eq 0) { $parts += Element @(2,0,2) @(14,5,14) }
            if ($i -eq 4) { $parts += Element @(1,11,5) @(15,14,11) }
            Model "${id}_$i" @{ textures = @{ all = $texture; particle = $texture }; elements = $parts }
            $variants["part=$i"] = @{ model = "${mod}:block/${id}_$i" }
        }
        $itemParent = "${mod}:block/${id}_0"
    } elseif ($id -like '*_pipe_*') {
        $multipart = @(Pipe-Models $id)
        $itemParent = "${mod}:block/${id}_inventory"
    } elseif ($id -eq 'structural_glass') {
        Model $id @{ parent = 'minecraft:block/cube_all'; render_type = 'minecraft:cutout'; ambientocclusion = $false; textures = @{ all = $texture } }
        foreach($d in @('north','east','south','west')) { $variants["facing=$d"] = @{ model = "${mod}:block/$id" } }
        $itemParent = "${mod}:block/$id"
    } elseif ($id -eq 'electric_pump') {
        foreach($part in @('side','top','bottom','active')){Texture 'block' "${id}_$part"}
        $faces=@{side="${mod}:block/${id}_side";top="${mod}:block/${id}_top";bottom="${mod}:block/${id}_bottom";front=$texture}
        Model $id @{parent='minecraft:block/orientable_with_bottom';textures=$faces}
        $activeFaces=$faces.Clone();$activeFaces.front="${mod}:block/${id}_active"
        Model "${id}_active" @{parent='minecraft:block/orientable_with_bottom';textures=$activeFaces}
        foreach($d in @('north','east','south','west')){foreach($active in @('false','true')){$variants["facing=$d,lit=$active"]=@{model="${mod}:block/$(if($active -eq 'true'){"${id}_active"}else{$id})";y=@{north=0;east=90;south=180;west=270}[$d]}}}
        $itemParent="${mod}:block/$id"
    } elseif ($id -eq 'power_core') {
        $variants['part=0'] = @{ model = "${mod}:block/$id" }
        # Include every legal state, even though the single-block core always uses part 0.
        for ($i = 1; $i -lt 5; $i++) { $variants["part=$i"] = @{ model = "${mod}:block/$id" } }
        $itemParent = "${mod}:block/$id"
    } elseif ($id -in @('forming_component','infusion_component','forging_component','calcination_component','alloy_component','compound_component','decomposition_component','machine_casing','reinforced_casing','energy_coil','efficiency_coil','energy_matrix','processing_matrix','structural_glass','item_port','fluid_port','gas_port','machine_controller','fluid_tank','gas_tank','item_vault')) {
        if ($id -in @('machine_controller','fluid_tank','gas_tank','item_vault')) {
            Texture 'block' "${id}_side"; Texture 'block' "${id}_top"
            Model $id @{ parent = 'minecraft:block/orientable'; textures = @{ side = "${mod}:block/${id}_side"; front = $texture; top = "${mod}:block/${id}_top" } }
            if($id -eq 'fluid_tank') { Model $id @{ parent = 'minecraft:block/orientable'; render_type = 'minecraft:cutout'; ambientocclusion = $false; textures = @{ side = "${mod}:block/${id}_side"; front = $texture; top = "${mod}:block/${id}_top" } } }
        }
        if ($id -in @('item_port','fluid_port','gas_port')) {
            for ($mode=0;$mode -lt 4;$mode++) {
                Texture 'block' "${id}_$mode" $mode
                Model "${id}_$mode" @{ parent = 'minecraft:block/orientable'; textures = @{ side = "${mod}:block/machine_casing"; top = "${mod}:block/machine_casing_top"; front = "${mod}:block/${id}_$mode" } }
                foreach ($d in @('north','east','south','west')) { $variants["facing=$d,io_mode=$mode"] = @{ model = "${mod}:block/${id}_$mode"; y = @{ north = 0; east = 90; south = 180; west = 270 }[$d] } }
            }
            $itemParent = "${mod}:block/${id}_0"
        } else {
            foreach ($d in @('north','east','south','west')) { $variants["facing=$d"] = @{ model = "${mod}:block/$id"; y = @{ north = 0; east = 90; south = 180; west = 270 }[$d] } }
            $itemParent = "${mod}:block/$id"
        }
    } else { $variants[''] = @{ model = "${mod}:block/$id" }; $itemParent = "${mod}:block/$id" }
    if($id -in @('fluid_tank','gas_tank','storage_tank','item_vault')){
        $family=if($id -eq 'item_vault'){'item_vault'}else{'storage_tank'}
        Model $id @{parent='minecraft:block/cube_bottom_top';render_type='minecraft:cutout';ambientocclusion=$false;textures=@{side="${mod}:block/${family}_ct_side_0";top="${mod}:block/${family}_ct_top_0";bottom="${mod}:block/${family}_ct_top_0";particle=$texture}}
        $variants=[ordered]@{};foreach($d in @('north','east','south','west')){$variants["facing=$d"]=@{model="${mod}:block/$id"}}
    }
    if($id -in @('item_port','fluid_port','gas_port','universal_port')){
        $variants=[ordered]@{};for($mode=0;$mode -lt 4;$mode++){
            Texture 'block' "universal_port_$mode" $mode
            Model "${id}_$mode" @{parent='minecraft:block/cube_column';textures=@{side="${mod}:block/universal_port_$mode";end="${mod}:block/machine_casing_top";particle=$texture}}
            foreach($d in @('north','east','south','west')){$variants["facing=$d,io_mode=$mode"]=@{model="${mod}:block/${id}_$mode";y=@{north=0;east=90;south=180;west=270}[$d]}}
        };$itemParent="${mod}:block/${id}_0"
    }
    if ($id -like '*_pipe_*') { Write-Json "assets/$mod/blockstates/$id.json" @{ multipart = $multipart } }
    else { Write-Json "assets/$mod/blockstates/$id.json" @{ variants = $variants } }
    Write-Json "assets/$mod/models/item/$id.json" @{ parent = $itemParent }
    $conditions = @(@{ condition = 'minecraft:survives_explosion' })
    if ($id -in @('signal_pole','power_pylon')) { $conditions += @{ condition = 'minecraft:block_state_property'; block = "${mod}:$id"; properties = @{ part = '0' } } }
    $dropId=if($id -eq 'separation_core'){'decomposition_component'}elseif($id -in @('item_port','fluid_port','gas_port')){'universal_port'}elseif($id -in @('fluid_tank','gas_tank')){'storage_tank'}else{$id}
    Write-Json "data/$mod/loot_tables/blocks/$id.json" @{ type = 'minecraft:block'; pools = @(@{ rolls = 1; entries = @(@{ type = 'minecraft:item'; name = "${mod}:$dropId" }); conditions = $conditions }) }
}
Texture 'block' 'machine_casing_top'
foreach($family in @('storage_tank','item_vault')){foreach($surface in @('side','top')){
    $count=if($surface -eq 'side'){4}else{16}
    for($mask=0;$mask -lt $count;$mask++){$name="${family}_ct_${surface}_$mask";Texture 'block' $name;Model $name @{parent='minecraft:block/cube_all';render_type='minecraft:cutout';ambientocclusion=$false;textures=@{all="${mod}:block/$name"}}}
}}
Texture 'block' 'tank_gas';Model 'tank_gas' @{parent='minecraft:block/cube_all';textures=@{all="${mod}:block/tank_gas"}}
# Keep old mode sprites compatible with existing development resource references.
foreach($storage in @('fluid_tank','gas_tank','item_vault')) { for($mode=0;$mode -lt 4;$mode++){Texture 'block' "${storage}_$mode" $mode} }
# 47 legal edge/corner combinations, each an independent 16x16 transparent PNG.
for($mask=0;$mask -lt 256;$mask++) {
    if(!(Glass-MaskValid $mask)){continue}
    $id="structural_glass_ct_$mask"; Texture 'block' $id
    Model $id @{ parent = 'minecraft:block/cube_all'; render_type = 'minecraft:cutout'; ambientocclusion = $false; textures = @{ all = "${mod}:block/$id" } }
}
# North-facing collars are rotated by the client renderer, without adding block-state combinations.
# A six-pixel pipe receives an eight-pixel sleeve: one pixel larger on each side.
Pipe-Texture 'pipe_connector_body' @(110,125,139) 'collar'
Pipe-Texture 'pipe_pull_mode' @(66,146,228) 'mode'
Pipe-Texture 'pipe_push_mode' @(232,144,54) 'mode'
foreach ($mode in @('pull','push')) {
    $z0 = if ($mode -eq 'pull') { 1.8 } else { .55 }
    $parts = @((Pipe-Panel @(4,4,0) @(12,12,3)), (Pipe-Panel @(3.98,3.98,$z0) @(12.02,12.02,($z0+.65)) '#mode'))
    Model "pipe_${mode}_connector" @{ textures = @{ all = "${mod}:block/pipe_connector_body"; mode = "${mod}:block/pipe_${mode}_mode"; particle = "${mod}:block/pipe_connector_body" }; elements = $parts }
}
$items = [ordered]@{
    wrench = @('机械扳手','Mechanical Wrench','stone_axe')
    engineering_hammer = @('工程锤','Engineering Hammer','iron_axe')
    tutorial_book = @('教程书','Tutorial Book','book')
}
$materials = [ordered]@{
    basic_battery = @('初级电池','Basic Battery')
    intermediate_battery = @('中级电池','Intermediate Battery')
    advanced_battery = @('高级电池','Advanced Battery')
    high_power_battery = @('高级电池 · 高功率','Advanced Battery · High Power')
    long_life_battery = @('高级电池 · 长续航','Advanced Battery · Long Life')
    emergency_battery = @('应急电池','Emergency Battery')
    iron_dust = @('铁粉','Iron Dust')
    copper_dust = @('铜粉','Copper Dust')
    coal_dust = @('煤粉','Coal Dust')
    carbon_dust = @('碳粉','Carbon Dust')
    steel_dust = @('钢粉','Steel Dust')
    steel_ingot = @('钢锭','Steel Ingot')
    uranium_ingot = @('铀锭','Uranium Ingot')
    raw_uranium = @('粗铀','Raw Uranium')
    uranium_dust = @('铀粉','Uranium Dust')
    gold_dust = @('金粉','Gold Dust')
    diamond_dust = @('钻石粉','Diamond Dust')
    netherite_dust = @('下界合金粉末','Netherite Dust')
    quartz_sand = @('石英砂','Quartz Sand')
}
foreach ($entry in $materials.GetEnumerator()) { $items[$entry.Key] = $entry.Value }
foreach ($entry in $items.GetEnumerator()) {
    $id = $entry.Key; $zh["item.$mod.$id"] = $entry.Value[0]; $en["item.$mod.$id"] = $entry.Value[1]
    if ($materials.Contains($id)) {
        Texture 'item' $id
        $parent = 'minecraft:item/generated'
    } else {
        Texture 'item' $id
        $parent = if($id -in @('wrench','engineering_hammer')) {'minecraft:item/handheld'} else {'minecraft:item/generated'}
    }
    Write-Json "assets/$mod/models/item/$id.json" @{ parent = $parent; textures = @{ layer0 = "${mod}:item/$id" } }
}
$text = [ordered]@{
    'tab.alone_mechanical.blocks' = @('孤独机械 · 方块','Alone Mechanical · Blocks')
    'tab.alone_mechanical.items' = @('孤独机械 · 物品','Alone Mechanical · Items')
    'tab.alone_mechanical.misc' = @('孤独机械 · 杂项','Alone Mechanical · Misc')
    'machine.alone_mechanical.crusher' = @('粉碎机','Crusher')
    'machine.alone_mechanical.arc_furnace' = @('电弧炉','Arc Furnace')
    'machine.alone_mechanical.alloy_smelter' = @('合金冶炼机','Alloy Smelter')
    'message.alone_mechanical.requires_alloy_stores' = @('合金冶炼机需要恰好三个独立缓存列：输入1物品仓、输入2物品仓/流体储罐/气体储罐、输出1物品仓','Alloy smelter needs exactly three separate columns: item Input 1, item/fluid/gas Input 2 and item Output 1')
    'jei.alone_mechanical.alloy_smelter' = @('合金冶炼机：中心放合金核心，外表面一个控制器及至少一个能量矩阵。恰好三个独立缓存列，主料为输入1物品仓，辅料为输入2物品仓/流体储罐/气体储罐，输出1为物品仓。端口紧贴缓存列顶部或底部。2铁粉+1碳粉→2钢锭；煤或木炭可粉碎为碳粉。支持线圈和处理矩阵。','Alloy smelter: central alloy core, one surface controller and at least one energy matrix. Exactly three separate columns: item Input 1, item/fluid/gas Input 2 and item Output 1, with ports directly above or below. 2 iron dust + 1 carbon dust → 2 steel ingots. Crush coal or charcoal for carbon dust. Supports coils and processing matrices.')
    'machine.alone_mechanical.gas_liquid_converter' = @('气液转换机','Gas-Liquid Converter')
    'conversion.alone_mechanical.gas_to_liquid' = @('液化：气体 → 液体','Liquefy: Gas → Liquid')
    'conversion.alone_mechanical.liquid_to_gas' = @('气化：液体 → 气体','Vaporize: Liquid → Gas')
    'gui.alone_mechanical.conversion_help' = @('切换转换类型并自动反转两侧端口；重置加工进度，保留储罐内容','Select conversion direction and reverse both ports. Resets processing progress and preserves tank contents.')
    'message.alone_mechanical.requires_converter_tanks' = @('气液转换机需要恰好一个独立流体储罐列和一个独立气体储罐列，不使用物品仓','The converter needs exactly one independent fluid tank column and one independent gas tank column, without item vaults')
    'message.alone_mechanical.requires_converter_ports' = @('气液转换机的两个仓储列须使用对应通用端口，分别设置输入1和输出1；端口位于各列顶部或底部','Use universal ports above or below the two columns, set to Input 1 and Output 1')
    'message.alone_mechanical.converter_direction_mismatch' = @('端口方向与转换类型不符，请恢复端口或用工程锤重新检测；成型后通过GUI选择转换类型','Ports do not match the selected conversion type. Restore the ports or reassemble with the hammer, then select the type in the GUI.')
    'message.alone_mechanical.converter_formed' = @('成型成功：气液转换机 · %s；可在GUI选择液化或气化','Assembled: Gas-Liquid Converter · %s; select liquefaction or vaporization in the GUI')
    'jei.alone_mechanical.gas_liquid_converter' = @('气液转换机：中心放转换核心，完整外壳、一个控制器及至少一个能量矩阵。恰好一个流体储罐列和一个气体储罐列，对应端口放在各列顶部或底部，分别设输入1、输出1。成型后在GUI选择液化（气体→液体）或气化（液体→气体），自动切换端口方向。每段储罐16000 mB。转换配方由数据包提供，实际消耗、产量和耗能按配方执行。','Gas-Liquid Converter: central conversion core, complete casing, one controller and at least one energy matrix. Exactly one fluid tank column and one gas tank column, with matching ports above/below, set to Input 1 and Output 1. Select liquefaction or vaporization in the GUI to reverse the ports automatically. Each tank segment holds 16000 mB. Data packs define conversion pairs, quantities and energy costs.')
    'machine.alone_mechanical.liquid_reactor' = @('双相反应釜 · 液相合制','Biphasic Reactor · Liquid Synthesis')
    'machine.alone_mechanical.gas_reactor' = @('双相反应釜 · 气相化合','Biphasic Reactor · Gas Combination')
    'machine.alone_mechanical.dissolution' = @('双相反应釜 · 溶出制液','Biphasic Reactor · Dissolution')
    'machine.alone_mechanical.crystallization' = @('双相反应釜 · 溶解析晶','Biphasic Reactor · Crystallization')
    'machine.alone_mechanical.liquid_capture' = @('双相反应釜 · 液相捕收','Biphasic Reactor · Liquid Capture')
    'machine.alone_mechanical.gas_evolution' = @('双相反应釜 · 液相生气','Biphasic Reactor · Gas Evolution')
    'machine.alone_mechanical.gas_solid_refining' = @('双相反应釜 · 气固相炼','Biphasic Reactor · Gas-Solid Refining')
    'machine.alone_mechanical.solid_gasification' = @('双相反应釜 · 固转气化','Biphasic Reactor · Solid Gasification')
    'machine.alone_mechanical.solid_fusion' = @('双相反应釜 · 固相熔合','Biphasic Reactor · Solid Fusion')
    'message.alone_mechanical.requires_three_reactor_stores' = @('双相反应釜须恰好三个独立仓储列：两个输入仓和一个输出仓，可使用物品仓、流体储罐或气体储罐','The biphasic reactor needs exactly three independent storage columns: two inputs and one output, using item vaults, fluid tanks or gas tanks')
    'message.alone_mechanical.requires_reactor_ports' = @('三个独立仓储列须分别设置输入1、输入2、输出1；通用端口必须紧贴每列顶部或底部','Set the three independent columns to Input 1, Input 2 and Output 1, using universal ports directly above or below each column')
    'message.alone_mechanical.unsupported_reactor_profile' = @('仓储组合不支持：物品+液体→液体/物品；液体+气体→液体/气体；物品+气体→物品/气体；同类+同类→同类','Unsupported storage combination: item+fluid to fluid/item; fluid+gas to fluid/gas; item+gas to item/gas; two matching resources to the same resource type')
    'message.alone_mechanical.reactor_formed' = @('成型成功：%s；输入1：%s，输入2：%s，输出1：%s','Assembled: %s; Input 1: %s, Input 2: %s, Output 1: %s')
    'message.alone_mechanical.hammer_corner' = @('已框选起点（%s, %s, %s）：手持工程锤查看青色范围，右键另一对角点检测','First corner selected (%s, %s, %s): hold the hammer to preview the cyan region, then click the opposite corner')
    'gui.alone_mechanical.hammer_bounds' = @('工程锤选区：%s × %s × %s','Hammer selection: %s × %s × %s')
    'gui.alone_mechanical.hammer_help' = @('右键另一对角点检测 · Shift右键取消','Click opposite corner to assemble · Sneak-click to cancel')
    'gui.alone_mechanical.reactor_channel' = @('%s · %s','%s · %s')
    'gui.alone_mechanical.empty_store' = @('未连接仓储','No storage')
    'gui.alone_mechanical.empty_gas' = @('空气体储罐','Empty gas tank')
    'gui.alone_mechanical.vault_size_0' = @('小 · 27','Small · 27')
    'gui.alone_mechanical.vault_size_1' = @('中 · 54','Medium · 54')
    'gui.alone_mechanical.vault_size_2' = @('大 · 81','Large · 81')
    'gui.alone_mechanical.tank_auto' = @('空罐自动识别液体或气体','Empty tanks detect liquid or gas')
    'gui.alone_mechanical.tank_fluid' = @('储罐 · 液体','Tank · Liquid')
    'gui.alone_mechanical.tank_gas' = @('储罐 · 气体','Tank · Gas')
    'jei.alone_mechanical.biphasic_reactor' = @('双相反应釜：反应核心不变。一个控制器、至少一个能量矩阵，恰好三个独立仓储列。各列顶部或底部放对应通用端口，设为输入1、输入2、输出1。两个输入可交换通道。按物品仓、流体储罐、气体储罐的输入/输出组合识别九种模式：溶出制液、溶解析晶、液相捕收、液相生气、气固相炼、固转气化、液相合制、气相化合、固相熔合。工程锤框选显示范围及尺寸，成型消息显示识别模式及三个通道。','Biphasic reactor: same reaction core, one controller and at least one energy matrix. Exactly three independent storage columns with matching ports above or below, set to Input 1, Input 2 and Output 1. Inputs may be swapped. Item/fluid/gas input and output stores determine one of nine reaction modes. The hammer previews bounds and dimensions; assembly reports the mode and channels.')
    'message.alone_mechanical.requires_three_fluid_tanks' = @('液体反应机必须有恰好三个独立流体储罐列；竖向相连的储罐算一个整体','The liquid reactor requires exactly three independent fluid tank columns; stacked tanks count as one column')
    'message.alone_mechanical.requires_reactor_fluid_ports' = @('三个独立储罐列须分别连接输入1、输入2和输出1通用端口','Connect the three separate tank columns to Input 1, Input 2 and Output 1 fluid ports')
    'gui.alone_mechanical.fluid_input_title' = @('液体输入','Fluid input')
    'gui.alone_mechanical.fluid_output_title' = @('液体输出','Fluid output')
    'gui.alone_mechanical.empty_fluid' = @('空储罐','Empty tank')
    'gui.alone_mechanical.fluid_capacity' = @('%s / %s mB','%s / %s mB')
    'config.jade.plugin_alone_mechanical.gas_storage' = @('气体储量','Gas storage')
    'jei.alone_mechanical.liquid_reactor' = @('液体反应机：中心放反应核心，外壳需要一个控制器及至少一个能量矩阵。恰好三个独立流体储罐列，通用端口放在各列顶部或底部，分别设置输入1、输入2、输出1。不需要物品仓。竖向叠罐扩容，每段16000 mB。','Liquid reactor: central reaction core, one surface controller and at least one energy matrix. Exactly three independent fluid tank columns, with universal ports directly above or below them, set to Input 1, Input 2 and Output 1. No item vaults required. Each stacked tank adds 16000 mB.')
    'mode.alone_mechanical.input_1' = @('输入 1','Input 1')
    'mode.alone_mechanical.input_2' = @('输入 2','Input 2')
    'mode.alone_mechanical.output_1' = @('输出 1','Output 1')
    'mode.alone_mechanical.output_2' = @('输出 2','Output 2')
    'message.alone_mechanical.selected' = @('已选择起点，右键另一节点进行连线或断线','Start selected. Click another node to connect or disconnect.')
    'message.alone_mechanical.connected' = @('连线成功','Connected')
    'message.alone_mechanical.disconnected' = @('已断开连线','Disconnected')
    'message.alone_mechanical.cancelled' = @('已取消连线','Selection cancelled')
    'message.alone_mechanical.same_node' = @('不能连接自身','Cannot connect a node to itself')
    'message.alone_mechanical.too_far' = @('超出连接距离：信号杆80米，供电桩60米','Too far: signal 80m, pylon 60m')
    'message.alone_mechanical.multiple_cores' = @('同一电网只允许一个电网核心','A grid can contain only one core')
    'message.alone_mechanical.missing_node' = @('起点已移除或区块未加载','Node removed or chunk unloaded')
    'message.alone_mechanical.invalid_structure' = @('选区外表面须完整使用机器外壳类方块，内部仅允许空气或机器部件','The cuboid surface needs casing blocks; the interior allows only air or machine parts')
    'message.alone_mechanical.use_hammer' = @('请用工程锤右键长方体的两个对角点进行整体检测','Use an engineering hammer on two opposite corners to assemble.')
    'message.alone_mechanical.hammer_selected' = @('已选第一个角点；右键另一对角点检测，Shift右键取消','First corner selected. Select the opposite corner; sneak-click cancels.')
    'message.alone_mechanical.hammer_cancelled' = @('已取消工程锤选区；下次右键选择新起点','Hammer selection cancelled; next click selects a new first corner')
    'message.alone_mechanical.invalid_dimensions' = @('范围的长、宽、高分别须为3至%s格，允许三边不等长','Select a cuboid with each dimension from 3 to %s blocks')
    'message.alone_mechanical.build_failed' = @('构建失败：%s','Construction failed: %s')
    'message.alone_mechanical.failure_position' = @('%s（坐标 %s, %s, %s）','%s (at %s, %s, %s)')
    'message.alone_mechanical.unknown_machine' = @('不存在这种多方块结构，中心须放置粉碎核心、熔炼核心、合金核心、反应核心或转换核心','Unknown multiblock type; place a crusher, smelting, alloy, reaction or conversion core at the center')
    'message.alone_mechanical.extra_machine_core' = @('只允许中心放置一个机器核心','Only one machine core is allowed, at the center')
    'message.alone_mechanical.missing_independent_vaults' = @('至少需要两个独立物品仓；竖向相连的仓算一个整体','At least two independent vault columns are required')
    'message.alone_mechanical.missing_input_vault' = @('缺少至少27槽的输入物品仓，请将相邻通用端口设为输入1','Needs at least 27 input slots; set the attached port to Input 1')
    'message.alone_mechanical.missing_output_vault' = @('缺少至少27槽的输出物品仓，请将独立仓的通用端口设为输出1','Needs at least 27 output slots; set a separate store port to Output 1')
    'message.alone_mechanical.missing_energy_module' = @('至少需要一个能量模块（能量矩阵）','Needs at least one energy module (Energy Matrix)')
    'message.alone_mechanical.incomplete_shell' = @('外壳不完整，此处需要机器结构部件','Incomplete shell; place a machine part here')
    'message.alone_mechanical.invalid_interior' = @('内部存在非机器部件','Interior contains a non-machine block')
    'message.alone_mechanical.missing_block_entity' = @('机器部件数据缺失，请重新放置该方块','Missing machine part data; replace this block')
    'message.alone_mechanical.outside_world' = @('选区超出世界高度范围','Selected region exceeds the world height limits')
    'message.alone_mechanical.construction_error' = @('构建过程发生异常，机器已停止；详细原因见服务器日志','Unexpected construction error; machine stopped. See the server log for details')
    'message.alone_mechanical.unloaded_structure' = @('选区存在未加载区块','Selected region contains unloaded chunks')
    'message.alone_mechanical.missing_machine_core' = @('长方体中心缺少机器核心，须放置粉碎核心、熔炼核心、合金核心、反应核心或转换核心','The cuboid center needs a crusher, smelting, alloy, reaction or conversion core')
    'message.alone_mechanical.wrong_controller' = @('外表面必须且只能有一个控制器','The surface needs exactly one controller')
    'message.alone_mechanical.overlapping_machine' = @('选区与另一台已成型机器重叠','The region overlaps another formed machine')
    'message.alone_mechanical.storage_crosses_boundary' = @('竖向缓存整体超出机器选区，请断开选区外的连接','A storage column crosses the machine boundary')
    'message.alone_mechanical.valid_structure' = @('结构符合要求','Structure valid')
    'message.alone_mechanical.formed_structure' = @('整体检测成功，机器已成型','Whole-region check passed; machine assembled')
    'gui.alone_mechanical.storage_amount' = @('缓存 %s / 容量 %s','Stored %s / Capacity %s')
    'gui.alone_mechanical.storage_height' = @('竖向连接 %s 个方块','Vertical column: %s blocks')
    'gui.alone_mechanical.inputs' = @('输入库存 · %s槽','Input inventory · %s slots')
    'gui.alone_mechanical.outputs' = @('输出库存 · %s槽','Output inventory · %s slots')
    'message.alone_mechanical.port_requires_storage' = @('通用端口必须紧贴对应仓库列的顶部或底部，且只能连接一列','A port must touch the top or bottom of exactly one matching storage column')
    'message.alone_mechanical.storage_requires_port' = @('仓库列缺少对应通用端口，请放在顶部或底部','Storage needs a matching port directly above or below its column')
    'message.alone_mechanical.port_crosses_boundary' = @('对应仓库端口在机器选区外','The storage port lies outside the machine boundary')
    'message.alone_mechanical.conflicting_storage_ports' = @('同一仓库列不能共用不同通道；输入仓与输出仓必须独立','A storage column cannot share different channels; input and output stores must be separate')
    'gui.alone_mechanical.storage_only' = @('仅存储 · 输入输出由相邻端口负责','Storage only · I/O through attached ports')
    'jei.alone_mechanical.structure' = @('最小3×3×3长方体，长宽高允许不同，中心放置对应核心，外壳需要一个控制器和至少一个能量模块。至少两个独立物品仓，输入与输出各27槽；每叠加一个物品仓增加27槽。通用端口紧贴对应仓库列的顶部或底部，由扳手设置输入或输出，同一列不能共用输入与输出。','Minimum 3×3×3 cuboid (unequal sides allowed): a central core, one surface controller and at least one energy module. Use separate input and output vault columns with at least 27 slots each. Each stacked vault adds 27 slots. Universal ports must sit directly above or below a column; configure ports with the wrench. Input and output cannot share a column.')
    'jei.alone_mechanical.smelting' = @('熔炼核心与物品仓构成电弧炉，兼容当前数据包及其他模组的熔炼配方。使用电力，不需要燃料。','A smelting core with item vaults forms an arc furnace. Uses current vanilla, data pack and mod smelting recipes. Consumes electricity without furnace fuel.')
    'jei.alone_mechanical.duration' = @('耗时 %s tick','Duration: %s ticks')
    'jei.alone_mechanical.energy' = @('基础耗能 %s FE/t','Base energy: %s FE/t')
    'gui.alone_mechanical.unformed' = @('结构未成型','Incomplete structure')
    'tooltip.alone_mechanical.processing_matrix' = @('每个处理矩阵使并行加工数量翻倍：2 / 4 / 8 / 16 / 32 份','Each processing matrix doubles parallel recipes: 2 / 4 / 8 / 16 / 32')
    'tooltip.alone_mechanical.processing_matrix_limit' = @('最多 5 个生效；实际耗能按并行份数增加','Up to 5 effective matrices; energy scales with actual parallel recipes')
    'gui.alone_mechanical.processing_detail' = @('处理矩阵 %s/5 · 并行上限 %s 份','Processing matrices %s/5 · Up to %s parallel recipes')
    'gui.alone_mechanical.parallel_value' = @('并行 %s / %s','Parallel %s / %s')
    'gui.alone_mechanical.input_title' = @('物品输入','Item input')
    'gui.alone_mechanical.output_title' = @('物品输出','Item output')
    'gui.alone_mechanical.slot_count' = @('%s 槽','%s slots')
    'gui.alone_mechanical.previous_page' = @('上一页','Previous page')
    'gui.alone_mechanical.next_page' = @('下一页','Next page')
    'gui.alone_mechanical.page_number' = @('第 %s / %s 页','Page %s / %s')
    'gui.alone_mechanical.status_tab' = @('运行状态','Status')
    'gui.alone_mechanical.cache_tab' = @('流体 / 气体','Fluid / Gas')
    'gui.alone_mechanical.waiting_input' = @('等待原料','Waiting for input')
    'gui.alone_mechanical.low_energy' = @('电量不足','Low energy')
    'gui.alone_mechanical.output_blocked' = @('输出空间不足','Output blocked')
    'gui.alone_mechanical.status_help' = @('加工需要匹配的原料、足够电量及输出空间。','Processing needs matching input, enough energy and output space.')
    'gui.alone_mechanical.progress_label' = @('加工进度','Processing progress')
    'gui.alone_mechanical.progress_detail' = @('加工进度：%s / %s 基础 tick','Progress: %s / %s base ticks')
    'gui.alone_mechanical.energy_usage' = @('耗能  %s FE/t','Energy  %s FE/t')
    'gui.alone_mechanical.speed_value' = @('加工速度  %s%%','Speed  %s%%')
    'gui.alone_mechanical.battery_label' = @('电池','Battery')
    'gui.alone_mechanical.upgrade_summary' = @('节能 %s · 效率 %s · 矩阵 %s','Energy %s · Speed %s · Matrix %s')
    'gui.alone_mechanical.matrix_detail' = @('能量矩阵 %s 个 · 每个增加 100%% 基础电池容量','%s energy matrices · +100%% base battery capacity each')
    'gui.alone_mechanical.reinforced_detail' = @('强化外壳 %s 个','Reinforced casings: %s')
    'gui.alone_mechanical.fluid_label' = @('流体缓存','Fluid buffers')
    'gui.alone_mechanical.gas_label' = @('气体缓存','Gas buffers')
    'gui.alone_mechanical.channel_0' = @('输入1','In 1')
    'gui.alone_mechanical.channel_1' = @('输入2','In 2')
    'gui.alone_mechanical.channel_2' = @('输出1','Out 1')
    'gui.alone_mechanical.channel_3' = @('输出2','Out 2')
    'gui.alone_mechanical.cache_channel' = @('%s：%s mB','%s: %s mB')
    'gui.alone_mechanical.running' = @('加工中','Processing')
    'gui.alone_mechanical.idle' = @('等待电力、原料或输出空间','Waiting for power / input / space')
    'gui.alone_mechanical.cost' = @('耗能 %s FE/t · 速度 %s%%','%s FE/t · Speed %s%%')
    'gui.alone_mechanical.coils' = @('能量线圈 %s/8 · 效率线圈 %s/8','Energy coils %s/8 · Speed coils %s/8')
    'gui.alone_mechanical.battery' = @('电池 %s/%s FE · 矩阵 %s','Battery %s/%s FE · Matrices %s')
    'gui.alone_mechanical.fluids' = @('液体 I1/I2/O1/O2: %s/%s/%s/%s','Fluid I1/I2/O1/O2: %s/%s/%s/%s')
    'gui.alone_mechanical.gases' = @('气体 I1/I2/O1/O2: %s/%s/%s/%s','Gas I1/I2/O1/O2: %s/%s/%s/%s')
    'gui.alone_mechanical.online' = @('电网有电','Grid powered')
    'gui.alone_mechanical.offline' = @('电网无电 / 未接入核心','No power / no connected core')
    'gui.alone_mechanical.energy' = @('储能 %s / %s FE','Energy %s / %s FE')
    'gui.alone_mechanical.links' = @('连接 %s 条 · 最远 %s 米','%s links · Range %s m')
    'gui.alone_mechanical.link_help' = @('点击开始连线，再右键目标节点；重复连接可断线。潜行右键节点取消。','Click Start wiring, then right-click a target. Repeat to disconnect. Sneak-click a node to cancel.')
    'gui.alone_mechanical.start_link' = @('开始连线','Start wiring')
    'gui.alone_mechanical.cancel_link' = @('取消连线','Cancel wiring')
    'pipe.alone_mechanical.normal' = @('普通（接收/输送）','Normal (receive / send)')
    'pipe.alone_mechanical.pull' = @('拉取（主动抽取）','Pull (extract)')
    'pipe.alone_mechanical.push' = @('推送（仅输出）','Push (output only)')
    'pipe.alone_mechanical.none' = @('断开','Disconnected')
    'pipe.alone_mechanical.side' = @('端面 %s：%s；Shift+扳手右键切换','Side %s: %s; sneak-click with a wrench to change')
    'tooltip.alone_mechanical.pipe.expand' = @('按住 [%s] 查看详细信息','Hold [%s] for details')
    'tooltip.alone_mechanical.pipe.summary.item' = @('在容器与机器之间自动输送物品。','Automatically transports items between inventories and machines.')
    'tooltip.alone_mechanical.pipe.summary.fluid' = @('在储罐与机器之间自动输送流体。','Automatically transports fluids between tanks and machines.')
    'tooltip.alone_mechanical.pipe.summary.gas' = @('在储罐与机器之间自动输送气体。','Automatically transports gases between tanks and machines.')
    'tooltip.alone_mechanical.pipe.tier' = @('管道等级：%s','Tier: %s')
    'tooltip.alone_mechanical.pipe.resource' = @('传输介质：%s','Transports: %s')
    'tooltip.alone_mechanical.pipe.resource.item' = @('物品','Items')
    'tooltip.alone_mechanical.pipe.resource.fluid' = @('流体','Fluids')
    'tooltip.alone_mechanical.pipe.resource.gas' = @('气体','Gases')
    'tooltip.alone_mechanical.pipe.rate' = @('最大传输速率：%s','Maximum transfer rate: %s')
    'tooltip.alone_mechanical.pipe.rate.items' = @('%s 个/秒','%s items/s')
    'tooltip.alone_mechanical.pipe.rate.mb' = @('%s mB/秒','%s mB/s')
    'tooltip.alone_mechanical.pipe.capacity' = @('单段缓存：%s','Buffer per segment: %s')
    'tooltip.alone_mechanical.pipe.network' = @('六面自动连接 · 混级路径受最慢段限制','Connects on all six sides; mixed-tier routes use the slowest segment.')
    'tooltip.alone_mechanical.pipe.modes' = @('端面配置','Connection modes')
    'tooltip.alone_mechanical.pipe.normal' = @('普通：接收与输送，不主动抽取','Normal: receives and sends; no active extraction')
    'tooltip.alone_mechanical.pipe.pull' = @('拉取 / 抽入：大一圈蓝色接头，主动从容器抽取','Pull / intake: enlarged blue collar, extracts from containers')
    'tooltip.alone_mechanical.pipe.push' = @('推送 / 抽出：大一圈橙色接头，向容器送出','Push / output: enlarged orange collar, sends to containers')
    'tooltip.alone_mechanical.pipe.split' = @('分流：可用出口均分，满仓跳过，余数轮换','Distribution: equal shares, skips full outputs, rotates remainders')
    'tooltip.alone_mechanical.pipe.none' = @('断开：收起支臂，停止该面传输','None: retracts the arm and disables this face')
    'tooltip.alone_mechanical.pipe.configure' = @('Shift + 扳手右键支臂：切换端面模式','Sneak + wrench-click an arm: cycle its mode')
    'tooltip.alone_mechanical.pipe.inspect' = @('扳手右键支臂：查看当前模式','Wrench-click an arm: inspect its mode')
    'tier.alone_mechanical.basic' = @('赤铜','Red Copper')
    'tier.alone_mechanical.improved' = @('殷钢','Invar')
    'tier.alone_mechanical.advanced' = @('共振','Resonant')
    'tier.alone_mechanical.elite' = @('超导','Superconducting')
    'tier.alone_mechanical.ultimate' = @('量子','Quantum')
    'machine.alone_mechanical.turbine_generator' = @('涡轮发电机','Turbine Generator')
    'gas.alone_mechanical.steam' = @('蒸汽','Steam')
    'message.alone_mechanical.turbine_requires_rotor' = @('涡轮发电机至少需要一个涡轮转子','A turbine generator needs at least one turbine rotor')
    'message.alone_mechanical.turbine_requires_steam' = @('涡轮发电机必须有一列蒸汽输入储罐和输入1端口','A turbine generator needs one steam tank column on Input 1')
    'message.alone_mechanical.turbine_axis_mismatch' = @('涡轮转子必须和能量矩阵处于同一条X轴或Z轴线上','Rotors and the energy matrix must share an X or Z axis')
    'message.alone_mechanical.turbine_requires_horizontal' = @('涡轮发电机必须是横向结构','A turbine generator must be a horizontal structure')
    'tooltip.alone_mechanical.turbine' = @('每个转子提供%s FE/t并消耗%s mB蒸汽/t','Each rotor provides %s FE/t and consumes %s mB steam/t')
    'command.alone_mechanical.player_only' = @('该命令只能由玩家执行','This command can only be run by a player')
    'command.alone_mechanical.unknown_structure' = @('未知结构：%s','Unknown structure: %s')
    'command.alone_mechanical.built' = @('已快速搭建并成型：%s','Quick-built and formed: %s')
    'command.alone_mechanical.build_failed' = @('快速搭建失败（%s），请检查结构模板','Quick build failed (%s); check the structure template')
}
. (Join-Path $PSScriptRoot 'generate_tutorial_resources.ps1')
foreach ($entry in (Tutorial-Translations).GetEnumerator()) { $text[$entry.Key]=$entry.Value }
. (Join-Path $PSScriptRoot 'generate_item_descriptions.ps1')
foreach ($entry in (Item-DescriptionTranslations).GetEnumerator()) { $text[$entry.Key]=$entry.Value }
. (Join-Path $PSScriptRoot 'generate_separator_resources.ps1')
foreach ($entry in (Separator-Translations).GetEnumerator()) { $text[$entry.Key]=$entry.Value }
. (Join-Path $PSScriptRoot 'generate_generator_resources.ps1')
Generator-Resources
foreach ($entry in (Generator-Translations).GetEnumerator()) { $text[$entry.Key]=$entry.Value }
. (Join-Path $PSScriptRoot 'generate_pump_resources.ps1')
foreach ($entry in (Pump-Translations).GetEnumerator()) { $text[$entry.Key]=$entry.Value }
. (Join-Path $PSScriptRoot 'generate_machine_component_resources.ps1')
foreach ($entry in (Machine-ComponentTranslations).GetEnumerator()) { $text[$entry.Key]=$entry.Value }
. (Join-Path $PSScriptRoot 'generate_energy_matrix_resources.ps1')
foreach ($entry in (Energy-MatrixTranslations).GetEnumerator()) { $text[$entry.Key]=$entry.Value }
. (Join-Path $PSScriptRoot 'generate_engineering_resources.ps1')
Engineering-Resources
foreach ($entry in (Engineering-Translations).GetEnumerator()) { $text[$entry.Key]=$entry.Value }
. (Join-Path $PSScriptRoot 'generate_field_resources.ps1')
foreach ($entry in (Field-Translations).GetEnumerator()) { $text[$entry.Key]=$entry.Value }
. (Join-Path $PSScriptRoot 'generate_boiler_resources.ps1')
. (Join-Path $PSScriptRoot 'generate_nuclear_resources.ps1')
Boiler-Resources
foreach ($entry in (Boiler-Translations).GetEnumerator()) { $text[$entry.Key]=$entry.Value }
foreach ($entry in (Nuclear-Translations).GetEnumerator()) { $text[$entry.Key]=$entry.Value }
. (Join-Path $PSScriptRoot 'generate_dimension_conversion_resources.ps1')
Dimension-ConversionResources
foreach($entry in (Dimension-ConversionTranslations).GetEnumerator()){$text[$entry.Key]=$entry.Value}
. (Join-Path $PSScriptRoot 'generate_fluid_tool_resources.ps1')
Fluid-ToolResources
foreach($entry in (Fluid-ToolTranslations).GetEnumerator()){$text[$entry.Key]=$entry.Value}
foreach ($entry in $text.GetEnumerator()) { $zh[$entry.Key] = $entry.Value[0]; $en[$entry.Key] = $entry.Value[1] }
Write-Json "assets/$mod/lang/zh_cn.json" $zh; Write-Json "assets/$mod/lang/en_us.json" $en
$shellIds = @('machine_casing','reinforced_casing','energy_coil','efficiency_coil','energy_matrix','processing_matrix','structural_glass','item_port','fluid_port','gas_port','universal_port','machine_controller','fluid_tank','gas_tank','storage_tank','item_vault')
Write-Json "data/$mod/tags/blocks/machine_casings.json" @{ replace = $false; values = @($shellIds | ForEach-Object { "${mod}:$_" }) }
Write-Json 'data/minecraft/tags/blocks/mineable/pickaxe.json' @{ replace = $false; values = @($blocks.Keys | ForEach-Object { "${mod}:$_" }) }
Write-Json 'data/minecraft/tags/blocks/needs_iron_tool.json' @{ replace = $false; values = @($blocks.Keys | Where-Object { $_ -ne 'structural_glass' } | ForEach-Object { "${mod}:$_" }) }
# Raw-metal and carbon crushing feed the first alloy recipe; the guide is a utility recipe.
Write-Json "data/$mod/recipes/tutorial_book.json" @{type='minecraft:crafting_shapeless';ingredients=@(@{item='minecraft:book'},@{item='minecraft:iron_nugget'});result=@{item="${mod}:tutorial_book";count=1}}
foreach ($metal in @('iron','copper','gold')) {
    Write-Json "data/forge/tags/items/dusts/$metal.json" @{ replace = $false; values = @("${mod}:${metal}_dust") }
    Write-Json "data/$mod/recipes/crush_raw_$metal.json" @{
        type = "${mod}:machine"; machine = "${mod}:crusher"
        item_inputs = @(@{ ingredient = @(@{ item = "minecraft:raw_$metal" }, @{ tag = "forge:raw_materials/$metal" }); count = 1 })
        item_outputs = @(@{ item = "${mod}:${metal}_dust"; count = 2 })
        ticks = 100; energy_per_tick = 20
    }
}
Write-Json "data/$mod/recipes/crush_carbon.json" @{type="${mod}:machine";machine="${mod}:crusher";item_inputs=@(@{ingredient=@(@{item='minecraft:coal'},@{item='minecraft:charcoal'});count=1});item_outputs=@(@{item="${mod}:carbon_dust";count=1});ticks=100;energy_per_tick=20}
Write-Json "data/$mod/recipes/alloy_steel.json" @{type="${mod}:machine";machine="${mod}:alloy_smelter";item_inputs=@(@{ingredient=@{tag='forge:dusts/iron'};count=2},@{ingredient=@{tag='forge:dusts/carbon'};count=1});item_outputs=@(@{item="${mod}:steel_ingot";count=2});ticks=200;energy_per_tick=40}
Write-Json "data/$mod/recipes/steel_dust_mixing.json" @{type='minecraft:crafting_shapeless';ingredients=@(@{item="${mod}:iron_dust"},@{item="${mod}:coal_dust"});result=@{item="${mod}:steel_dust";count=1}}
Write-Json "data/$mod/recipes/fluid_pipe_1.json" @{type='minecraft:crafting_shaped';pattern=@(' R ','SBS',' R ');key=@{R=@{item='minecraft:redstone'};S=@{item="${mod}:steel_ingot"};B=@{item='minecraft:bucket'}};result=@{item="${mod}:fluid_pipe_1";count=12}}
Write-Json "data/$mod/recipes/gas_pipe_1.json" @{type='minecraft:crafting_shaped';pattern=@(' R ','SBS',' R ');key=@{R=@{item='minecraft:redstone'};S=@{item="${mod}:steel_ingot"};B=@{item='minecraft:glass'}};result=@{item="${mod}:gas_pipe_1";count=12}}
Write-Json "data/$mod/recipes/water_electrolysis.json" @{type="${mod}:machine";machine="${mod}:separator_electrolysis";fluid_inputs=@(@{fluid='minecraft:water';amount=1000});gas_outputs=@(@{gas="${mod}:hydrogen";amount=1000},@{gas="${mod}:oxygen";amount=500});ticks=200;energy_per_tick=60}
Write-Json 'data/forge/tags/items/dusts/carbon.json' @{replace=$false;values=@("${mod}:carbon_dust")}
Write-Json 'data/forge/tags/items/ingots/steel.json' @{replace=$false;values=@("${mod}:steel_ingot")}
Write-Json 'data/forge/tags/items/ingots.json' @{replace=$false;values=@('#forge:ingots/steel')}
Write-Json 'data/forge/tags/items/dusts/steel.json' @{replace=$false;values=@("${mod}:steel_dust")}
Write-Json 'data/forge/tags/items/dusts.json' @{replace=$false;values=@('#forge:dusts/iron','#forge:dusts/copper','#forge:dusts/gold','#forge:dusts/carbon','#forge:dusts/steel')}
. (Join-Path $PSScriptRoot 'generate_nuclear_resources.ps1')
Nuclear-Resources
. (Join-Path $PSScriptRoot 'generate_turbine_models.ps1')
. (Join-Path $PSScriptRoot 'update_unified_core_resources.ps1')
. (Join-Path $PSScriptRoot 'generate_tower_resources.ps1')
. (Join-Path $PSScriptRoot 'generate_sulfuric_acid_resources.ps1')
. (Join-Path $PSScriptRoot 'generate_air_separation_resources.ps1')
. (Join-Path $PSScriptRoot 'generate_machine_settings_resources.ps1')
. (Join-Path $PSScriptRoot 'generate_heavy_water_resources.ps1')
. (Join-Path $PSScriptRoot 'generate_creative_tank_resources.ps1')
. (Join-Path $PSScriptRoot 'generate_ore_processing_resources.ps1')
. (Join-Path $PSScriptRoot 'generate_nonmetal_processing_resources.ps1')
. (Join-Path $PSScriptRoot 'generate_fuel_agriculture_resources.ps1')
. (Join-Path $PSScriptRoot 'generate_dimension_fluid_resources.ps1')
. (Join-Path $PSScriptRoot 'generate_reactor_safety_resources.ps1')
. (Join-Path $PSScriptRoot 'generate_uranium_worldgen.ps1')
Write-Output "Generated $($blocks.Count) blocks, $($items.Count) items, loot, tags, bilingual language resources, tutorial book, crushing recipes, steel alloy and nuclear material resources."

Emergency-BatteryRecipe
. (Join-Path $PSScriptRoot 'remove_machine_block_recipes.ps1')
. (Join-Path $PSScriptRoot 'generate_manufacturing_resources.ps1')
