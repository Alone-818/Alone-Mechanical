param([string]$Root = (Join-Path $PSScriptRoot '..\src\main\resources'))
$recipeRoot=Join-Path $Root 'data\alone_mechanical\recipes'
$produced=@{};$consumed=@{};$files=Get-ChildItem $recipeRoot -Filter '*.json'
function Add-Count([hashtable]$table,[string]$key){$current=0;if($table.ContainsKey($key)){$current=$table[$key]};$table[$key]=1+$current}
foreach($file in $files){
    try{$r=Get-Content -Raw $file.FullName|ConvertFrom-Json}catch{Write-Warning "Invalid JSON: $($file.Name)";continue}
    foreach($o in @($r.item_outputs)){if($o -and $o.item){Add-Count $produced $o.item}}
    foreach($o in @($r.fluid_outputs)){if($o -and $o.fluid){Add-Count $produced "fluid:$($o.fluid)"}}
    foreach($o in @($r.gas_outputs)){if($o -and $o.gas){Add-Count $produced "gas:$($o.gas)"}}
    foreach($i in @($r.item_inputs)){if($i -and $i.ingredient.item){Add-Count $consumed $i.ingredient.item}}
    foreach($i in @($r.fluid_inputs)){if($i -and $i.fluid){Add-Count $consumed "fluid:$($i.fluid)"}}
    foreach($i in @($r.gas_inputs)){if($i -and $i.gas){Add-Count $consumed "gas:$($i.gas)"}}
}
"Recipes: $($files.Count)"
"Produced resources: $($produced.Count)"
"Produced resources without a downstream consumer:"
$produced.Keys|Where-Object {-not $consumed.ContainsKey($_)}|Sort-Object
