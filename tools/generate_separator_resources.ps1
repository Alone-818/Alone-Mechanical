# One core, eighteen physical storage profiles. These names do not derive reverse reactor recipes.
function Separator-Translations {
    $names=[ordered]@{
        solid_separation=@('物品分解','Item Decomposition')
        solid_liquid_separation=@('固液浸分','Solid-Liquid Separation')
        solid_gas_separation=@('固气解离','Solid-Gas Dissociation')
        melt_separation=@('熔相分离','Melt Separation')
        pyrolytic_separation=@('热解分离','Pyrolytic Separation')
        gasifying_separation=@('气化分解','Gasifying Decomposition')
        liquid_precipitation=@('液相析固','Liquid Precipitation')
        slurry_separation=@('固液分离','Slurry Separation')
        liquid_decomposition=@('液相分解','Liquid Decomposition')
        liquid_fractionation=@('流体分解','Fluid Decomposition')
        degassing=@('脱气分离','Degassing')
        electrolysis=@('电解分离','Electrolytic Separation')
        gas_deposition=@('气相沉积','Gas Deposition')
        condensing_separation=@('冷凝析固','Condensing Separation')
        gas_solid_separation=@('气固分离','Gas-Solid Separation')
        cryogenic_separation=@('深冷分离','Cryogenic Separation')
        gas_liquid_separation=@('气液分离','Gas-Liquid Separation')
        gas_fractionation=@('气体分解','Gas Decomposition')
    }
    $t=[ordered]@{
        'message.alone_mechanical.requires_separator_stores'=@('三相分离器需要恰好三列独立仓储，端口分别设置为输入1、输出1、输出2；不能使用输入2','A separator needs exactly three independent columns on Input 1, Output 1 and Output 2; Input 2 must be absent')
        'message.alone_mechanical.unsupported_separator_profile'=@('三相分离器仓储介质组合无效','Unsupported separator storage profile')
        'message.alone_mechanical.separator_formed'=@('已成型：%s；输入1：%s；输出1：%s；输出2：%s','Formed: %s; Input 1: %s; Output 1: %s; Output 2: %s')
        'jei.alone_mechanical.separator'=@('三相分离器：中心放分离核心，完整外壳、一个控制器和至少一个能量矩阵。恰好三列独立仓储，端口紧贴列顶部或底部，设为输入1、输出1、输出2。仓储组合决定18种类型，使用独立分解配方。水电解：1000 mB水→1000 mB氢气+500 mB氧气，200 tick、60 FE/t。任一输出堵塞则暂停，支持线圈和处理矩阵。','Three-Phase Separator: central separation core, complete casing, one controller and at least one energy matrix. Exactly three independent storage columns with ports above/below, on Input 1, Output 1 and Output 2. Storage selects one of eighteen profiles with independent decomposition recipes. Water electrolysis: 1000 mB water → 1000 mB hydrogen + 500 mB oxygen; 200 ticks at 60 FE/t. Both outputs must fit. Supports coils and processing matrices.')
        'recipe.alone_mechanical.water_electrolysis'=@('水电解','Water Electrolysis')
        'gas.alone_mechanical.hydrogen'=@('氢气','Hydrogen')
        'gas.alone_mechanical.oxygen'=@('氧气','Oxygen')
        'tutorial.alone_mechanical.separation.title'=@('三相分离器','Three-Phase Separator')
        'tutorial.alone_mechanical.separation.body'=@(
            "分离核心负责一进两出的独立分解加工。中心放分离核心，完整外壳、一个控制器、至少一个能量矩阵。工程锤右键两个相对角点成型，潜行右键取消。`n`n恰好三列独立仓储：输入1、输出1、输出2。每列使用物品仓或储罐，端口紧贴列顶部或底部。输入2不参与构建。GUI左侧为输入，右侧两列为输出，两个输出都不能放入物品。`n`n仓储介质决定机器类型：物品分选、固液浸分、热解分离、液液分离、脱气分离、电解分离、气体分离等18种组合。异类输出可以交换通道；同类输出按配方次序进入输出1和输出2。空储罐会随匹配配方准备输出介质。`n`n水电解：输入1为水储罐，输出1、输出2为两个独立气体罐。1000 mB水 → 1000 mB氢气＋500 mB氧气，基础200 tick、60 FE/t。若用通用储罐，先向输入1加入水，空输出罐会按水电解配方切换为气体。`n`n任一输出空间不足或介质不兼容时暂停，完成前再次检查两路输出，避免原料丢失或只产出一路。支持线圈、电池及处理矩阵，每个处理矩阵增加1个并行位和15%%耗能倍率，实际批次同时受输入与两路输出约束。新分解配方由数据包单独定义。",
            "The separation core performs independent one-input, two-output recipes. Use a central separation core, complete casing, one controller and at least one energy matrix. Hammer-right-click opposite corners to form; sneak-right-click cancels.`n`nExactly three independent storage columns: Input 1, Output 1 and Output 2. Use item vaults or tanks with ports directly above or below. Input 2 must be absent. The GUI shows one input on the left and two protected outputs on the right.`n`nThe storage media select eighteen profiles, including sorting, pyrolytic separation, liquid fractionation, degassing, electrolysis and gas fractionation. Different output media may swap channels; matching media follow recipe order. Empty tanks prepare the output phase for a matching recipe.`n`nWater electrolysis: water on Input 1, separate gas stores on Output 1 and Output 2. 1000 mB water → 1000 mB hydrogen + 500 mB oxygen; 200 ticks at 60 FE/t. With universal tanks, insert water first; empty output tanks adopt the gas phase automatically.`n`nA full or incompatible output pauses processing. Both outputs are checked before completion. Coils and batteries are supported; each processing matrix adds one parallel slot and 15%% energy multiplier; input and both output capacities limit the batch. Data packs define additional decomposition recipes independently.")
    }
    foreach($entry in $names.GetEnumerator()){$t["machine.alone_mechanical.separator_$($entry.Key)"]=@("三相分离器 · $($entry.Value[0])","Separator · $($entry.Value[1])")}
    return $t
}
