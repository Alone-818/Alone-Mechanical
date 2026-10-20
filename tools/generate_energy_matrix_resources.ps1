function Energy-MatrixTranslations {
    [ordered]@{
        'matrix.alone_mechanical.title'=@('储能矩阵','Energy storage matrix')
        'matrix.alone_mechanical.mode_charge'=@('仅充电','Charge only')
        'matrix.alone_mechanical.mode_discharge'=@('仅放电','Discharge only')
        'matrix.alone_mechanical.mode_balance'=@('动态平衡','Dynamic balance')
        'matrix.alone_mechanical.cells'=@('能量矩阵：%s 个','Energy cells: %s')
        'matrix.alone_mechanical.matrix_flow'=@('本机  充电 %s  /  放电 %s FE/t','Matrix  In %s / Out %s FE/t')
        'matrix.alone_mechanical.disconnected'=@('当前电网：未连接供电桩','Grid: no connected pylon')
        'matrix.alone_mechanical.grid_flow'=@('当前电网 · 总输入 / 总输出','Connected grid · total flow')
        'matrix.alone_mechanical.input'=@('输入 %s FE/t','Input %s FE/t')
        'matrix.alone_mechanical.output'=@('输出 %s FE/t','Output %s FE/t')
        'matrix.alone_mechanical.net'=@('电网净流量  %s FE/t','Grid net flow  %s FE/t')
        'matrix.alone_mechanical.rate'=@('整机充 / 放上限：各 %s FE/t','Shared in / out limit: %s FE/t each')
        'matrix.alone_mechanical.sampling'=@('流量为上一游戏刻实测值 · 悬停电网查看明细','Previous tick readings · hover grid for details')
        'matrix.alone_mechanical.help_charge'=@('仅接受充电；从电网核心现有电量充入矩阵，不向外放电。直接 FE 输入同样遵守此模式。','Accepts energy only. Charges from the grid core buffer and never discharges. Applies to direct FE access too.')
        'matrix.alone_mechanical.help_discharge'=@('仅允许放电；电网缺电时按需供电，不接受电网或直接 FE 充电。','Discharges on demand when the grid needs energy. Rejects grid and direct FE charging.')
        'matrix.alone_mechanical.help_balance'=@('电网有当刻发电余量时充电，核心不足时按需补充。无负载时不反复充放；直接 FE 可双向传输。','Stores the current tick generation surplus and supplies demand when the core is short. No idle cycling. Direct FE access works in both directions.')
        'matrix.alone_mechanical.external'=@('外部：输入 %s / 用电 %s FE/t','External: generation %s / demand %s FE/t')
        'matrix.alone_mechanical.storage'=@('全部储能：放入电网 %s / 从电网充入 %s FE/t','All storage: to grid %s / from grid %s FE/t')
        'matrix.alone_mechanical.core_buffer'=@('电网核心缓存：%s FE','Grid core buffer: %s FE')
        'message.alone_mechanical.energy_matrix_formed'=@('储能矩阵已成型：%s 个能量矩阵，容量 %s FE','Energy storage matrix formed: %s cells, %s FE')
        'message.alone_mechanical.invalid_energy_matrix_parts'=@('储能矩阵只接受机器外壳、强化外壳、结构玻璃和能量矩阵，边界完整且至少包含一个能量矩阵','Storage matrices require a complete shell of casings, reinforced casings, structural glass and at least one energy matrix')
        'message.alone_mechanical.partial_energy_matrix_overlap'=@('不能只覆盖旧储能矩阵的一部分；请选中完整旧范围','A selection must include the entire existing storage matrix')
        'tutorial.alone_mechanical.energy_storage.title'=@('储能矩阵','Energy storage')
        'tutorial.alone_mechanical.energy_storage.body'=@(
            "储能矩阵是独立的多方块电池，不需要机器核心或控制器。用机器外壳、强化外壳或结构玻璃搭建完整立方体边界，内部放能量矩阵，也可以留空；至少一个能量矩阵。不能混入线圈、处理矩阵、仓储或端口。`n`n工程锤右键选取两个对角点成型，Shift右键取消选点。每边3至%s格；每个能量矩阵提供%s FE，整机每刻充电和放电各共享%s FE上限。右键任意成员方块打开同一GUI。`n`n仅充电：从电网核心已有电量充电，不放电。仅放电：不接受充电，电网缺电时供电。动态平衡：存入当刻发电减用电后的余量，核心不足时按需补电，无负载不空转。三种模式也限制直接FE接口。`n`n让连接电网核心的供电桩覆盖结构几何中心，即可接入该电网；也可从任意实体成员直接输入或抽取FE。GUI显示本矩阵充放电、电网总输入/输出及净流量，单位FE/t，取上一游戏刻的实际传输。悬停电网区域可区分外部发电/用电与全部储能的充放电。`n`n拆坏结构会立即停用，原有电量留待原范围修复后重新锤定；拆光能量矩阵会清除存储。区块未加载时暂停工作。重新成型或扩建不复制电量，缩小容量时超出部分丢失。",
            "An independent multiblock battery needs no machine core or controller. Build a complete cuboid boundary from machine casings, reinforced casings or structural glass, with at least one energy matrix. Interior air is allowed. Coils, processing matrices, storage and ports are rejected.`n`nSelect opposite corners with the engineering hammer; sneak-click cancels. Each dimension is 3 to %s blocks. Each cell provides %s FE; all members share %s FE/t input and output limits independently. Right-click any member for the same GUI.`n`nCharge only stores the core buffer and cannot discharge. Discharge only rejects charging and supplies demand. Dynamic balance stores current-tick generation surplus and supplies core shortages on demand without idle cycling. Modes also constrain direct FE capabilities.`n`nCover the geometric center with a pylon linked to a grid core, or use direct FE on any solid member. The GUI shows this matrix, total grid input/output and net flow in FE/t, measured during the previous completed tick. Hover the grid for external generation/demand and all storage transfers.`n`nDamage immediately disables the structure while retaining its single stored battery for repair and re-forming. Removing every energy cell clears it. Unloaded chunks suspend operation. Re-forming or expanding transfers energy once; capacity reduction discards excess.")
        'jei.alone_mechanical.energy_storage'=@('能量矩阵也能组成独立储能多方块。只用能量矩阵与普通/强化外壳、结构玻璃构建完整边界，工程锤选两角成型，不放核心或控制器。右键GUI切换仅充电、仅放电、动态平衡并查看电网输入输出。供电桩需覆盖结构中心，也支持成员方块直接FE。详见教程书“储能矩阵”。','Energy matrices also form standalone multiblock batteries with casings, reinforced casings and structural glass. Use a complete boundary and select opposite corners with the engineering hammer; no core or controller. Its GUI offers charge-only, discharge-only and balance modes, plus grid flow readings. Cover the structure center with a pylon or use direct FE. See the Energy storage tutorial chapter.')
    }
}
