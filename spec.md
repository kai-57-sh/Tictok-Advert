# Technical Spec: AI 广告推荐信息流 Android App (Tictok Advert)

## 1. 目标

项目目标是交付一个可演示、可本地运行、可离线加载素材的 Android 广告信息流应用，覆盖以下能力：

- 本地持久化广告数据与埋点数据
- 多卡片样式混排与单实例视频播放
- 对话式广告搜索
- 离线广告包导入与版本更新

## 2. 当前技术方案

### 2.1 数据层

- `Room` 负责保存 `ads` 与 `analytics_events` 两张表。
- 应用启动时，`TictokAdvertRepository.populateAdsIfNeeded()` 会检查离线广告包版本。
- `OfflineAdBundleLoader` 从 `app/src/main/assets/offline_ads/manifest.json` 读取广告清单，并把封面/视频复制到 `filesDir/offline_ads/` 供运行时使用。
- 若 `manifest.generatedAt` 与已导入版本不同，仓库会清空旧广告和旧统计，再导入新的 bundle。

### 2.2 素材来源

- 当前仓库内置 100 条中文广告素材。
- 素材来源为今日头条搜索结果，生成脚本位于 `scripts/fetch_toutiao_ads.py`。
- 脚本会尝试为部分关键词下载对应视频，因此视频卡片数不是硬编码常量，而由可用视频源决定。

### 2.3 UI 与播放

- 首页使用 `LazyColumn` 混排大图、小图和视频卡片。
- Feed 内最多只允许一个视频卡片处于播放状态。
- 详情页会复用 `AdvertViewModel` 中保存的视频进度，实现详情页续播。
- 封面优先使用本地缓存文件，找不到时再回退到原始远程 URL。

### 2.4 搜索能力

- `QwenSearchHelper` 通过 ModelScope 的 `Qwen/Qwen3-32B` 接口把自然语言查询解析为 `channel + tags + explanation`。
- 通过 `extra_body.enable_thinking=false` 关闭思考输出，优先保证搜索延迟和 JSON 输出稳定性。
- 如果 API key 缺失、调用失败或返回异常，会自动使用本地关键词规则解析。
- 搜索结果优先按 AI 意图过滤，若完全无结果，再回退到标题/描述/广告主名称的文本匹配。

### 2.5 统计能力

- 曝光、点击、点赞、收藏、分享都写入 `analytics_events`。
- Stats 页面基于 Room Flow 展示曝光、点击和交互汇总。
- `resetStats()` 会清空埋点并重置广告上的行为状态。

## 3. 架构关系

```text
MainActivity
  -> AppContainer
  -> TictokAdvertApp
     -> HomeScreen / DetailsScreen / SearchScreen / StatsScreen
        -> AdvertViewModel
           -> TictokAdvertRepository
              -> OfflineAdBundleLoader
              -> AppDatabase (Room)
              -> QwenSearchHelper
```

## 4. 关键实现约束

- 离线广告包至少需要 100 条广告，否则导入失败并回退到仓库内的 fallback 生成数据。
- 本地素材路径和远程素材 URL 都保存在 `AdEntity` 中，UI 统一通过实体方法解析实际展示/播放源。
- 视频下载属于补充能力，只有本地视频缺失且 `videoUrl` 可用时才会触发后台下载。

## 5. 维护入口

- 更新广告数据：`python3 scripts/fetch_toutiao_ads.py`
- 调试数据导入：检查 `manifest.generatedAt`、`offline_ad_bundle_version`
- 构建调试包：`./gradlew :app:assembleDebug`
