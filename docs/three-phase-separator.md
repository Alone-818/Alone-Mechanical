# 三相分离器

分离器使用独立的**分离核心**（`alone_mechanical:separation_core`），处理**一路输入→两路输出**的分解配方。配方由数据包独立定义。反应釜和分离器分别查询自己的机器 ID，没有自动反转或推导另一台机器配方的逻辑。

## 构建与类型

完整长方体外壳，中心放分离核心，外表面放一个控制器，至少一个能量矩阵。恰好三个独立竖向仓储列：

| 通道 | 职责 | 可用仓储 |
| --- | --- | --- |
| 输入1 | 待分解原料 | 物品仓、流体储罐、气体储罐 |
| 输出1 | 第一种产物 | 物品仓、流体储罐、气体储罐 |
| 输出2 | 第二种产物 | 物品仓、流体储罐、气体储罐 |

端口直接放在各列顶部或底部，扳手设定输入1、输出1、输出2。不能使用输入2，两个输出也不能共用同一列。用工程锤右键两个相对角点完成检测，潜行右键取消。右键成型结构任意部件打开GUI：左侧输入、右侧两个输出；物品列各自分页，两个输出禁止放入物品。

物品、液体、气体的三种输入与六种输出组合，共18种类型。表内顺序是配方的规范输出顺序；异类输出允许交换物理通道，会自动映射到对应介质仓储。同类输出按输出1、输出2顺序处理。

| 输入 | 输出组合 | 类型 | machine ID（前缀均为 `alone_mechanical:separator_`） |
| --- | --- | --- | --- |
| 物品 | 物品＋物品 | 固相分选 | `solid_separation` |
| 物品 | 物品＋流体 | 固液浸分 | `solid_liquid_separation` |
| 物品 | 物品＋气体 | 固气解离 | `solid_gas_separation` |
| 物品 | 流体＋流体 | 熔相分离 | `melt_separation` |
| 物品 | 流体＋气体 | 热解分离 | `pyrolytic_separation` |
| 物品 | 气体＋气体 | 气化分解 | `gasifying_separation` |
| 流体 | 物品＋物品 | 液相析固 | `liquid_precipitation` |
| 流体 | 物品＋流体 | 固液分离 | `slurry_separation` |
| 流体 | 物品＋气体 | 液相分解 | `liquid_decomposition` |
| 流体 | 流体＋流体 | 液液分离 | `liquid_fractionation` |
| 流体 | 流体＋气体 | 脱气分离 | `degassing` |
| 流体 | 气体＋气体 | 电解分离 | `electrolysis` |
| 气体 | 物品＋物品 | 气相沉积 | `gas_deposition` |
| 气体 | 物品＋流体 | 冷凝析固 | `condensing_separation` |
| 气体 | 物品＋气体 | 气固分离 | `gas_solid_separation` |
| 气体 | 流体＋流体 | 深冷分离 | `cryogenic_separation` |
| 气体 | 流体＋气体 | 气液分离 | `gas_liquid_separation` |
| 气体 | 气体＋气体 | 气体分离 | `gas_fractionation` |

通用储罐按实际介质参与识别。对于可匹配的独立分离配方，空输出罐会准备为该配方需要的液体或气体类型；已有内容的输出罐保留其介质，若不兼容则等待清空。物品仓和储罐之间的结构变化需要重新锤定。

## 首个独立配方：水电解

**1000 mB 水 → 1000 mB 氢气＋500 mB 氧气**，基础200 tick、60 FE/t。游戏配方的氢氧体积比为2∶1。输入1接水，输出1收氢气，输出2收氧气。使用通用储罐时，先向输入1装水，两个空输出罐会准备为气体罐。

任一输出空间不足或内容不兼容，整轮加工暂停。完成前再次验证输入和两路输出，然后消耗原料、写入两个产物。请为两种产物都安排出口，以免其中一路积满后停机。

沿用能量线圈、效率线圈、能量矩阵、强化外壳和处理矩阵的升级规则。每个效率线圈增加15%速度和10%耗能倍率；每个处理矩阵增加1个并行位和15%耗能倍率，无额外固定数量上限。并行量同时受输入、输出1和输出2的空间限制。5个矩阵可同周期电解6份：需要6000 mB水，产出6000 mB氢气与3000 mB氧气，未安装线圈时为630 FE/t。加工进度、批次数、物理仓储和机器类型随存档保存。

## 数据包

文件位于 `data/<命名空间>/recipes/<配方名>.json`。水电解的完整格式如下：

```json
{
  "type": "alone_mechanical:machine",
  "machine": "alone_mechanical:separator_electrolysis",
  "fluid_inputs": [{ "fluid": "minecraft:water", "amount": 1000 }],
  "gas_outputs": [
    { "gas": "alone_mechanical:hydrogen", "amount": 1000 },
    { "gas": "alone_mechanical:oxygen", "amount": 500 }
  ],
  "ticks": 200,
  "energy_per_tick": 60
}
```

每种资源数组均按通道占位：输入数组第一个位置是输入1，第二个位置必须为空；输出数组分别是输出1、输出2。混合输出示例，`separator_slurry_separation` 的物品＋流体产物应写为：

```json
"item_outputs": [{ "item": "minecraft:clay_ball", "count": 1 }, null],
"fluid_outputs": [null, { "fluid": "minecraft:water", "amount": 500 }]
```

完整配方还需要匹配类型的输入1、耗时和能量。空位置支持`null`或`{}`。每个输出必须且只能有一种介质，两路都必须存在。非法形状会在JSON加载和网络读取时被拒绝。气体通过本模组独立 `GasCapabilities.GAS` 接口传输。

配方显示名称使用语言键：`recipe.<命名空间>.<路径>`，路径中的`/`改为`.`。首个配方的键是 `recipe.alone_mechanical.water_electrolysis`，显示“水电解 / Water Electrolysis”。机器类型名、配方名分别维护，便于同类型添加不同分解配方。

## 验证

`SeparatorGameTests` 验证18种类型及交换输出、配方网络往返、两路堵塞保护、水电解、32份并行和存档、空储罐介质准备、输出2分页与防放入、非法结构和配方隔离。

```powershell
./tools/verify_separator_resources.ps1
./tools/verify_textures.ps1
./gradlew.bat --offline runGameTestServer build
```
