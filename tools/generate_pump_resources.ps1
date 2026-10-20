function Pump-Translations {
    [ordered]@{
        'tooltip.alone_mechanical.item.electric_pump.function'=@('单方块机器，从正下方抽取液体源。','A single-block machine that pumps liquid sources directly below.')
        'tooltip.alone_mechanical.item.electric_pump.info'=@('右键GUI安装效率线圈、能量线圈和能量矩阵；内部16000 mB缓存，上方/侧面出液，电网或FE供电。','Install efficiency coils, energy coils and energy matrices in its GUI. 16000 mB buffer; top/side output; pylon or direct FE supply.')
        'pump.alone_mechanical.status_0'=@('等待液体源','No liquid source')
        'pump.alone_mechanical.status_1'=@('抽取中','Pumping')
        'pump.alone_mechanical.status_2'=@('电量不足','Low energy')
        'pump.alone_mechanical.status_3'=@('缓存已满','Buffer full')
        'pump.alone_mechanical.status_4'=@('液体不兼容','Fluid mismatch')
        'pump.alone_mechanical.cost'=@('耗能 %s FE/t','Cost %s FE/t')
        'pump.alone_mechanical.speed'=@('速度 %s%%','Speed %s%%')
        'pump.alone_mechanical.upgrades'=@('升级','Upgrades')
        'pump.alone_mechanical.slot_0'=@('效','S')
        'pump.alone_mechanical.slot_1'=@('能','E')
        'pump.alone_mechanical.slot_2'=@('容','B')
        'pump.alone_mechanical.upgrade_0'=@('效率线圈槽 · 最多64个','Efficiency coils · up to 64')
        'pump.alone_mechanical.upgrade_1'=@('能量线圈槽 · 最多64个','Energy coils · up to 64')
        'pump.alone_mechanical.upgrade_2'=@('能量矩阵槽 · 最多64个','Energy matrices · up to 64')
        'pump.alone_mechanical.upgrade_help_0'=@('每个加速15%%、耗能倍率增加10%%','Each adds 15%% speed and 10%% energy multiplier')
        'pump.alone_mechanical.upgrade_help_1'=@('每个减少20%%基础耗能，最低1 FE/t','Each subtracts 20%% of base cost; minimum 1 FE/t')
        'pump.alone_mechanical.upgrade_help_2'=@('每个增加一份基础电池容量','Each adds one base battery capacity')
        'pump.alone_mechanical.empty'=@('液体缓存为空','Empty fluid buffer')
        'pump.alone_mechanical.outlet'=@('上方与侧面出液；管道源端设为拉取','Output on top/sides; set the pipe source face to Pull')
        'pump.alone_mechanical.intake'=@('只抽正下方可汲取的源液体；普通流动液体不抽取','Only drainable source liquids directly below; flowing liquids are skipped')
        'pump.alone_mechanical.battery'=@('电量 %s / %s FE','Energy %s / %s FE')
        'pump.alone_mechanical.supply'=@('供电桩覆盖或直接输入FE','Pylon coverage or direct FE input')
        'pump.alone_mechanical.progress'=@('进度 %s / %s tick','Progress %s / %s ticks')
        'jei.alone_mechanical.electric_pump'=@('电动泵是单方块机器，直接放在液体源上方，无需工程锤。每轮抽取一个可汲取的源液体，水/岩浆为1000 mB，内部缓存16000 mB；基础20 tick、40 FE/t。只有正下方参与抽取，不搜索周围液池。水浸方块按桶取水规则处理并保留原方块。供电桩按泵的位置覆盖，也可直接输入FE。右键GUI安装效率线圈、能量线圈、能量矩阵，分别最多64/64/64个；矩阵扩容电池。升级变动重置当前进度。顶部/侧面流体管道设拉取即可输出，底部为抽取口；满罐、不兼容、无液体或电力不足时停机。','Place this single-block electric pump above a liquid source; no hammer needed. Each cycle collects one drainable source (1000 mB for water/lava) into a 16000 mB buffer, at a base 20 ticks and 40 FE/t. Only the block immediately below is sampled. Waterlogged blocks follow bucket pickup rules. Supply pylon coverage or direct FE. The GUI accepts up to 64 efficiency coils, 8 energy coils and 64 battery matrices. Upgrade changes reset progress. Pull from top/side fluid pipes; the bottom is intake only. Full, incompatible, absent sources or low energy pause operation.')
        'tutorial.alone_mechanical.pumps.title'=@('电动泵','Electric pump')
        'tutorial.alone_mechanical.pumps.body'=@(
            "电动泵是单方块机器，放置即可使用，不需要多方块外壳、核心或工程锤。`n`n抽取：将泵放在液体源正上方，只检查紧贴底面的一个方块。水和岩浆每轮抽取1000 mB；自然流动的液体不会被当作液体源。水浸方块按桶取水规则排水并保留原方块。不会向周围搜索或凭空生成液体。`n`n供电：供电桩覆盖泵的位置即可，也可从任意面输入FE。基础电池使用机器电池配置。基础周期20 tick（1秒），40 FE/t，内部液体缓存16000 mB。`n`n升级：右键打开GUI，把升级方块放入对应槽。效率线圈最多64个，每个速度加15%%、耗能倍率增加10%%；能量线圈最多64个，每个减20%%基础耗能，最低1 FE/t。能量矩阵最多64个，每个增加一份基础电池容量。变更升级会重置当前进度，移除矩阵时电量会裁剪到新容量。处理矩阵用于多方块配方机器，不接受在泵内。`n`n输出：上方或侧面接流体管道，源端设为拉取。底部保留为抽取口，缓存只向外提供液体，不接受外部灌入。液体满罐、不兼容或电量不足时暂停。可在液位、电池和升级槽悬停查看详情。拆除泵会掉落已安装升级方块。",
            "The electric pump is a single-block machine. Place it directly; no casing, core or hammer is required.`n`nIntake: only the block touching its bottom is checked. Each water/lava source supplies 1000 mB per cycle. Ordinary flowing liquid is skipped. Waterlogged blocks use bucket pickup behavior and retain the host block. The pump does not search nearby pools or create liquid.`n`nPower: cover the pump position with a pylon, or supply FE directly on any face. Base battery uses the machine battery config. Base cycle: 20 ticks (1 second), 40 FE/t; internal fluid buffer: 16000 mB.`n`nUpgrades: right-click and place blocks in their matching GUI slots. Up to 64 efficiency coils each add 15%% speed and 10%% energy multiplier. Up to 64 energy coils each subtract 20%% of base cost, minimum 1 FE/t. Up to 64 energy matrices each add one base battery capacity. Changing upgrades resets progress; removing battery matrices clamps energy to the new capacity. Processing matrices are reserved for recipe multiblocks.`n`nOutput: connect top/side fluid pipes and set their source face to Pull. Bottom is intake only. The buffer rejects external filling. Full/incompatible fluid buffers or low energy pause work. Hover the tank, battery and upgrade slots for details. Breaking the pump drops its installed upgrade blocks.")
    }
}
