$ErrorActionPreference='Stop'
$taskRoot=Split-Path $PSScriptRoot -Parent
$taskResources=Join-Path $taskRoot 'src/main/resources'
function Read-SeparatorResource($path){Get-Content -LiteralPath (Join-Path $taskResources $path) -Raw | ConvertFrom-Json -AsHashtable}
function Assert-Separator($condition,$message){if(!$condition){throw $message}}
$recipe=Read-SeparatorResource 'data/alone_mechanical/recipes/water_electrolysis.json'
Assert-Separator ($recipe.type -eq 'alone_mechanical:machine' -and $recipe.machine -eq 'alone_mechanical:separator_electrolysis') 'Water recipe must use the dedicated separator profile.'
Assert-Separator ($recipe.fluid_inputs.Count -eq 1 -and $recipe.fluid_inputs[0].fluid -eq 'minecraft:water' -and $recipe.fluid_inputs[0].amount -eq 1000) 'Water input must be 1000 mB on Input 1.'
Assert-Separator ($recipe.gas_outputs.Count -eq 2 -and $recipe.gas_outputs[0].gas -eq 'alone_mechanical:hydrogen' -and $recipe.gas_outputs[0].amount -eq 1000 -and $recipe.gas_outputs[1].gas -eq 'alone_mechanical:oxygen' -and $recipe.gas_outputs[1].amount -eq 500 -and $recipe.ticks -eq 200 -and $recipe.energy_per_tick -eq 60) 'Both water products, ratio, duration and cost must match.'
$source=Get-Content -LiteralPath (Join-Path $taskRoot 'src/main/java/Alone818/com/alone_mechanical/machine/MachineDefinitions.java') -Raw
$profiles=[regex]::Matches($source,'separator\("([a-z_]+)",PartKind\.([A-Z_]+),PartKind\.([A-Z_]+),PartKind\.([A-Z_]+)\)')
Assert-Separator ($profiles.Count -eq 18) 'Exactly eighteen storage profiles must be declared.'
Assert-Separator (@($profiles | ForEach-Object {$_.Groups[1].Value} | Sort-Object -Unique).Count -eq 18) 'Profile IDs must be unique.'
foreach($language in @('zh_cn','en_us')){
    $lang=Read-SeparatorResource "assets/alone_mechanical/lang/$language.json"
    foreach($profile in $profiles){Assert-Separator ($lang.ContainsKey("machine.alone_mechanical.separator_$($profile.Groups[1].Value)")) "Missing $language profile name: $($profile.Groups[1].Value)"}
    foreach($key in @('block.alone_mechanical.separation_core','recipe.alone_mechanical.water_electrolysis','gas.alone_mechanical.hydrogen','gas.alone_mechanical.oxygen','message.alone_mechanical.requires_separator_stores','message.alone_mechanical.separator_formed','jei.alone_mechanical.separator','tutorial.alone_mechanical.separation.title','tutorial.alone_mechanical.separation.body','tooltip.alone_mechanical.item.separation_core.function','tooltip.alone_mechanical.item.separation_core.info')){
        Assert-Separator ($lang.ContainsKey($key) -and $lang[$key].Length -gt 0) "Missing $language translation: $key"
    }
}
$model=Read-SeparatorResource 'assets/alone_mechanical/models/item/separation_core.json'
Assert-Separator ($model.parent -eq 'alone_mechanical:block/separation_core') 'Separation core must use its block model.'
$loot=Read-SeparatorResource 'data/alone_mechanical/loot_tables/blocks/separation_core.json'
Assert-Separator ($loot.pools[0].entries[0].name -eq 'alone_mechanical:separation_core') 'Separation core must drop itself.'
Write-Output 'SEPARATOR RESOURCES PASSED: eighteen unique named modes, independent water electrolysis at 2:1, bilingual gas/recipe names, guide, Shift description, core model and loot.'
