$ErrorActionPreference='Stop'
$taskResources=Join-Path (Split-Path $PSScriptRoot -Parent) 'src/main/resources'
function Read-PumpResource($path){Get-Content -LiteralPath (Join-Path $taskResources $path) -Raw|ConvertFrom-Json -AsHashtable}
foreach($language in @('zh_cn','en_us')){
    $lang=Read-PumpResource "assets/alone_mechanical/lang/$language.json"
    foreach($key in @('block.alone_mechanical.electric_pump','tooltip.alone_mechanical.item.electric_pump.function','tooltip.alone_mechanical.item.electric_pump.info','tutorial.alone_mechanical.pumps.title','tutorial.alone_mechanical.pumps.body','jei.alone_mechanical.electric_pump','pump.alone_mechanical.outlet','pump.alone_mechanical.upgrade_0','pump.alone_mechanical.upgrade_1','pump.alone_mechanical.upgrade_2')){if(!$lang.ContainsKey($key)){throw "Missing $language pump translation: $key"}}
    for($i=0;$i -lt 5;$i++){if(!$lang.ContainsKey("pump.alone_mechanical.status_$i")){throw "Missing pump state: $language/$i"}}
}
$states=Read-PumpResource 'assets/alone_mechanical/blockstates/electric_pump.json'
if($states.variants.Count -ne 8){throw 'Pump must define all four directions and both running states.'}
foreach($facing in @('north','east','south','west')){foreach($active in @('false','true')){if(!$states.variants.ContainsKey("facing=$facing,lit=$active")){throw "Missing pump state: $facing/$active"}}}
$model=Read-PumpResource 'assets/alone_mechanical/models/block/electric_pump.json'
if($model.parent -ne 'minecraft:block/orientable_with_bottom'||$model.textures.bottom -ne 'alone_mechanical:block/electric_pump_bottom'){throw 'Pump needs directional housing and a bottom intake texture.'}
$loot=Read-PumpResource 'data/alone_mechanical/loot_tables/blocks/electric_pump.json'
if($loot.pools[0].entries[0].name -ne 'alone_mechanical:electric_pump'){throw 'Pump must drop itself.'}
Write-Output 'PUMP RESOURCES PASSED: eight blockstates, directional intake/outlet artwork, own loot, bilingual statuses, upgrade help, guide and JEI information.'
