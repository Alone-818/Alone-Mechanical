# 正式配方入口

正式资源位于 `src/main/resources/data/alone_mechanical/recipes`。机器配方使用 `alone_mechanical:machine`，工作台制造使用 `minecraft:crafting_shaped` 或 `minecraft:crafting_shapeless`。

| 入口 | 机器/组件 | 主要输出 |
| --- | --- | --- |
| `manufacturing_machine_casing.json` | 工作台基础制造 | 机器外壳 |
| `manufacturing_structural_glass.json` | 工作台外壳延伸制造 | 结构玻璃 |
| `crush_raw_*.json`、`ore_nonmetal_*.json` | 粉碎组件 | 金属粉、钻石/绿宝石/红石/青金石中间品 |
| `alloy_steel.json`、`manufacturing_alloy_*.json` | 合金组件 | 钢与管道合金 |
| `dimension_void_amplification.json` | 化合组件 | 250 mB 虚空 + 1000 mB 水 → 500 mB 虚空 |
| `void_recreate_*.json` | 灌注组件 | 铁、铜、金、铀、红石、青金石、钻石和绿宝石矿石 |
| `manufacturing_synthesis_component.json` | 合成组件 | 定向合成机组件 |
| `zipline_hook.json` | 钢与机械 | 滑索挂钩 |
| `grid_analyzer.json` | 工业起步 | 电网节点诊断 |
| `pipe_filter_chip.json` | 化学与自动化 | 物品/流体/气体白名单样本 |
| `priority_chip.json` | 高级能源 | 管道分支优先级 |
| `maintenance_battery_pack.json` | 高级能源 | 便携式耐久维护 |

JEI 的机器分类从服务端配方管理器重建，显示时间、FE、科技阶段和所需上置组件。教程书只描述实际代码支持的仓储列、端口、介质和暂停条件。
