# 铀矿自然生成

初始铀可通过主世界采矿获得，接入既有粗铀、铀粉、铀锭和液态核燃料加工线。

| 参数 | 默认值 |
| --- | --- |
| 群系范围 | `#minecraft:is_overworld`，不加入下界和末地 |
| 生成阶段 | `underground_ores` |
| 每区块矿脉尝试次数 | 8 |
| 矿脉尺寸 | 8 |
| 生成高度 | Y=-64～64，三角分布，Y=0附近最集中 |
| 石质地层 | `minecraft:stone_ore_replaceables` → 铀矿 |
| 深板岩地层 | `minecraft:deepslate_ore_replaceables` → 深层铀矿 |
| 洞穴暴露额外丢弃概率 | 0 |

丰度定位为比铁少、比普通主世界群系的金多。原版普通金矿每区块常规尝试4次、深层额外0～1次，铁矿包含中层10次、小矿脉10次及高山矿脉；铀矿采用中间的尝试次数。恶地额外金矿保持原版规则，因此此比较不适用于恶地。矿脉尝试次数和尺寸是生成参数，实际矿石数量还受高度、岩层、地形与矿脉重叠影响，每区块不保证固定产量。

已有存档需探索新生成的区块；不会向已经生成的区块补刷矿石。铁镐及以上工具可获得粗铀，精准采集保留对应矿石，时运影响粗铀产量。

## 资源与数据包

可通过数据包覆盖以下文件调整频率、尺寸、高度或群系：

- `data/alone_mechanical/worldgen/configured_feature/ore_uranium.json`：矿脉尺寸和替换规则。
- `data/alone_mechanical/worldgen/placed_feature/ore_uranium.json`：尝试次数、高度分布与区块位置。
- `data/alone_mechanical/forge/biome_modifier/add_uranium_ore.json`：主世界群系注入。

资源生成脚本为`tools/generate_uranium_worldgen.ps1`，已接入完整资源生成及帮助更新；同时更新中英文矿石提示与核燃料教程。

## 验证

`tools/uranium-worldgen-tests.gradle`运行5项铀矿测试及相关核能回归，共40项服务端GameTest。覆盖动态注册表加载、全部群系注入范围、16000次放置位置采样、普通石层 / 深板岩实际矿脉生成、下界岩不替换，以及采掘掉落与精准采集。

```powershell
./gradlew.bat --offline -I tools/uranium-worldgen-tests.gradle runGameTestServer build
```
