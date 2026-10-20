$ErrorActionPreference='Stop'
$taskRoot=Split-Path $PSScriptRoot -Parent
$taskResources=Join-Path $taskRoot 'src/main/resources'
function Read-Resource($path){Get-Content -LiteralPath (Join-Path $taskResources $path) -Raw | ConvertFrom-Json -AsHashtable}
function Require($condition,$message){if(!$condition){throw $message}}
$alloy=Read-Resource 'data/alone_mechanical/recipes/alloy_steel.json'
Require ($alloy.type -eq 'alone_mechanical:machine' -and $alloy.machine -eq 'alone_mechanical:alloy_smelter') 'Steel must target the dedicated alloy recipe type.'
Require ($alloy.item_inputs.Count -eq 2 -and $alloy.item_inputs[0].ingredient.tag -eq 'forge:dusts/iron' -and $alloy.item_inputs[0].count -eq 2 -and $alloy.item_inputs[1].ingredient.tag -eq 'forge:dusts/carbon' -and $alloy.item_inputs[1].count -eq 1) 'Steel must consume 2 iron dust on Input 1 and 1 carbon dust on Input 2.'
Require ($alloy.item_outputs.Count -eq 1 -and $alloy.item_outputs[0].item -eq 'alone_mechanical:steel_ingot' -and $alloy.item_outputs[0].count -eq 2 -and $alloy.ticks -eq 200 -and $alloy.energy_per_tick -eq 40) 'Steel output, duration or energy does not match the design.'
$carbon=Read-Resource 'data/alone_mechanical/recipes/crush_carbon.json'
Require ($carbon.machine -eq 'alone_mechanical:crusher' -and $carbon.item_inputs[0].count -eq 1 -and $carbon.item_inputs[0].ingredient.Count -eq 2 -and $carbon.item_inputs[0].ingredient[0].item -eq 'minecraft:coal' -and $carbon.item_inputs[0].ingredient[1].item -eq 'minecraft:charcoal' -and $carbon.item_outputs[0].item -eq 'alone_mechanical:carbon_dust' -and $carbon.item_outputs[0].count -eq 1) 'Coal and charcoal must each crush into one carbon dust.'
foreach($tag in @(@('dusts/carbon','carbon_dust'),@('ingots/steel','steel_ingot'))){
    $value=Read-Resource "data/forge/tags/items/$($tag[0]).json"
    Require ($value.values -contains "alone_mechanical:$($tag[1])") "Missing Forge material tag: $($tag[0])"
}
foreach($language in @('zh_cn','en_us')){
    $lang=Read-Resource "assets/alone_mechanical/lang/$language.json"
    foreach($key in @('block.alone_mechanical.alloy_core','item.alone_mechanical.carbon_dust','item.alone_mechanical.steel_ingot','machine.alone_mechanical.alloy_smelter','message.alone_mechanical.requires_alloy_stores','jei.alone_mechanical.alloy_smelter')){
        Require ($lang.ContainsKey($key) -and $lang[$key].Length -gt 0) "Missing translation: $language/$key"
    }
    foreach($part in @('alloy_core','carbon_dust','steel_ingot')){foreach($section in @('function','info')){
        Require ($lang.ContainsKey("tooltip.alone_mechanical.item.$part.$section")) "Missing Shift description: $language/$part/$section"
    }}
    Require ($lang['tutorial.alone_mechanical.machines.body'].Contains('200') -and $lang['tutorial.alone_mechanical.machines.body'].Contains('40 FE/t')) "Guide is missing the steel cycle: $language"
}
$model=Read-Resource 'assets/alone_mechanical/models/item/alloy_core.json'
Require ($model.parent -eq 'alone_mechanical:block/alloy_core') 'Alloy core item must use the actual core block model.'
$loot=Read-Resource 'data/alone_mechanical/loot_tables/blocks/alloy_core.json'
Require ($loot.pools[0].entries[0].name -eq 'alone_mechanical:alloy_core') 'Alloy core must drop itself.'
foreach($file in (Get-ChildItem -LiteralPath $taskResources -Filter '*.json' -Recurse)){
    try{Get-Content -LiteralPath $file.FullName -Raw | ConvertFrom-Json -AsHashtable | Out-Null}catch{throw "Invalid JSON: $($file.FullName): $_"}
}
Write-Output 'ALLOY RESOURCES PASSED: 2 iron dust + 1 carbon dust -> 2 steel; coal/charcoal supply, Forge tags, bilingual guide/Shift descriptions, core model/drop and resource JSON.'
