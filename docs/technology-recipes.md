# 正式配方清单

核对日期：2026-10-20。由 tools/generate_technology_catalog.ps1 从正式JSON生成；不含测试配方。此清单不更改配方或注册内容。科技依赖关系见[科技线总览](current-technology-tree.md)。

共 213 条正式配方：

| 配方类型 | 数量 |
| --- | ---: |
| alone_mechanical:machine | 129 |
| minecraft:blasting | 7 |
| minecraft:crafting_shaped | 66 |
| minecraft:crafting_shapeless | 4 |
| minecraft:smelting | 7 |

下列时间 / FE为JSON基础值，不含线圈、并行、塔强度。未显式指定时按序列化器默认100 tick、20 FE/t列出（锅炉与核堆默认0 FE/t）。输入1/2及输出1/2按各介质数组的原始位置显示，空占位不挪动；异种介质通道可依机器规则换位，同种介质须遵循顺序。标注标签的原料由实际数据包决定。

涡轮、光能与燃料发电、抽液和范围作业由代码驱动，不在JSON清单中；电弧炉动态读取熔炉配方，不能以此处没有电弧炉JSON判断为未实现。

## 分离塔（1条）

机型ID：`alone_mechanical:air_separation_tower`。

| 配方ID | 输入 | 输出 | tick | FE/t |
| --- | --- | --- | ---: | ---: |
| `air_separation` | 无需原料（环境空气） | 输出1 · 氮气 400 mB<br>输出2 · 氧气 100 mB | 100 | 20 |

## 合金冶炼机（6条）

机型ID：`alone_mechanical:alloy_smelter`。

| 配方ID | 输入 | 输出 | tick | FE/t |
| --- | --- | --- | ---: | ---: |
| `alloy_steel` | 输入1 · #forge:dusts/iron ×2<br>输入2 · #forge:dusts/carbon ×1 | 输出1 · 钢锭 ×2 | 200 | 40 |
| `manufacturing_alloy_invar` | 输入1 · 铁板 ×2<br>输入2 · 钢粉 ×1 | 输出1 · 热殷钢坯 ×3 | 200 | 50 |
| `manufacturing_alloy_quantum` | 输入1 · 超导锭 ×2<br>输入2 · minecraft:netherite_ingot ×1 | 输出1 · 量子合金基体 ×2 | 320 | 260 |
| `manufacturing_alloy_red_copper` | 输入1 · 铜粉 ×2<br>输入2 · minecraft:redstone ×1 | 输出1 · 赤铜混合料 ×2 | 140 | 30 |
| `manufacturing_alloy_resonant` | 输入1 · 殷钢锭 ×2<br>输入2 · minecraft:ender_pearl ×1 | 输出1 · 共振合金基体 ×2 | 220 | 80 |
| `manufacturing_alloy_superconducting` | 输入1 · 共振锭 ×2<br>输入2 · 钻石粉 ×1 | 输出1 · 超导合金基体 ×2 | 260 | 140 |

## 锅炉（1条）

机型ID：`alone_mechanical:boiler`。

| 配方ID | 输入 | 输出 | tick | FE/t |
| --- | --- | --- | ---: | ---: |
| `boiler_water_steam` | 输入1 · minecraft:water 50 mB<br>输入2 · 高温液态钠 1 mB | 输出2 · 液态钠 1 mB<br>输出1 · 蒸汽 50 mB | 1 | 0 |

## 冷凝塔（2条）

机型ID：`alone_mechanical:condensation_tower`。

| 配方ID | 输入 | 输出 | tick | FE/t |
| --- | --- | --- | ---: | ---: |
| `heavy_water_condensation` | 输入1 · 重水蒸汽 1000 mB | 输出1 · 重水 1000 mB | 100 | 40 |
| `tower_condense_steam` | 输入1 · 蒸汽 1000 mB | 输出1 · minecraft:water 1000 mB | 100 | 20 |

## 粉碎机（17条）

机型ID：`alone_mechanical:crusher`。

| 配方ID | 输入 | 输出 | tick | FE/t |
| --- | --- | --- | ---: | ---: |
| `crush_carbon` | 输入1 · minecraft:coal / minecraft:charcoal ×1 | 输出1 · 碳粉 ×1 | 100 | 20 |
| `crush_raw_copper` | 输入1 · minecraft:raw_copper / #forge:raw_materials/copper ×1 | 输出1 · 铜粉 ×2 | 100 | 20 |
| `crush_raw_gold` | 输入1 · minecraft:raw_gold / #forge:raw_materials/gold ×1 | 输出1 · 金粉 ×2 | 100 | 20 |
| `crush_raw_iron` | 输入1 · minecraft:raw_iron / #forge:raw_materials/iron ×1 | 输出1 · 铁粉 ×2 | 100 | 20 |
| `fuel_crush_raw_uranium` | 输入1 · #forge:raw_materials/uranium ×1 | 输出1 · 铀粉 ×2 | 100 | 20 |
| `fuel_crush_uranium_ingot` | 输入1 · #forge:ingots/uranium ×1 | 输出1 · 铀粉 ×1 | 100 | 20 |
| `manufacturing_crush_diamond` | 输入1 · minecraft:diamond ×1 | 输出1 · 钻石粉 ×1 | 120 | 40 |
| `manufacturing_crush_quartz` | 输入1 · minecraft:quartz ×1 | 输出1 · 石英砂 ×2 | 80 | 20 |
| `ore_crush_leached_copper` | 输入1 · 铜矿团 ×1 | 输出1 · 铜粉 ×1 | 60 | 20 |
| `ore_crush_leached_gold` | 输入1 · 金矿团 ×1 | 输出1 · 金粉 ×1 | 60 | 20 |
| `ore_crush_leached_iron` | 输入1 · 铁矿团 ×1 | 输出1 · 铁粉 ×1 | 60 | 20 |
| `ore_nonmetal_crush_diamond` | 输入1 · #forge:ores/diamond ×1 | 输出1 · minecraft:diamond ×2 | 100 | 20 |
| `ore_nonmetal_crush_lapis` | 输入1 · #forge:ores/lapis ×1 | 输出1 · minecraft:lapis_lazuli ×18 | 100 | 20 |
| `ore_nonmetal_crush_redstone` | 输入1 · #forge:ores/redstone ×1 | 输出1 · minecraft:redstone ×10 | 100 | 20 |
| `ore_nonmetal_crush_washed_diamond` | 输入1 · 钻石矿团 ×1 | 输出1 · minecraft:diamond ×1 | 60 | 20 |
| `ore_nonmetal_crush_washed_lapis` | 输入1 · 青金石矿团 ×1 | 输出1 · minecraft:lapis_lazuli ×9 | 60 | 20 |
| `ore_nonmetal_crush_washed_redstone` | 输入1 · 红石矿团 ×1 | 输出1 · minecraft:redstone ×5 | 60 | 20 |

## 双相反应釜 · 溶解析晶（2条）

机型ID：`alone_mechanical:crystallization`。

| 配方ID | 输入 | 输出 | tick | FE/t |
| --- | --- | --- | ---: | ---: |
| `fuel_thorium_heavy_water` | 输入1 · 贫化铀粉 ×4<br>输入2 · 重水 100 mB | 输出1 · 钍粉 ×4 | 200 | 60 |
| `sulfur_from_coal` | 输入1 · minecraft:coal / minecraft:charcoal ×1<br>输入2 · minecraft:water 1000 mB | 输出1 · 硫 ×1 | 200 | 40 |

## 双相反应釜 · 溶出制液（16条）

机型ID：`alone_mechanical:dissolution`。

| 配方ID | 输入 | 输出 | tick | FE/t |
| --- | --- | --- | ---: | ---: |
| `coolant_raw_water_preparation` | 输入1 · minecraft:gravel ×1<br>输入2 · minecraft:water 1000 mB | 输出1 · 含盐原水 1000 mB | 100 | 40 |
| `dimension_end_preparation_native` | 输入1 · minecraft:popped_chorus_fruit ×1<br>输入2 · 重水 1000 mB | 输出1 · 末地维度转换液 1000 mB | 240 | 120 |
| `dimension_end_preparation` | 输入1 · minecraft:ender_pearl ×4<br>输入2 · 重水 1000 mB | 输出1 · 末地维度转换液 250 mB | 400 | 240 |
| `dimension_nether_preparation_overworld` | 输入1 · minecraft:obsidian ×8<br>输入2 · minecraft:lava 1000 mB | 输出1 · 地狱维度转换液 250 mB | 400 | 240 |
| `dimension_nether_preparation` | 输入1 · minecraft:blaze_powder ×1<br>输入2 · minecraft:lava 1000 mB | 输出1 · 地狱维度转换液 1000 mB | 200 | 80 |
| `fuel_enriched_uranium_blending` | 输入1 · 浓缩铀化合物 ×1<br>输入2 · minecraft:water 1000 mB | 输出1 · 浓缩铀燃料 1000 mB | 120 | 80 |
| `fuel_mox_blending` | 输入1 · 混合燃料化合物 ×1<br>输入2 · minecraft:water 1000 mB | 输出1 · MOX燃料 1000 mB | 120 | 80 |
| `fuel_thorium_blending` | 输入1 · 钍粉 ×4<br>输入2 · minecraft:water 1000 mB | 输出1 · 钍燃料 1000 mB | 120 | 60 |
| `fuel_uranium_dissolution` | 输入1 · 铀粉 ×4<br>输入2 · 液态硫酸 1000 mB | 输出1 · 铀矿浆 1000 mB | 200 | 80 |
| `fuel_uranium_ingot_dissolution` | 输入1 · #forge:ingots/uranium ×4<br>输入2 · 液态硫酸 1000 mB | 输出1 · 铀矿浆 1000 mB | 240 | 100 |
| `manufacturing_basic_electrolyte` | 输入1 · 硫 ×2<br>输入2 · minecraft:water 1000 mB | 输出1 · 初级电解液 1000 mB | 100 | 30 |
| `manufacturing_high_power_conditioner` | 输入1 · minecraft:redstone ×4<br>输入2 · 高级电解液 1000 mB | 输出1 · 高功率改性液 1000 mB | 160 | 60 |
| `manufacturing_long_life_stabilizer` | 输入1 · 石英砂 ×4<br>输入2 · 高级电解液 1000 mB | 输出1 · 长续航稳定液 1000 mB | 160 | 60 |
| `ore_dissolve_copper` | 输入1 · #forge:raw_materials/copper ×1<br>输入2 · 液态硫酸 1000 mB | 输出1 · 粗铜矿浆 1000 mB | 200 | 80 |
| `ore_dissolve_gold` | 输入1 · #forge:raw_materials/gold ×1<br>输入2 · 液态硫酸 1000 mB | 输出1 · 粗金矿浆 1000 mB | 200 | 80 |
| `ore_dissolve_iron` | 输入1 · #forge:raw_materials/iron ×1<br>输入2 · 液态硫酸 1000 mB | 输出1 · 粗铁矿浆 1000 mB | 200 | 80 |

## 蒸馏塔（3条）

机型ID：`alone_mechanical:distillation_tower`。

| 配方ID | 输入 | 输出 | tick | FE/t |
| --- | --- | --- | ---: | ---: |
| `coolant_brine_concentration` | 输入1 · 含盐原水 1000 mB | 输出1 · minecraft:water 900 mB<br>输出2 · 浓缩卤水 100 mB | 200 | 60 |
| `coolant_salt_melting` | 输入1 · 浓缩卤水 1000 mB | 输出1 · 熔融氯化钠 200 mB<br>输出2 · 蒸汽 800 mB | 200 | 120 |
| `water_enrichment` | 输入1 · minecraft:water 1000 mB | 输出2 · 富集水 50 mB<br>输出1 · 蒸汽 950 mB | 100 | 40 |

## 交换塔（1条）

机型ID：`alone_mechanical:exchange_tower`。

| 配方ID | 输入 | 输出 | tick | FE/t |
| --- | --- | --- | ---: | ---: |
| `tower_exchange_heat` | 输入1 · minecraft:water 50 mB<br>输入2 · 高温液态钠 1 mB | 输出2 · 液态钠 1 mB<br>输出1 · 蒸汽 50 mB | 1 | 20 |

## 锻压机（4条）

机型ID：`alone_mechanical:forging_press`。

| 配方ID | 输入 | 输出 | tick | FE/t |
| --- | --- | --- | ---: | ---: |
| `manufacturing_press_copper_plate` | 输入1 · minecraft:copper_ingot ×1 | 输出1 · 铜板 ×1 | 60 | 20 |
| `manufacturing_press_invar` | 输入1 · 热殷钢坯 ×1 | 输出1 · 殷钢锭 ×1 | 100 | 40 |
| `manufacturing_press_iron_plate` | 输入1 · minecraft:iron_ingot ×1 | 输出1 · 铁板 ×1 | 60 | 20 |
| `manufacturing_press_steel_plate` | 输入1 · 钢锭 ×1 | 输出1 · 钢板 ×1 | 60 | 20 |

## 塑形机（2条）

机型ID：`alone_mechanical:forming_machine`。

| 配方ID | 输入 | 输出 | tick | FE/t |
| --- | --- | --- | ---: | ---: |
| `manufacturing_form_copper_wire` | 输入1 · 铜板 ×1 | 输出1 · 铜线 ×4 | 60 | 20 |
| `manufacturing_form_steel_gear` | 输入1 · 钢板 ×2 | 输出1 · 钢齿轮 ×1 | 100 | 30 |

## 气液转换机（1条）

机型ID：`alone_mechanical:gas_liquid_converter`。

| 配方ID | 输入 | 输出 | tick | FE/t |
| --- | --- | --- | ---: | ---: |
| `sulfuric_acid_liquefaction` | 输入1 · 硫酸气体 1000 mB | 输出1 · 液态硫酸 1000 mB | 100 | 40 |

## 双相反应釜 · 气相化合（4条）

机型ID：`alone_mechanical:gas_reactor`。

| 配方ID | 输入 | 输出 | tick | FE/t |
| --- | --- | --- | ---: | ---: |
| `agri_ammonia_synthesis` | 输入1 · 氮气 1000 mB<br>输入2 · 氢气 3000 mB | 输出1 · 氨气 2000 mB | 200 | 80 |
| `heavy_water_synthesis` | 输入1 · 氘气 1000 mB<br>输入2 · 氧气 500 mB | 输出1 · 重水蒸汽 1000 mB | 100 | 80 |
| `sulfur_trioxide` | 输入1 · 二氧化硫 1000 mB<br>输入2 · 氧气 500 mB | 输出1 · 三氧化硫 1000 mB | 160 | 60 |
| `sulfuric_acid_gas` | 输入1 · 三氧化硫 1000 mB<br>输入2 · 蒸汽 1000 mB | 输出1 · 硫酸气体 1000 mB | 120 | 60 |

## 双相反应釜 · 气固相炼（4条）

机型ID：`alone_mechanical:gas_solid_refining`。

| 配方ID | 输入 | 输出 | tick | FE/t |
| --- | --- | --- | ---: | ---: |
| `agri_fertilizer` | 输入1 · minecraft:bone_meal ×1<br>输入2 · 氨气 500 mB | 输出1 · 化工肥料 ×4 | 100 | 40 |
| `ore_reduce_copper` | 输入1 · 铜晶体 ×1<br>输入2 · 氢气 200 mB | 输出1 · 铜矿团 ×1 | 100 | 80 |
| `ore_reduce_gold` | 输入1 · 金晶体 ×1<br>输入2 · 氢气 200 mB | 输出1 · 金矿团 ×1 | 100 | 80 |
| `ore_reduce_iron` | 输入1 · 铁晶体 ×1<br>输入2 · 氢气 200 mB | 输出1 · 铁矿团 ×1 | 100 | 80 |

## 灌注机（39条）

机型ID：`alone_mechanical:infuser`。

| 配方ID | 输入 | 输出 | tick | FE/t |
| --- | --- | --- | ---: | ---: |
| `infuse_end_cobblestone` | 输入1 · minecraft:cobblestone ×1<br>输入2 · 末地维度转换液 250 mB | 输出1 · minecraft:end_stone ×1 | 100 | 40 |
| `infuse_end_flowers` | 输入1 · #minecraft:flowers ×1<br>输入2 · 末地维度转换液 250 mB | 输出1 · minecraft:chorus_flower ×1 | 100 | 40 |
| `infuse_end_seeds` | 输入1 · #alone_mechanical:dimension_conversion_seeds ×1<br>输入2 · 末地维度转换液 250 mB | 输出1 · minecraft:chorus_fruit ×1 | 100 | 40 |
| `infuse_end_stone` | 输入1 · minecraft:stone ×1<br>输入2 · 末地维度转换液 250 mB | 输出1 · minecraft:end_stone ×1 | 100 | 40 |
| `infuse_end_water_bottle` | 输入1 · minecraft:potion（NBT {"Potion":"minecraft:water"}） ×1<br>输入2 · 末地维度转换液 250 mB | 输出1 · minecraft:dragon_breath ×1 | 100 | 40 |
| `infuse_nether_clay` | 输入1 · minecraft:clay ×1<br>输入2 · 地狱维度转换液 250 mB | 输出1 · minecraft:netherrack ×1 | 100 | 40 |
| `infuse_nether_cobblestone` | 输入1 · minecraft:cobblestone ×1<br>输入2 · 地狱维度转换液 250 mB | 输出1 · minecraft:blackstone ×1 | 100 | 40 |
| `infuse_nether_flowers` | 输入1 · #minecraft:flowers ×1<br>输入2 · 地狱维度转换液 250 mB | 输出1 · minecraft:crimson_fungus ×1<br>输出1二选一：minecraft:warped_fungus ×1（与主产物各50%，非同时产出） | 100 | 40 |
| `infuse_nether_lapis` | 输入1 · minecraft:lapis_lazuli ×1<br>输入2 · 地狱维度转换液 250 mB | 输出1 · minecraft:quartz ×1 | 100 | 40 |
| `infuse_nether_redstone` | 输入1 · minecraft:redstone ×1<br>输入2 · 地狱维度转换液 250 mB | 输出1 · minecraft:glowstone_dust ×1 | 100 | 40 |
| `infuse_nether_sand` | 输入1 · minecraft:sand ×1<br>输入2 · 地狱维度转换液 250 mB | 输出1 · minecraft:soul_sand ×1 | 100 | 40 |
| `infuse_nether_seeds` | 输入1 · #alone_mechanical:dimension_conversion_seeds ×1<br>输入2 · 地狱维度转换液 250 mB | 输出1 · minecraft:nether_wart ×1 | 100 | 40 |
| `infuse_nether_stone` | 输入1 · minecraft:stone ×1<br>输入2 · 地狱维度转换液 250 mB | 输出1 · minecraft:basalt ×1 | 100 | 40 |
| `infuse_nether_vines` | 输入1 · minecraft:vine ×1<br>输入2 · 地狱维度转换液 250 mB | 输出1 · minecraft:weeping_vines ×1<br>输出1二选一：minecraft:twisting_vines ×1（与主产物各50%，非同时产出） | 100 | 40 |
| `manufacturing_infuse_advanced_battery` | 输入1 · 高级空电池壳 ×1<br>输入2 · 高级电解液 1000 mB | 输出1 · 高级电池 ×1 | 200 | 60 |
| `manufacturing_infuse_basic_battery` | 输入1 · 初级空电池壳 ×1<br>输入2 · 初级电解液 250 mB | 输出1 · 初级电池 ×1 | 100 | 30 |
| `manufacturing_infuse_high_power_battery` | 输入1 · 高级电池 ×1<br>输入2 · 高功率改性液 250 mB | 输出1 · 高级电池 · 高功率 ×1 | 160 | 60 |
| `manufacturing_infuse_intermediate_battery` | 输入1 · 中级空电池壳 ×1<br>输入2 · 中级电解液 500 mB | 输出1 · 中级电池 ×1 | 160 | 40 |
| `manufacturing_infuse_long_life_battery` | 输入1 · 高级电池 ×1<br>输入2 · 长续航稳定液 250 mB | 输出1 · 高级电池 · 长续航 ×1 | 160 | 60 |
| `manufacturing_infuse_quantum` | 输入1 · 量子合金基体 ×2<br>输入2 · 液态钠 500 mB | 输出1 · 量子锭 ×2 | 300 | 240 |
| `manufacturing_infuse_resonant` | 输入1 · 共振合金基体 ×2<br>输入2 · 氧气 500 mB | 输出1 · 共振锭 ×2 | 180 | 80 |
| `manufacturing_infuse_superconducting` | 输入1 · 超导合金基体 ×2<br>输入2 · 重水 500 mB | 输出1 · 超导锭 ×2 | 240 | 140 |
| `manufacturing_quantum_circuit` | 输入1 · 高级电路 ×1<br>输入2 · 末地维度转换液 500 mB | 输出1 · 末地电路 ×1 | 300 | 120 |
| `ore_infusion_copper` | 输入1 · #forge:raw_materials/copper ×1<br>输入2 · 氧气 200 mB | 输出1 · 铜矿团 ×3 | 160 | 40 |
| `ore_infusion_gold` | 输入1 · #forge:raw_materials/gold ×1<br>输入2 · 氧气 200 mB | 输出1 · 金矿团 ×3 | 160 | 40 |
| `ore_infusion_iron` | 输入1 · #forge:raw_materials/iron ×1<br>输入2 · 氧气 200 mB | 输出1 · 铁矿团 ×3 | 160 | 40 |
| `ore_nonmetal_crystallize_lapis` | 输入1 · 青金石碎片 ×1<br>输入2 · minecraft:water 200 mB | 输出1 · 青金石矿团 ×1 | 100 | 80 |
| `ore_nonmetal_crystallize_redstone` | 输入1 · 红石碎片 ×1<br>输入2 · minecraft:water 200 mB | 输出1 · 红石矿团 ×1 | 100 | 80 |
| `ore_nonmetal_infusion_diamond` | 输入1 · #forge:ores/diamond ×1<br>输入2 · 液态硫酸 500 mB | 输出1 · 钻石碎片 ×5 | 200 | 80 |
| `ore_nonmetal_infusion_lapis` | 输入1 · #forge:ores/lapis ×1<br>输入2 · 液态硫酸 500 mB | 输出1 · 青金石碎片 ×5 | 200 | 80 |
| `ore_nonmetal_infusion_redstone` | 输入1 · #forge:ores/redstone ×1<br>输入2 · 液态硫酸 500 mB | 输出1 · 红石碎片 ×5 | 200 | 80 |
| `ore_nonmetal_press_diamond` | 输入1 · 钻石碎片 ×1<br>输入2 · minecraft:water 200 mB | 输出1 · 钻石矿团 ×1 | 100 | 80 |
| `ore_nonmetal_wash_diamond` | 输入1 · #forge:ores/diamond ×1<br>输入2 · 氧气 200 mB | 输出1 · 钻石矿团 ×3 | 160 | 40 |
| `ore_nonmetal_wash_lapis` | 输入1 · #forge:ores/lapis ×1<br>输入2 · 氧气 200 mB | 输出1 · 青金石矿团 ×3 | 160 | 40 |
| `ore_nonmetal_wash_redstone` | 输入1 · #forge:ores/redstone ×1<br>输入2 · 氧气 200 mB | 输出1 · 红石矿团 ×3 | 160 | 40 |
| `void_recreate_copper` | 输入1 · #forge:ingots/copper ×1<br>输入2 · 液态虚空 1000 mB | 输出1 · minecraft:copper_ore ×1 | 200 | 240 |
| `void_recreate_gold` | 输入1 · #forge:ingots/gold ×1<br>输入2 · 液态虚空 1000 mB | 输出1 · minecraft:gold_ore ×1 | 200 | 240 |
| `void_recreate_iron` | 输入1 · #forge:ingots/iron ×1<br>输入2 · 液态虚空 1000 mB | 输出1 · minecraft:iron_ore ×1 | 200 | 240 |
| `void_recreate_uranium` | 输入1 · #forge:ingots/uranium ×1<br>输入2 · 液态虚空 1000 mB | 输出1 · 铀矿石 ×1 | 200 | 240 |

## 双相反应釜 · 液相捕收（1条）

机型ID：`alone_mechanical:liquid_capture`。

| 配方ID | 输入 | 输出 | tick | FE/t |
| --- | --- | --- | ---: | ---: |
| `manufacturing_advanced_electrolyte` | 输入1 · 中级电解液 1000 mB<br>输入2 · 氨气 1000 mB | 输出1 · 高级电解液 2000 mB | 200 | 60 |

## 双相反应釜 · 液相合制（7条）

机型ID：`alone_mechanical:liquid_reactor`。

| 配方ID | 输入 | 输出 | tick | FE/t |
| --- | --- | --- | ---: | ---: |
| `dimension_void_amplification` | 输入1 · 液态虚空 250 mB<br>输入2 · minecraft:water 1000 mB | 输出1 · 液态虚空 1000 mB | 200 | 320 |
| `dimension_void_initiation` | 输入1 · minecraft:water 1000 mB<br>输入2 · 末地维度转换液 250 mB | 输出1 · 液态虚空 1000 mB | 200 | 160 |
| `dimension_water_to_lava` | 输入1 · minecraft:water 1000 mB<br>输入2 · 地狱维度转换液 250 mB | 输出1 · minecraft:lava 1000 mB | 100 | 80 |
| `manufacturing_intermediate_electrolyte` | 输入1 · 初级电解液 1000 mB<br>输入2 · 液态硫酸 1000 mB | 输出1 · 中级电解液 2000 mB | 160 | 40 |
| `ore_wash_copper` | 输入1 · 粗铜矿浆 1000 mB<br>输入2 · minecraft:water 1000 mB | 输出1 · 洗选铜矿浆 2000 mB | 160 | 60 |
| `ore_wash_gold` | 输入1 · 粗金矿浆 1000 mB<br>输入2 · minecraft:water 1000 mB | 输出1 · 洗选金矿浆 2000 mB | 160 | 60 |
| `ore_wash_iron` | 输入1 · 粗铁矿浆 1000 mB<br>输入2 · minecraft:water 1000 mB | 输出1 · 洗选铁矿浆 2000 mB | 160 | 60 |

## 核反应堆（6条）

机型ID：`alone_mechanical:nuclear_reactor`。

| 配方ID | 输入 | 输出 | tick | FE/t |
| --- | --- | --- | ---: | ---: |
| `nuclear_enriched_uranium_fuel_heat_carrier` | 输入1 · 浓缩铀燃料 1 mB<br>输入2 · 液态钠 50 mB | 输出1 · 核废料 1 mB<br>输出2 · 高温液态钠 50 mB | 1 | 0 |
| `nuclear_enriched_uranium_fuel_water` | 输入1 · 浓缩铀燃料 1 mB<br>输入2 · minecraft:water 50 mB | 输出1 · 核废料 1 mB<br>输出2 · 蒸汽 50 mB | 1 | 0 |
| `nuclear_mox_fuel_heat_carrier` | 输入1 · MOX燃料 1 mB<br>输入2 · 液态钠 50 mB | 输出1 · 核废料 1 mB<br>输出2 · 高温液态钠 50 mB | 1 | 0 |
| `nuclear_mox_fuel_water` | 输入1 · MOX燃料 1 mB<br>输入2 · minecraft:water 50 mB | 输出1 · 核废料 1 mB<br>输出2 · 蒸汽 50 mB | 1 | 0 |
| `nuclear_thorium_fuel_heat_carrier` | 输入1 · 钍燃料 1 mB<br>输入2 · 液态钠 50 mB | 输出1 · 核废料 1 mB<br>输出2 · 高温液态钠 50 mB | 1 | 0 |
| `nuclear_thorium_fuel_water` | 输入1 · 钍燃料 1 mB<br>输入2 · minecraft:water 50 mB | 输出1 · 核废料 1 mB<br>输出2 · 蒸汽 50 mB | 1 | 0 |

## 三相分离器 · 脱气分离（1条）

机型ID：`alone_mechanical:separator_degassing`。

| 配方ID | 输入 | 输出 | tick | FE/t |
| --- | --- | --- | ---: | ---: |
| `coolant_sodium_electrolysis` | 输入1 · 熔融氯化钠 1000 mB | 输出1 · 液态钠 500 mB<br>输出2 · 氯气 500 mB | 200 | 160 |

## 三相分离器 · 电解分离（1条）

机型ID：`alone_mechanical:separator_electrolysis`。

| 配方ID | 输入 | 输出 | tick | FE/t |
| --- | --- | --- | ---: | ---: |
| `water_electrolysis` | 输入1 · minecraft:water 1000 mB | 输出1 · 氢气 1000 mB<br>输出2 · 氧气 500 mB | 200 | 60 |

## 三相分离器 · 气体分解（1条）

机型ID：`alone_mechanical:separator_gas_fractionation`。

| 配方ID | 输入 | 输出 | tick | FE/t |
| --- | --- | --- | ---: | ---: |
| `deuterium_separation` | 输入1 · 氢气 1000 mB | 输出1 · 氢气 990 mB<br>输出2 · 氘气 10 mB | 200 | 120 |

## 三相分离器 · 流体分解（2条）

机型ID：`alone_mechanical:separator_liquid_fractionation`。

| 配方ID | 输入 | 输出 | tick | FE/t |
| --- | --- | --- | ---: | ---: |
| `heavy_water_separation` | 输入1 · 富集水 1000 mB | 输出1 · minecraft:water 900 mB<br>输出2 · 重水 100 mB | 200 | 60 |
| `ore_acid_recovery` | 输入1 · 酸性母液 1000 mB | 输出1 · 液态硫酸 500 mB<br>输出2 · minecraft:water 500 mB | 160 | 80 |

## 三相分离器 · 液相析固（2条）

机型ID：`alone_mechanical:separator_liquid_precipitation`。

| 配方ID | 输入 | 输出 | tick | FE/t |
| --- | --- | --- | ---: | ---: |
| `fuel_uranium_separation` | 输入1 · 铀矿浆 1000 mB | 输出1 · 浓缩铀化合物 ×1<br>输出2 · 贫化铀粉 ×3 | 240 | 100 |
| `fuel_waste_recovery` | 输入1 · 核废料 1000 mB | 输出1 · 回收燃料颗粒 ×1<br>输出2 · 封装核废物 ×9 | 300 | 120 |

## 三相分离器 · 固液分离（3条）

机型ID：`alone_mechanical:separator_slurry_separation`。

| 配方ID | 输入 | 输出 | tick | FE/t |
| --- | --- | --- | ---: | ---: |
| `ore_separate_copper` | 输入1 · 洗选铜矿浆 2000 mB | 输出1 · 铜晶体 ×5<br>输出2 · 酸性母液 1000 mB | 240 | 100 |
| `ore_separate_gold` | 输入1 · 洗选金矿浆 2000 mB | 输出1 · 金晶体 ×5<br>输出2 · 酸性母液 1000 mB | 240 | 100 |
| `ore_separate_iron` | 输入1 · 洗选铁矿浆 2000 mB | 输出1 · 铁晶体 ×5<br>输出2 · 酸性母液 1000 mB | 240 | 100 |

## 双相反应釜 · 固相熔合（1条）

机型ID：`alone_mechanical:solid_fusion`。

| 配方ID | 输入 | 输出 | tick | FE/t |
| --- | --- | --- | ---: | ---: |
| `fuel_mixed_compound` | 输入1 · 回收燃料颗粒 ×1<br>输入2 · 贫化铀粉 ×3 | 输出1 · 混合燃料化合物 ×1 | 160 | 80 |

## 双相反应釜 · 固转气化（1条）

机型ID：`alone_mechanical:solid_gasification`。

| 配方ID | 输入 | 输出 | tick | FE/t |
| --- | --- | --- | ---: | ---: |
| `sulfur_dioxide` | 输入1 · #forge:dusts/sulfur ×1<br>输入2 · 氧气 1000 mB | 输出1 · 二氧化硫 1000 mB | 100 | 40 |

## 熔炉、高炉与工作台

包括分阶段机器制造、工业材料、工具与教程书配方。熔炼时间按JSON列出，工作台不耗FE；制造阶段见[机器制造与电池](machine-manufacturing.md)。

| 配方ID | 类型 | 输入 | 输出 | 烧炼tick |
| --- | --- | --- | --- | ---: |
| `emergency_battery` | minecraft:crafting_shapeless | minecraft:potato + minecraft:redstone + minecraft:copper_ingot | 应急电池 ×2 | — |
| `engineering_goggles` | minecraft:crafting_shaped | C=minecraft:copper_ingot；G=minecraft:glass_pane；L=minecraft:leather；布局 L L / CGC /  G  | 工程护目镜 ×1 | — |
| `fuel_raw_uranium_blasting` | minecraft:blasting | 粗铀 | 铀锭 | 100 |
| `fuel_raw_uranium_smelting` | minecraft:smelting | 粗铀 | 铀锭 | 200 |
| `fuel_uranium_dust_blasting` | minecraft:blasting | 铀粉 | 铀锭 | 100 |
| `fuel_uranium_dust_smelting` | minecraft:smelting | 铀粉 | 铀锭 | 200 |
| `manufacturing_advanced_battery_casing` | minecraft:crafting_shaped | B=中级电池；C=钻石粉；S=钢板；W=minecraft:gold_ingot；布局  W  / SCS /  B  | 高级空电池壳 ×1 | — |
| `manufacturing_advanced_circuit` | minecraft:crafting_shaped | B=高级电池；C=化工电路；G=minecraft:gold_ingot；Q=石英砂；布局 GQG / CBC / GQG | 高级电路 ×2 | — |
| `manufacturing_air_separation_component` | minecraft:crafting_shaped | B=基础电路；G=钢齿轮；P=minecraft:piston；S=钢板；W=铜线；布局 SGS / WBW / SPS | 空气分离组件 ×1 | — |
| `manufacturing_alloy_component` | minecraft:crafting_shaped | B=基础电路；C=minecraft:copper_ingot；F=minecraft:furnace；I=minecraft:iron_ingot；布局 CFC / IBI / CFC | 合金组件 ×1 | — |
| `manufacturing_basic_battery_casing` | minecraft:crafting_shaped | C=碳粉；I=铁板；W=铜线；布局  W  / ICI /  I  | 初级空电池壳 ×2 | — |
| `manufacturing_basic_circuit` | minecraft:crafting_shaped | C=minecraft:copper_ingot；I=minecraft:iron_ingot；R=minecraft:redstone；布局 CRC / RIR / CRC | 基础电路 ×2 | — |
| `manufacturing_boiler_core` | minecraft:crafting_shaped | B=化工电路；C=minecraft:copper_block；G=钢齿轮；S=钢板；W=铜线；布局 SCS / WBW / SGS | 锅炉组件 ×1 | — |
| `manufacturing_breeding_component` | minecraft:crafting_shaped | B=初级电池；C=化工电路；G=钢齿轮；S=钢板；T=minecraft:golden_carrot；布局 STS / GCG / SBS | 养殖组件 ×1 | — |
| `manufacturing_calcination_component` | minecraft:crafting_shaped | B=中级电池；C=化工电路；F=minecraft:blast_furnace；S=钢板；W=铜线；布局 SFS / WCW / SBS | 煅烧组件 ×1 | — |
| `manufacturing_chemical_circuit` | minecraft:crafting_shaped | B=初级电池；C=基础电路；R=minecraft:redstone；S=钢板；W=铜线；布局 WRW / CBC / WSW | 化工电路 ×2 | — |
| `manufacturing_coal_dust` | minecraft:crafting_shapeless | 碳粉 | 煤粉 ×1 | — |
| `manufacturing_compound_component` | minecraft:crafting_shaped | B=基础电路；G=#forge:glass；S=钢板；W=铜线；布局 SGS / WBW / SGS | 化合组件 ×1 | — |
| `manufacturing_condensation_component` | minecraft:crafting_shaped | B=基础电路；C=minecraft:copper_block；G=#forge:glass；S=钢板；W=铜线；布局 SWS / GBG / SCS | 冷凝组件 ×1 | — |
| `manufacturing_conversion_core` | minecraft:crafting_shaped | B=基础电路；G=#forge:glass；S=钢板；W=铜线；布局 SWS / GBG / SWS | 气液转换组件 ×1 | — |
| `manufacturing_crusher_core` | minecraft:crafting_shaped | B=基础电路；I=minecraft:iron_ingot；P=minecraft:piston；布局 IPI / IBI / IPI | 粉碎组件 ×1 | — |
| `manufacturing_decomposition_component` | minecraft:crafting_shaped | B=基础电路；G=钢齿轮；S=钢板；W=铜线；布局 SGS / WBW / SGS | 分解组件 ×1 | — |
| `manufacturing_distillation_component` | minecraft:crafting_shaped | B=基础电路；F=minecraft:furnace；G=#forge:glass；S=钢板；W=铜线；布局 SWS / GBG / SFS | 蒸馏组件 ×1 | — |
| `manufacturing_efficiency_coil` | minecraft:crafting_shaped | C=化工电路；R=minecraft:redstone_block；W=铜线；布局 WWW / RCR / WWW | 效率线圈 ×1 | — |
| `manufacturing_electric_pump` | minecraft:crafting_shaped | B=基础电路；C=minecraft:copper_ingot；I=minecraft:iron_ingot；P=minecraft:piston；布局 ICI / PBP / III | 电动泵 ×1 | — |
| `manufacturing_energy_coil` | minecraft:crafting_shaped | B=中级电池；C=化工电路；W=铜线；布局 WWW / BCB / WWW | 能量线圈 ×1 | — |
| `manufacturing_energy_matrix` | minecraft:crafting_shaped | B=基础电路；C=minecraft:copper_ingot；I=minecraft:iron_ingot；R=minecraft:redstone_block；布局 IRI / CBC / IRI | 能量矩阵 ×1 | — |
| `manufacturing_engineering_goggles` | minecraft:crafting_shaped | B=基础电路；C=minecraft:copper_ingot；G=#forge:glass；L=minecraft:leather；布局 C C / GBG /  L  | 工程护目镜 ×1 | — |
| `manufacturing_engineering_hammer` | minecraft:crafting_shaped | I=minecraft:iron_ingot；S=minecraft:stick；布局  II /  SI / S   | 工程锤 ×1 | — |
| `manufacturing_exchange_component` | minecraft:crafting_shaped | B=化工电路；C=minecraft:copper_block；S=钢板；W=铜线；布局 SCS / WBW / SCS | 交换组件 ×1 | — |
| `manufacturing_fluid_pipe_1` | minecraft:crafting_shaped | C=赤铜锭；I=minecraft:iron_nugget；M=minecraft:bucket；布局 III / CMC / III | 赤铜流体管道 ×8 | — |
| `manufacturing_fluid_pipe_2` | minecraft:crafting_shaped | E=铜线；M=殷钢锭；P=赤铜流体管道；布局 PPP / MEM / PPP | 殷钢流体管道 ×6 | — |
| `manufacturing_fluid_pipe_3` | minecraft:crafting_shaped | E=化工电路；M=共振锭；P=殷钢流体管道；布局 PPP / MEM / PPP | 共振流体管道 ×6 | — |
| `manufacturing_fluid_pipe_4` | minecraft:crafting_shaped | E=高级电路；M=超导锭；P=共振流体管道；布局 PPP / MEM / PPP | 超导流体管道 ×6 | — |
| `manufacturing_fluid_pipe_5` | minecraft:crafting_shaped | E=末地电路；M=量子锭；P=超导流体管道；布局 PPP / MEM / PPP | 量子流体管道 ×6 | — |
| `manufacturing_fluid_sampling_gun` | minecraft:crafting_shaped | B=minecraft:bucket；C=minecraft:copper_ingot；G=#forge:glass；I=minecraft:iron_ingot；布局  G  / CBC /  I  | 取液枪 ×1 | — |
| `manufacturing_forging_component` | minecraft:crafting_shaped | B=基础电路；I=minecraft:iron_block；P=minecraft:piston；S=钢锭；布局 SPS / IBI / SPS | 锻压组件 ×1 | — |
| `manufacturing_forming_component` | minecraft:crafting_shaped | B=基础电路；C=minecraft:copper_ingot；P=minecraft:piston；S=钢锭；布局 SPS / CBC / SPS | 塑形组件 ×1 | — |
| `manufacturing_gas_pipe_1` | minecraft:crafting_shaped | C=赤铜锭；I=minecraft:iron_nugget；M=minecraft:glass_bottle；布局 III / CMC / III | 赤铜气体管道 ×8 | — |
| `manufacturing_gas_pipe_2` | minecraft:crafting_shaped | E=铜线；M=殷钢锭；P=赤铜气体管道；布局 PPP / MEM / PPP | 殷钢气体管道 ×6 | — |
| `manufacturing_gas_pipe_3` | minecraft:crafting_shaped | E=化工电路；M=共振锭；P=殷钢气体管道；布局 PPP / MEM / PPP | 共振气体管道 ×6 | — |
| `manufacturing_gas_pipe_4` | minecraft:crafting_shaped | E=高级电路；M=超导锭；P=共振气体管道；布局 PPP / MEM / PPP | 超导气体管道 ×6 | — |
| `manufacturing_gas_pipe_5` | minecraft:crafting_shaped | E=末地电路；M=量子锭；P=超导气体管道；布局 PPP / MEM / PPP | 量子气体管道 ×6 | — |
| `manufacturing_harvest_component` | minecraft:crafting_shaped | B=初级电池；C=化工电路；G=钢齿轮；S=钢板；T=minecraft:iron_hoe；布局 STS / GCG / SBS | 收割组件 ×1 | — |
| `manufacturing_industrial_core` | minecraft:crafting_shaped | B=基础电路；C=minecraft:copper_ingot；I=minecraft:iron_ingot；R=minecraft:redstone；布局 IRI / CBC / IRI | 工业核心 ×1 | — |
| `manufacturing_infusion_component` | minecraft:crafting_shaped | B=基础电路；D=minecraft:dispenser；G=钢齿轮；S=钢板；W=铜线；布局 SDS / GBG / SWS | 灌注组件 ×1 | — |
| `manufacturing_intermediate_battery_casing` | minecraft:crafting_shaped | B=化工电路；C=碳粉；S=钢板；W=铜线；布局  W  / SCS /  B  | 中级空电池壳 ×2 | — |
| `manufacturing_item_pipe_1` | minecraft:crafting_shaped | C=赤铜锭；I=minecraft:iron_nugget；M=minecraft:chest；布局 III / CMC / III | 赤铜物品管道 ×8 | — |
| `manufacturing_item_pipe_2` | minecraft:crafting_shaped | E=铜线；M=殷钢锭；P=赤铜物品管道；布局 PPP / MEM / PPP | 殷钢物品管道 ×6 | — |
| `manufacturing_item_pipe_3` | minecraft:crafting_shaped | E=化工电路；M=共振锭；P=殷钢物品管道；布局 PPP / MEM / PPP | 共振物品管道 ×6 | — |
| `manufacturing_item_pipe_4` | minecraft:crafting_shaped | E=高级电路；M=超导锭；P=共振物品管道；布局 PPP / MEM / PPP | 超导物品管道 ×6 | — |
| `manufacturing_item_pipe_5` | minecraft:crafting_shaped | E=末地电路；M=量子锭；P=超导物品管道；布局 PPP / MEM / PPP | 量子物品管道 ×6 | — |
| `manufacturing_item_vault` | minecraft:crafting_shaped | C=minecraft:chest；I=minecraft:iron_ingot；布局 ICI / C C / ICI | 物品仓 ×4 | — |
| `manufacturing_lunar_generator` | minecraft:crafting_shaped | B=中级电池；C=化工电路；E=minecraft:ender_pearl；G=基础光能发电机；S=钢板；W=铜线；布局 EGE / CSC / WBW | 夜光发电机 ×1 | — |
| `manufacturing_machine_casing` | minecraft:crafting_shaped | C=minecraft:copper_ingot；G=#forge:glass；I=minecraft:iron_ingot；布局 III / CGC / III | 机器外壳 ×8 | — |
| `manufacturing_machine_controller` | minecraft:crafting_shaped | B=基础电路；G=#forge:glass；I=minecraft:iron_ingot；R=minecraft:redstone；布局  G  / RBR /  I  | 通用机器控制器 ×1 | — |
| `manufacturing_power_core` | minecraft:crafting_shaped | B=基础电路；C=minecraft:copper_ingot；I=minecraft:iron_ingot；R=minecraft:redstone；布局 ICI / RBR / ICI | 电网核心 ×1 | — |
| `manufacturing_power_pylon` | minecraft:crafting_shaped | B=基础电路；C=minecraft:copper_ingot；I=minecraft:iron_ingot；R=minecraft:redstone；布局 CRC /  I  / IBI | 供电桩 ×2 | — |
| `manufacturing_processing_matrix` | minecraft:crafting_shaped | B=高级电池 · 高功率；C=高级电路；D=minecraft:diamond；G=钢齿轮；布局 GCG / BDB / GCG | 处理矩阵 ×1 | — |
| `manufacturing_reactor_fuel_rod` | minecraft:crafting_shaped | A=高级电路；B=高级电池；S=钢板；U=铀锭；布局 SUS / BAB / SUS | 反应堆燃料棒 ×2 | — |
| `manufacturing_red_copper_compound_blasting` | minecraft:blasting | 赤铜混合料 | 赤铜锭 | 100 |
| `manufacturing_red_copper_compound_smelting` | minecraft:smelting | 赤铜混合料 | 赤铜锭 | 200 |
| `manufacturing_reinforced_casing` | minecraft:crafting_shaped | C=高级电路；O=minecraft:obsidian；S=钢板；布局 SOS / OCO / SOS | 强化外壳 ×4 | — |
| `manufacturing_repeller_component` | minecraft:crafting_shaped | B=初级电池；C=化工电路；G=钢齿轮；S=钢板；T=minecraft:torch；布局 STS / GCG / SBS | 驱逐组件 ×1 | — |
| `manufacturing_signal_pole` | minecraft:crafting_shaped | C=minecraft:copper_ingot；I=minecraft:iron_ingot；R=minecraft:redstone；布局  C  /  I  / RIR | 信号杆 ×4 | — |
| `manufacturing_slaughter_component` | minecraft:crafting_shaped | B=初级电池；C=化工电路；G=钢齿轮；S=钢板；T=minecraft:iron_sword；布局 STS / GCG / SBS | 屠杀组件 ×1 | — |
| `manufacturing_solar_generator` | minecraft:crafting_shaped | C=基础电路；G=#forge:glass；Q=石英砂；S=钢板；W=铜线；布局 GGG / QCQ / SWS | 基础光能发电机 ×1 | — |
| `manufacturing_steel_dust_blasting` | minecraft:blasting | 钢粉 | 钢锭 | 100 |
| `manufacturing_steel_dust_smelting` | minecraft:smelting | 钢粉 | 钢锭 | 200 |
| `manufacturing_storage_tank` | minecraft:crafting_shaped | G=#forge:glass；I=minecraft:iron_ingot；布局 IGI / G G / IGI | 储罐 ×4 | — |
| `manufacturing_structural_glass` | minecraft:crafting_shaped | G=#forge:glass；I=minecraft:iron_nugget；布局 GIG / IGI / GIG | 结构玻璃 ×8 | — |
| `manufacturing_thermal_generator` | minecraft:crafting_shaped | B=基础电路；C=minecraft:copper_ingot；F=minecraft:furnace；I=minecraft:iron_ingot；R=minecraft:redstone；布局 IFI / CBC / IRI | 热能发电机 ×1 | — |
| `manufacturing_turbine_core` | minecraft:crafting_shaped | B=中级电池；C=化工电路；G=钢齿轮；S=钢板；W=铜线；布局 SGS / WCW / SBS | 涡轮组件 ×1 | — |
| `manufacturing_turbine_rotor` | minecraft:crafting_shaped | G=钢齿轮；S=钢板；布局  S  / SGS /  S  | 涡轮转子 ×1 | — |
| `manufacturing_universal_port` | minecraft:crafting_shaped | C=minecraft:copper_ingot；H=minecraft:hopper；I=minecraft:iron_ingot；布局 ICI / CHC / ICI | 通用输入输出口 ×4 | — |
| `manufacturing_wrench` | minecraft:crafting_shaped | I=minecraft:iron_ingot；布局 I I /  I  /  I  | 机械扳手 ×1 | — |
| `ore_blasting_copper_dust` | minecraft:blasting | 铜粉 | minecraft:copper_ingot | 100 |
| `ore_blasting_gold_dust` | minecraft:blasting | 金粉 | minecraft:gold_ingot | 100 |
| `ore_blasting_iron_dust` | minecraft:blasting | 铁粉 | minecraft:iron_ingot | 100 |
| `ore_smelting_copper_dust` | minecraft:smelting | 铜粉 | minecraft:copper_ingot | 200 |
| `ore_smelting_gold_dust` | minecraft:smelting | 金粉 | minecraft:gold_ingot | 200 |
| `ore_smelting_iron_dust` | minecraft:smelting | 铁粉 | minecraft:iron_ingot | 200 |
| `steel_dust_mixing` | minecraft:crafting_shapeless | 铁粉 + 煤粉 | 钢粉 ×1 | — |
| `tutorial_book` | minecraft:crafting_shapeless | minecraft:book + minecraft:iron_nugget | 教程书 ×1 | — |
