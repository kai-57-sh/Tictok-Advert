# AI 广告推荐信息流 Android App (Tictok Advert)

## 项目介绍

Tictok Advert 是一个基于 Jetpack Compose 的单列广告信息流 Demo。应用默认从本地离线广告包加载 100 条中文广告，支持频道切换、下拉刷新、详情评论、互动统计和对话式搜索。

- 信息流数据来自 `app/src/main/assets/offline_ads/`
- 当前内置 100 条中文广告，来源为“今日头条搜索”
- 广告类型覆盖大图、小图、视频
- 首页支持点击 Tab 和左右滑动切换 `精选 / 电商 / 本地`
- 首页支持下拉刷新当前频道，并立即回到刷新后的列表顶部
- 标签栏固定一行展示 `全部 + 最多 3 个热门标签 + 更多下拉`
- 详情页支持本地持久化评论
- 数据写入使用 Room + Repository
- 搜索能力为 ModelScope `Qwen/Qwen3-32B` + 本地关键词兜底

配套文档：

- [技术设计文档](docs/AI广告推荐信息流-技术设计文档.md)
- [性能数据对比](docs/AI广告推荐信息流-性能数据对比.md)
- [曝光统计口径验证](docs/AI广告推荐信息流-曝光统计口径验证.md)
- [端侧模型 Demo](docs/AI广告推荐信息流-端侧模型demo.md)
- [测试用例](docs/AI广告推荐信息流-测试用例.md)
- [日报](docs/客户端工程训练营-日报提交-（吴琦）.md)
- [学习总结](docs/AI广告推荐信息流-学习总结.md)

> 仓库不包含演示视频，按结题要求单独提交。

## 如何运行

1. 配置 Android SDK。
   - Windows 环境请在 `local.properties` 中设置有效的 `sdk.dir`。
2. 如需启用真实语义搜索，请提供 `MODELSCOPE_API_KEY`。
3. 构建调试包。

```bash
# Windows
.\gradlew.bat :app:assembleDebug

# macOS / Linux
./gradlew :app:assembleDebug
```

4. 如需刷新中文广告离线包，运行：

```bash
python3 scripts/fetch_toutiao_ads.py
```

5. 离线包更新后请彻底关闭并重新打开 App，触发版本比对和重导入。

## 模块划分

| 路径 | 说明 |
| --- | --- |
| `app/src/main/java/com/example/data` | Room 表结构、仓库层、离线包加载、Qwen 搜索解析 |
| `app/src/main/java/com/example/ui` | 首页、详情页、搜索页、统计页和 ViewModel |
| `app/src/test/java/com/example/ui` | 首页频道切换映射等单元测试 |
| `app/src/main/assets/offline_ads` | 100 条中文广告的 manifest、图片和视频资源 |
| `scripts` | 今日头条素材抓取与离线包生成脚本 |
| `docs` | 结题文档、加分材料、日报 |

## 开发规范

- UI 层只负责展示和事件转发，数据写入统一通过 Repository。
- 广告媒体路径统一通过 `AdEntity` 的辅助方法访问，避免页面层重复拼接。
- 更新离线素材时必须同步更新 `manifest.json` 的 `generatedAt`。
- 首页刷新只重排当前频道的展示顺序，刷新完成后列表应回到顶部。
- 评论数据统一通过 Room 持久化，详情页只消费 `comments` 数据流。
- 搜索结果必须保持结构化输出；若 API 失败，必须走本地 fallback。
- 统计口径统一落到 `analytics_events` 表，避免重复定义埋点含义。
- 提交前至少做一次基础构建或测试检查。

## AI 声明

本项目使用 AI 辅助完成了部分代码重构建议、搜索 prompt 设计、离线广告文案整理和文档草稿。最终实现均经过人工复核，重点检查了中文素材准确性、路径可用性、搜索兜底行为和统计口径一致性。
