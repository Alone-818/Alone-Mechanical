# Generator artwork/models and localized information, called by generate_resources.ps1.
function Generator-Resources {
    foreach($kind in @('solar','lunar')) {
        $id="${kind}_generator"; $panel="${kind}_panel"
        Texture 'block' $panel
        $base=@((Element @(1,0,1) @(15,8,15)),(Element @(6,8,6) @(10,16,10)))
        Model $id @{textures=@{all="${mod}:block/$id";particle="${mod}:block/$id"};elements=$base}
        $slab=Element @(-7,-0.5,-6) @(7,0.5,6) '#panel'
        # Centered negative coordinates need explicit UVs; auto UVs would sample neighboring atlas sprites.
        foreach($face in $slab.faces.Values){$face.uv=@(0,0,16,16)}
        Model $panel @{textures=@{panel="${mod}:block/$panel";particle="${mod}:block/$panel"};elements=@($slab)}
        $inventoryPanel=Element @(1,15.5,2) @(15,16.5,14) '#panel'
        foreach($face in $inventoryPanel.faces.Values){$face.uv=@(0,0,16,16)}
        Model "${id}_inventory" @{textures=@{all="${mod}:block/$id";panel="${mod}:block/$panel";particle="${mod}:block/$id"};elements=@($base)+@($inventoryPanel)}
        Write-Json "assets/$mod/models/item/$id.json" @{parent="${mod}:block/${id}_inventory"}
    }
    # Tiered battery recipes remain reserved; generate_resources.ps1 supplies the emergency battery recipe.
    $recipes=[ordered]@{
        solar_generator=@{pattern=@('GGG','LRL','ICI');key=@{G=@{item='minecraft:glass'};L=@{item='minecraft:lapis_lazuli'};R=@{item='minecraft:redstone'};I=@{item='minecraft:iron_ingot'};C=@{item="${mod}:machine_casing"}}}
        lunar_generator=@{pattern=@('AGA','GSG','ARA');key=@{A=@{item='minecraft:amethyst_shard'};G=@{item='minecraft:glowstone_dust'};S=@{item="${mod}:solar_generator"};R=@{item='minecraft:redstone'}}}
        thermal_generator=@{pattern=@('ICI','RFR','ICI');key=@{I=@{item='minecraft:iron_ingot'};C=@{item='minecraft:copper_ingot'};R=@{item='minecraft:redstone'};F=@{item='minecraft:furnace'}}}
    }
    foreach($entry in $recipes.GetEnumerator()) {
        Write-Json "data/$mod/recipes/$($entry.Key).json" @{type='minecraft:crafting_shaped';pattern=$entry.Value.pattern;key=$entry.Value.key;result=@{item="${mod}:$($entry.Key)"}}
    }
}
function Generator-Translations {
    $text=[ordered]@{
        'gui.alone_mechanical.turbine_steam_store'=@('蒸汽输入','Steam input')
        'gui.alone_mechanical.turbine_ratings'=@('涡轮参数','Turbine ratings')
        'gui.alone_mechanical.turbine_rotor_count'=@('转子数量','Rotors')
        'gui.alone_mechanical.turbine_rated_output'=@('额定发电','Rated output')
        'gui.alone_mechanical.turbine_rated_steam'=@('额定耗汽','Steam demand')
        'gui.alone_mechanical.turbine_actual'=@('实际发电 %s FE/t    耗汽 %s mB/t','Actual: %s FE/t    Steam: %s mB/t')
        'gui.alone_mechanical.turbine_energy_buffer'=@('电力缓存','Energy buffer')
        'gui.alone_mechanical.turbine_stored'=@('已存电量','Stored')
        'gui.alone_mechanical.turbine_capacity'=@('电力容量','Capacity')
        'gui.alone_mechanical.turbine_matrices'=@('能量矩阵','Matrices')
        'gui.alone_mechanical.turbine_export'=@('通过能量矩阵输出电力','Output via energy matrices')
        'gui.alone_mechanical.turbine_waiting_steam'=@('等待蒸汽','Waiting for steam')
        'gui.alone_mechanical.turbine_rotor_detail'=@('已安装 %s 个涡轮转子','Installed turbine rotors: %s')
        'gui.alone_mechanical.turbine_rate_detail'=@('整机额定发电 %s FE/t，消耗蒸汽 %s mB/t','Rated output: %s FE/t; steam demand: %s mB/t')
        'gui.alone_mechanical.turbine_export_help'=@('从能量矩阵接出电力；缓存空间不足时暂停，不消耗蒸汽。','Extract FE through an energy matrix. Insufficient buffer space pauses operation without consuming steam.')
        'gui.alone_mechanical.turbine_status_help'=@('蒸汽充足且电力缓存有空间时持续发电；额定参数是运行时每刻的用量。','Generates continuously with sufficient steam and energy space. Ratings show the per-tick amounts during operation.')
        'gui.alone_mechanical.generator.status.0'=@('待机','Idle')
        'gui.alone_mechanical.generator.status.1'=@('正在发电','Generating')
        'gui.alone_mechanical.generator.status.2'=@('电力缓存已满 · 暂停','Buffer full: paused')
        'gui.alone_mechanical.generator.status.3'=@('顶部天空被遮挡 / 无天空','Sky blocked / no skylight')
        'gui.alone_mechanical.generator.status.4'=@('夜间待机','Night: idle')
        'gui.alone_mechanical.generator.status.5'=@('放入煤炭或电池','Insert coal or a battery')
        'gui.alone_mechanical.generator.rate'=@('发电：%s FE/t','Output: %s FE/t')
        'gui.alone_mechanical.generator.buffer'=@('%s / %s FE','%s / %s FE')
        'gui.alone_mechanical.generator.burn'=@('剩余：%s 秒','Fuel: %s s left')
        'gui.alone_mechanical.generator.sky'=@('顶部需露天','Requires open sky above')
    }
    $details=[ordered]@{
        solar_generator=@('白天利用太阳光持续发电。','当前%s FE/t；顶部需露天，面板追踪日月；相邻电网核心或FE设备可接收电力。','Continuously generates FE during daylight.','Currently %s FE/t; requires open sky. Its panel tracks the sun/moon; adjacent grid cores and FE devices accept output.')
        lunar_generator=@('昼夜均可发电的夜光光能发电机。','当前%s FE/t；顶部需露天，白天追踪太阳、夜间追踪月亮，不受月相和雨天影响。','A luminous solar generator that operates by day and night.','Currently %s FE/t; requires open sky. Tracks sun by day and moon by night, regardless of moon phase or rain.')
        thermal_generator=@('燃烧煤炭、木炭、煤炭块或一次性电池，持续产生FE。','煤燃料%s FE/t，采用原版燃烧时长；满电暂停燃烧，支持漏斗/物品管道和六面FE输出。','Burns coal, charcoal, coal blocks or disposable batteries to generate FE continuously.','Coal fuels produce %s FE/t for their vanilla burn duration. Pauses at full charge; supports item automation and FE output on all six sides.')
        basic_battery=@('初级一次性发电电池。','热能机内%s FE/t，持续%s tick；投入燃烧后消耗，不能充电。','Basic disposable generator battery.','Thermal generator: %s FE/t for %s ticks. Consumed as fuel; cannot be recharged.')
        intermediate_battery=@('中级一次性发电电池。','热能机内%s FE/t，持续%s tick；初级功率和时长的2倍。','Intermediate disposable generator battery.','Thermal generator: %s FE/t for %s ticks, twice the basic power and duration.')
        advanced_battery=@('高级一次性发电电池。','热能机内%s FE/t，持续%s tick；初级功率和时长的4倍。','Advanced disposable generator battery.','Thermal generator: %s FE/t for %s ticks, four times the basic power and duration.')
        high_power_battery=@('高级电池的高功率变种。','热能机内%s FE/t，持续%s tick；相对高级电池：发电量×1.5，燃烧时间×0.7。','High-power variant of the advanced battery.','Thermal generator: %s FE/t for %s ticks; advanced power x1.5 and burn duration x0.7.')
        long_life_battery=@('高级电池的长续航变种。','热能机内%s FE/t，持续%s tick；相对高级电池：发电量×1，燃烧时间×1.5。','Long-life variant of the advanced battery.','Thermal generator: %s FE/t for %s ticks; advanced power x1 and burn duration x1.5.')
    }
    foreach($entry in $details.GetEnumerator()) {
        $text["tooltip.alone_mechanical.item.$($entry.Key).function"]=@($entry.Value[0],$entry.Value[2])
        $text["tooltip.alone_mechanical.item.$($entry.Key).info"]=@($entry.Value[1],$entry.Value[3])
    }
    foreach($id in @('basic_battery','intermediate_battery','advanced_battery','high_power_battery','long_life_battery')){
        $text["tooltip.alone_mechanical.item.$id.info"][0]+=' 当前没有默认合成配方。'
        $text["tooltip.alone_mechanical.item.$id.info"][1]+=' No default crafting recipe.'
    }
    return $text
}
