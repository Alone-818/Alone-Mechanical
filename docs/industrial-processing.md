# 塑形机与灌注机

现已加入铜线、钢齿轮、分级电池、成品高级电池二次灌注和末地电路配方，详见[机器制造与电池](machine-manufacturing.md)。下方保留机器与数据包接口说明；矿物灌注当前使用氧气，酸溶5倍路线以[矿物倍产](ore-processing-design.md)为准。

两种机器均使用中心工业核心，机型组件必须紧贴核心正上方。遵守长方体外壳、唯一表面控制器、至少一个能量矩阵、仓储列上下端口及通道独立规则。线圈、处理矩阵、FE、分页与存档机制沿用通用机器实现。

| 组件 | 机器 | 仓储列 | 配方machine ID |
| --- | --- | --- | --- |
| 塑形组件 | 塑形机 | 恰好输入1物品仓、输出1物品仓 | `alone_mechanical:forming_machine` |
| 灌注组件 | 灌注机 | 恰好输入1物品仓、输入2流体或气体罐、输出1物品仓 | `alone_mechanical:infuser` |

每段物品仓27槽，堆叠扩容；灌注辅料不能用物品仓，也不能同时使用流体与气体。塑形机保留独立配方入口。灌注机支持原有维度转换配方，并新增铁、铜、金的硫酸浸出：1粗矿＋250 mB液态硫酸→3浸出精矿，精矿逐个1∶1粉碎、熔炼，最终获得3锭。完整三级产线见 [矿物倍产](ore-processing-design.md)。

配方类型沿用`alone_mechanical:machine`。下面仅为未注册的字段模板，材料ID与参数由数据包作者提供：

```json
{
  "type": "alone_mechanical:machine",
  "machine": "alone_mechanical:forming_machine",
  "item_inputs": [{ "ingredient": { "item": "your_mod:material" }, "count": 1 }],
  "item_outputs": [{ "item": "your_mod:formed_part", "count": 1 }],
  "ticks": 100,
  "energy_per_tick": 20
}
```

灌注配方将`machine`改为`alone_mechanical:infuser`，物品输入输出仍使用第一项，并额外选择以下一种辅料字段。数组开头的`null`必须保留，使辅料位于输入2。

```json
"fluid_inputs": [null, { "fluid": "your_mod:infusion_fluid", "amount": 250 }]
```

或：

```json
"gas_inputs": [null, { "gas": "your_mod:infusion_gas", "amount": 250 }]
```

不允许输出2、流体/气体产物或输入1介质辅料。非法配方在JSON加载和网络读取时拒绝，运行时再次检查。输入不足、输出堵塞或能源不足时不消耗资源；完成前复查全部资源。加工配方与其他机型独立，不自动借用粉碎、锻压、熔炼或合金配方。

初级、中级、高级、高功率、长续航五种电池的现有配方均已删除，资源生成脚本不再生成它们；电池物品及既有燃料功能保留。
