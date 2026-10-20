# 机器和系统扩展

## 教程第 1–4 章对应内容

- 第 1 章：清理演示逻辑，主类保留加载入口和注册调度。
- 第 2 章：`init/ModBlocks`、`ModItems`、`ModCreativeModeTabs` 使用独立 DeferredRegister；物品包含金属粉末，物品栏图标为铁粉。
- 第 3 章：方块配套 BlockItem、模型、方块状态、中英翻译、掉落和标签。所有现有方块及物品使用原生 16×16 原创像素 PNG，管道和接头使用原生 8×8 PNG；通用端口使用四种模式图标，缓存显示对应储物、流体或气体图案。
- 第 4 章：泛型 `registerBlock` 自动附带物品注册，`registerItem` 用工厂创建不同物品类型，统一资源生成入口。

## 代码入口

| 模块 | 主要入口 |
| --- | --- |
| 电网存档、连通性、单核心约束、覆盖和扣费 | `power/PowerGridData.java` |
| 杆体放置、拆解和点击重定向 | `block/GridNodeBlock.java` |
| GUI 开始/取消连线与服务端临时选点 | `menu/GridMenu.java` / `power/PowerLinkSession.java` |
| 顶部连线渲染 | `client/GridWireRenderer.java` |
| 长方体选区检测、成员归属和任意成员 GUI | `machine/MultiblockStructure.java` |
| 工程锤两点选区与 Shift 取消 | `item/EngineeringHammerItem.java` |
| 核心家族与仓储配置决定机器类型 | `block/MachineCoreBlock.java` / `machine/MachineDefinitions.java` |
| 资源保存、加成、通用加工逻辑 | `blockentity/MachineBlockEntity.java` |
| 四种模式和资源能力代理 | `blockentity/MachinePartBlockEntity.java` |
| 竖向整体及物理缓存存档 | `blockentity/StorageBlockEntity.java` |
| 多段/多列资源组合视图 | `transport/ResourceBanks.java` |
| 缓存分页菜单与界面 | `menu/StorageMenu.java` / `client/StorageScreen.java` |
| 扳手 | `item/WrenchItem.java` |
| 六面管道网络、路由和共享传输预算 | `blockentity/PipeBlockEntity.java` |
| 管道自动支臂模型与端面模式 | `block/PipeBlock.java` / `transport/PipeMode.java` |
| 全物品 Shift 提示 | `client/ItemTooltips.java` / `item/ItemDescriptions.java` / `transport/PipeTooltip.java` |
| 抽入/抽出动态套环 | `client/PipeConnectorRenderer.java` |
| 配方解析、网络序列化和资源提交 | `recipe/MachineRecipe.java` |
| 菜单与屏幕 | `menu` / `client` |
| 实际服务器回归测试 | `src/gametest/java/.../MechanicalGameTests.java` |

## 添加机器类型

在 `ModBlocks` 注册 `MachineCoreBlock` 时传入核心家族 ID，在 `MachineDefinitions.register` 注册机器 ID、核心家族与仓储类型判定。当前 `crusher` 核心解析为粉碎机，`smelting` 核心搭配物品仓解析为 `arc_furnace`，后续更具体的储罐配置可使用相同核心注册新的机器，后注册规则优先。配套新增方块资源与机器名翻译。未知核心或配置返回 `unknown_machine`。使用相同控制器和外壳；中心更换核心后，旧结构先拆解，再按新的类型成型。

把配方 JSON 中 `machine` 设为同一 ID 即可复用现有加工和 GUI。需要不同流程时可以从 `MachineBlockEntity` 和 `MachineMenu` 扩展，并通过相应控制器的 `newBlockEntity` / `createMenu` 注册接入。

`StorageConnections` 统一识别端口顶部/底部相邻列；存储本身无通道，端口模式决定库存归属。每个端口只代理一列，机器按通道汇总并去重；形成机器时仓库不暴露外部输入输出。JEI 的配方列表来自 `RecipeDisplays`，可选插件入口为 `client/jei/MechanicalJeiPlugin`。电弧炉的标准熔炼适配位于 `recipe/ArcFurnaceRecipes`，从当前配方管理器匹配，不缓存已移除配方。

## 后续配方扩展格式

当前正式资源及生成脚本仅包含粗铁、粗铜、粗金的三条粉碎配方，均为 1 个粗矿产出 2 个对应粉末，100 tick、20 FE/t。合成、粉末熔炼及合金配方后续参考通用机械的科技进度再设计。以下示例描述实际粗铁粉碎配方的加载格式。

后续存放路径：`data/<namespace>/recipes/<name>.json`。

```json
{
  "type": "alone_mechanical:machine",
  "machine": "alone_mechanical:crusher",
  "item_inputs": [
    {"ingredient": [{"item": "minecraft:raw_iron"}, {"tag": "forge:raw_materials/iron"}], "count": 1}
  ],
  "item_outputs": [
    {"item": "alone_mechanical:iron_dust", "count": 2}
  ],
  "ticks": 100,
  "energy_per_tick": 20
}
```

每个数组最多两项，数组位置直接对应通道 1 / 2；未用的数组可省略。至少需要一种输入。`ingredient` 支持 Forge 的物品或标签 Ingredient。液体必须是已注册的流体，气体是独立命名空间 ID。配方时长允许 1–1,000,000 tick，基础能耗允许 1–1,000,000 FE/t，每项流体或气体量不得超过储罐容量。

开始和每次推进都验证输入与输出容量，完成时一次性提交所有资源。多条配方同时匹配时按配方 ID 排序选择。修改配方或原料使配方不再匹配，会清除原配方进度。

## 实现边界

- 机器范围来自工程锤两个对角点，包含两个端点；长、宽、高可以不同，默认每边各为 3–16 格。工作条件是已加载且完整的选区，不强制加载区块。
- 中心坐标为 `(min + max) / 2` 向下取整，控制器位于任意外表面位置。机器不会自动初次成型；修复后需工程锤重新检测，存档加载恢复已成型范围。
- 粉碎机最小3×3×3，必须有两个独立物品仓列，输入1和输出1各至少27槽，并有至少一个能量矩阵。同一竖向列只计数一次，不能同时作为输入和输出。
- 单段物品仓27槽，旧9槽NBT加载时固定扩为27槽并保留库存。四个物品通道完全来自实体仓库，机器GUI按每页27槽展示输入1／输出1全部实体仓库，独立翻页并同步总槽数；旧控制器暂存物品在成型时尝试转入仓库，剩余内容仍保存，拆控制器会掉落。流体和气体仍有控制器暂存区。组合视图实时访问物理方块，不保存复制后的整体库存；合并、拆分和跨存档恢复不会复制库存。
- `assembleDetailed` 返回带原因及可选坐标的结果；检查尺寸上限后才遍历，拒绝未加载区块、世界高度越界、无核心、未知类型、缺失部件、非法外壳／内部、重复控制器、归属重叠和跨边界缓存。工程锤统一显示构建失败前缀，捕获运行时构建异常，停止部分构建并记录服务器日志，失败保留首角点。只有 `tryForm` 真正成功才能返回成功。
- `MachinePortBlock.IO_MODE` 为四种输入输出模式提供方块状态；`setMode` 和服务端 `onLoad` 更新状态，资源生成器覆盖所有朝向和模式。缓存扳手对整列同步模式及正面箭头材质。
- 同类储罐和物品仓只竖向连接，预装不同介质的罐按从下往上的确定规则划分，不会让中间空罐同时属于两组。机器检测禁止整列缓存跨选区边界。
- 供电只由供电桩覆盖，信号杆负责转接，电网核心负责储能。
- 每台机器有独立电池，配置基础容量默认 100,000 FE；`ENERGY_MATRIX` 外壳按基础容量线性扩容，容量乘法用 long 并限制到 Forge 的整数上限。电量只存控制器 NBT，矩阵代理同一电池，不创建分散的副本。
- 成型机器每 tick 从覆盖电网充电，默认上限 10,000 FE，配方从本机电池扣电；断网后可使用剩余电量。控制器和矩阵提供只入不出的 Forge Energy 能力，旧矩阵能力引用在解体后禁止操作；重新成型后按实际矩阵数裁剪超容量电量。
- 能量线圈和效率线圈各最多 8 个生效。节能按基础耗能每个减 20% 的加法规则，倍率最低零，最终每个工作 tick 至少扣 1 FE；效率按每个 +30% 速度、+15% 耗能计算，速度最高 340%。
- 处理矩阵为 PROCESSING_MATRIX 外壳类型，最多 5 个生效，并行容量为 `1 << min(数量, 5)`。MachineBlockEntity 在每轮进度为零时根据全部输入资源和输出空间选择实际批次数；整轮固定 activeBatch，并随进度保存。单份线圈修正后的 FE/t 乘实际批次数，速度和单份周期保持原逻辑。MachineRecipe 的批量匹配、输出模拟及完成接口同时放大两个物品通道、流体和气体，完成前再次检查全部资源；ItemHandlerHelper 将大于单栈上限的产物分配到多个真实仓库槽。原版及数据包熔炼适配只聚合同一配方且产物相同的输入，避免混合不同 NBT 产物。GUI 同步有效矩阵数量、并行上限及当前批次数。
- 电网邻近供电桩按区块建立索引，已加载连通分量每 tick 缓存，避免每台机器扫描整世界。
- 电网 GUI 使用原版菜单按钮消息；服务端校验菜单归属与距离，再从当前菜单节点建立临时选点。目标点击仍由 `PowerGridData.toggleLink` 校验连接距离和单核心约束，未加载起点不能连接；临时选择不保存到物品或存档。
- 管道同资源类型六面邻接，任一侧 NONE 阻断连通。NORMAL 接受外部输入并可输出，PULL 主动提取，PUSH 仅输出。已加载网络用 BFS 建立可用路径，去重环路；各可用目标先均分，未用完份额再次分配，余数轮换。每段共享当 tick 预算，模拟以虚拟预算生成 Allocation 计划，执行逐段扣减实际接受量；混级与分支不能放大吞吐。
- 端点按储存列底座或机器控制器加通道去重，避免多个接口重复模拟同一缓存容量，也排除源端整体。抽入面和物品槽分别轮换，目标轮换只在实际接受资源后推进，NBT 保存三个位置。缓存的端面能力包装器实时检查配置，断开后不能继续使用旧引用输入。实际拒收的余量先退回源，拒绝退回则持久暂存并优先重试，拆管掉落暂存物品；没有物品颜色过滤或物品包动画。
- 流体、气体管道每段分别通过 FluidTank／GasTank 提供1000mB真实缓存，容量独立于等级速率。正常输入先按路径预算直送，无法直送的部分进入本段缓存；主动抽入缓存也扣本段速率预算。tick优先输出已存内容，输出、主动抽入和外部抽走共享本段预算。缓存使用BufferFluid／BufferGas及BufferInlets保存并同步，记录注入端避免回流；旧PendingFluid／PendingGas迁入缓存。路径不能穿过存有不兼容介质的管段。
- 全部本模组物品和 BlockItem 的键盘读取统一在 Dist.CLIENT 的 ItemTooltips 中，公共 ItemDescriptions 不加载客户端类。折叠时仅追加一个通用 Shift 提示；展开时追加用途和信息，管道复用 PipeTooltip 的分色参数。教程书和处理矩阵不再自行追加常驻说明。所有非管道物品都要求中英 function/info 两个文本，容量与范围读取实现常量/当前配置。GameTest 遍历整个 ModItems 注册表检查覆盖和泄漏，实际客户端验证事件只追加一次提示且两种语言没有未翻译键/占位符。
- 管道为 6 像素管身、8 像素套环；蓝色内环表示 PULL，橙色外环表示 PUSH，无立体箭头或扩口。额外模型渲染与支臂均采用 BlockModelRotation，北向基模的 UP=X270、DOWN=X90。渲染读取 Modes 同步数据，只在连接面绘制；点击形状覆盖套环，支臂识别优先选已连接方向。
- 管道使用 tools/generate_pipe_textures.ps1 直接绘制原生 8×8 方形接点与纵向色轨，multipart 根据六向连接布尔状态选择三轴整段直管或接点加支臂。恰好两个相对端连接时隐藏接点并使用整段 6×6×16 直管；其他状态显示端点、转角或分支接点。支臂和直管的四个侧面都将纹理 U 映射为横截面、V 映射为管长，uvlock=false 使纹理随几何一起旋转；侧纹理沿纵向各行相同，不出现每格重复的横向图案。套环作为独立标识面保留完整 UV，纹理自带蓝色抽入 / 橙色抽出模式色。`tools/generate_resources.ps1 -PipesOnly` 只重生成管道资源，`tools/preview_pipe_connections.ps1` 检查 960 种连接组合并生成实际模型的俯视预览。真实客户端检查还验证 960 种烘焙模型的面数以及三个直管方向的 UV。其他方块和物品由 tools/generate_mod_textures.ps1 按功能绘制 16×16 材质，资源重生成不恢复占位图。
- 结构玻璃的 StructuralGlassBlock 在六个面分别计算边缘和凹角连接，隐藏相邻内部面，阻光值为 0。ConnectedGlassModel 根据 ModelData 选择 47 种独立 16×16 透明 PNG，使用 cutout、不旋转 UV 的模型和关闭 AO 的玻璃表面；透明中心不覆盖背景。采用[机械动力连接材质](https://github.com/Creators-of-Create/Create/blob/mc1.20.1/dev/src/main/java/com/simibubi/create/foundation/block/connected/CTModel.java)的按面连接思路，不需要安装机械动力。
- tools/verify_textures.ps1 检查全套 PNG 尺寸、透明中心、占位图和模型贴图引用；tools/preview_mod_textures.ps1 生成全部材质和玻璃拼接预览。真实客户端资源验证：`./gradlew.bat --offline -I tools/texture-client-check.gradle runClient`，使用独立 run-textures-check 目录并自动退出；成功日志包含 `TEXTURE CLIENT CHECK PASSED`。验证源位于 gameTest source set，不进入发布 jar。
- 气体接口独立于液体；没有隐式把任意液体当气体，也没有未经声明的跨模组气体兼容。
- GUI 的数值通过 32 位整数拆分同步，避免 1,000,000 FE 在原版短整数通道中溢出；槽位操作由服务端负责。
- 服务器配置保持默认的证书校验，不需要为了教程的工程拆分关闭下载校验。

- 机器和缓存菜单通过 `MenuItemPage` 将服务端槽位映射到真实仓库；客户端使用独立的27槽显示缓存接收原版菜单槽位消息，不依赖客户端未同步的 `members` 集合或远处仓库区块。机器界面的翻页由服务端校验，Shift输入访问完整输入通道。

开发运行时使用 JEI 和 Jade。加工测试探针只放在 src/gametest/resources/data/alone_mechanical_test/recipes，不参与普通 runClient 或发布 jar。电弧炉读取外部已有熔炼配方，不自动注册任何新配方。

统一仓储：正式入口为 `storage_tank`、`universal_port`，旧 `fluid_tank`、`gas_tank` 和三类端口保留注册用于读取存档，创造栏只展示统一入口，旧方块掉落统一物品。`PartKind` 新枚举追加在末尾，保持已有 NBT ordinal 不变。储罐按竖向内容分组，空列通过实际接受的介质选择液体或气体，SIMULATE 不写世界；非空列拒绝异相介质，排空保留记忆类型但允许下一次输入自动切换。旧端口同样按附着仓储选择能力。`StorageConnections` 根据相邻仓储动态寻址，不再按端口注册类型筛选，机器通道按储罐实际介质汇总。反应釜、合金及分离机器刷新实际存储类型，空输出罐可按适配配方准备介质；转换机保持 GUI 方向约束。

`ConnectedStorageBlock` / `ConnectedStorageModel` 只连接同一竖向仓储列，侧面使用上接/下接两位掩码选择4个原生16px变体，隐藏内部端盖，外露端面固定使用完整边框。相邻独立列不横向连接；储罐以实际内容的自底向上分段规则判断连接，通过渲染区域快照读取局部内容，不访问实时世界或加载区块。对应材质的接缝RGBA逐像素一致。槽位、容量和多方块独立列计数不受影响。储罐液体与气体通过窗口渲染实际内容。`StorageMenu` 采用27/54槽的分页映射，旧81槽偏好迁移为54槽；尺寸切换通过服务端重开菜单实现，携带栈单独转移至新菜单，页位置换算后钳制，原物理仓库不变。`StorageScreen` 对超过窗口的视图单独缩放并同步转换鼠标坐标，不改全局GUI设置。`UnifiedStorageClientCheck` 检查实际世界中四侧连接变体、内部端盖、相邻独立列，并执行真实服务器菜单尺寸往返，验证携带物品、108个实体槽和视口适配。

气液转换机使用 `conversion_core` / `conversion` 核心及 `gas_liquid_converter` 机器 ID。结构恰好一个独立流体列和一个独立气体列，端口仅使用 INPUT_1、OUTPUT_1，初次成型由端口推断方向。`ConversionMode` 以字符串保存；GUI 通过原版菜单动作10、11申请方向，服务端验证距离、菜单有效性与完整结构后，统一反转端口、清空加工进度／批次／配方状态，保留真实库存和电量。菜单整数30同步当前方向，完整流体和气体缓存复用现有消息。配方的 `conversion` 字段区分两个方向，并对输入／输出资源形状进行校验；只有转换机配方的网络数据附带此字段，原有配方格式保持兼容。测试配方仅位于 gameTest 资源，正式内容由数据包提供。

双相反应釜共用 `reaction` 核心家族和 `reaction_core` 方块。`MachineDefinitions.REACTORS` 定义九种输入／输出资源配置，由独立仓储列的三个端口模式确定，而不是仅根据仓储种类集合猜测。`MultiblockStructure` 要求恰好三个独立列，分别为 INPUT_1、INPUT_2、OUTPUT_1；匹配对应资源端口、完整外壳、控制器、能量矩阵、边界及重叠规则共用。`ReactorStores` 随控制器 NBT 和客户端更新保存实际通道顺序；`recipeInputChannel` 允许两个异类输入交换，保持数据包配方的规范顺序。反应釜的液体和气体通道仅汇总实体仓储，不包括控制器暂存区。物品页大小支持9槽，三个物品通道独立分页和 Shift 搬运。

`FluidMenuData` / `FluidMenuSync` 同步完整 FluidStack（含 NBT）、32位液量和容量；`GasMenuData` / `GasMenuSync` 同步独立 GasStack ID、气量及容量。消息仅服务端到客户端，按菜单 ID 分发，变化时发送，不依赖远处客户端仓储区块。液体和气体变动发送储罐实体更新。`TankColumnVolume` 只读汇总同一真实储罐列的内容，自底向上计算整体液位；`FluidTankRenderer` 按各方块所占切片渲染，以保留正常距离/视锥裁剪。只有整列底部和实际液面有水平面，中间切片延伸到方块边界且UV连续，消除逐段留白与内部重叠面。实际资源仍留在原段，独立列及不同介质分区不混合。液体使用自身静态纹理与染色，气体使用独立图案。`TankColumnVolumeGameTests` 检查分散存储、上段进液、空/半满/全满、长列体积与分区；客户端检查真实整列液位并生成效果图。JEI 注册九个反应模式分类。`HammerSelectionPreview` 显示第一角点、当前目标构成的三维框及尺寸，服务端成型消息显示识别模式和仓储通道。正式资源不注册假定的反应材料配方。

开发客户端每次启动前把所属source set的编译类与资源复制到独立的`.gradle/client-runtime/<启动ID>/`快照，MOD_CLASSES和运行类路径使用快照；后续编译或清理build不会改变运行中的类文件，避免连接机器端口时延迟加载的能力实现类发生NoClassDefFoundError。修改Java后需重新启动客户端才能使用新实现。客户端资源检查额外解析物品、流体、气体端口的能力实现。

物品图标保持原生16×16透明PNG，八种粉末使用不同轮廓、配色和成组颗粒高光；碳粉独立于煤粉，钢锭使用金属锭轮廓。扳手和工程锤增强工具结构及金属／手柄对比。`tools/generate_resources.ps1 -ItemsOnly`仅重生成十二个物品图标。

合金冶炼机使用 `alloy_core` 方块、`alloy` 核心家族和 `alloy_smelter` 机器 ID。结构要求恰好三个独立缓存列：输入1物品、输入2物品/流体/气体之一、输出1物品。实际通道类型沿用 `ReactorStores` 保存和同步，但配方通道顺序固定，输入1主料、输入2辅料。流体与气体仅来自实际储罐。菜单复用三列布局，按物品列数量创建9槽分页显示。配方加载与网络读取均校验合金形状，拒绝缺失辅料、混合多种辅料或非物品输出；运行时匹配也执行同样检查。默认 `alloy_steel` 为2铁粉+1碳粉→2钢锭，200 tick、40 FE/t，沿用通用并行、线圈、电池和完成前资源复查。`crush_carbon` 提供煤/木炭制碳链，所有材料具备Forge标签、原创16×16材质及中英文说明。构建与数据包详见 [alloy-smelter.md](alloy-smelter.md)。

三相分离器使用 `separation_core` 方块及 `separation` 核心家族，`SEPARATORS` 定义18种单输入、双输出介质配置。`resolveSeparator` 读取通道0、2、3，要求通道1为空；不同输出介质允许交换，由 `recipeOutputChannel` 为输出模拟和完成提交提供相同映射。JSON和网络加载校验一种输入及两种产物的完整形状，物品输出数组支持null/{}占位。空通用输出储罐通过匹配独立分离配方准备介质，实体库存与 `ReactorStores` 同步；不创建控制器隐式罐。菜单 `threeColumns` / `columnChannel` 把三列映射为0、2、3，整数28/29在分离器中表示输出2分页，在反应釜中仍表示输入2分页，其他同步索引保持兼容。原有并行计算同时检查两路输出。独立水电解配方将1000 mB水变为1000 mB氢气与500 mB氧气，200 tick、60 FE/t。十八种JEI分类、核心用途、中英文气体与配方名、原生16×16核心材质及教程由资源脚本生成。测试与数据包文档见 [three-phase-separator.md](three-phase-separator.md)。

核心正上方的唯一机型组件由 `MachineDefinitions.assemblyFamily` 统一解析；构建验证和运行时解析复用同一规则。工业+锻压、熔炼+煅烧/合金、反应+化合/分解的搭建与配方入口见 [machine-components.md](machine-components.md)。旧独立合金/分离核心只保留存档注册。

气体外观由 `GasRendering.sprite/color/draw` 统一维护：默认复用水的动画图集，氢气白色、氧气天蓝色。`FluidRendering.drawSprite` 统一GUI纹理铺贴和染色；JEI显示数据保留独立GasStack数组，按输入输出通道绘制气体图标和悬浮名称/数量。`gas-appearance-client-check.gradle` 验证动画帧、颜色、世界储罐及GUI截图。
