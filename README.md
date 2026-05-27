# AI 广告推荐信息流 Android App (Tictok Advert)

Tictok Advert 是一款结合本地 Room 数据存储和 **Gemini 3.5 Flash** 大语言模型意图解析的高品质单列广告信息流展示型 Android 客户端。本产品以优异的 Material 3 动效、响应式自适应布局技术，为您展示高完成度、更具人文关怀的广告理解和按需检索工具。

---

## 🔬 AI 声明 (AI Declaration)

我们认为，优质的广告不应仅依靠震撼的封面去赌点击，而是让用户可以通过兴趣精准匹配：
1. **真实 AI 调用**：本应用集成 Gemini 3.5 Flash API，当用户在 AI 搜广告助手内输入“适合打工人的平价降噪耳机”等任意口语时，模型会解析为频道与标签对。
2. **零中断优雅降级**：若未配置密钥或网络请求失败，算法会自适应启用本地的高精准 regex 关键词受控词引擎，产出同等优秀的条件解释与匹配体验。

---

## 🏃 运行方式 (How to Run)

### 1. 配置密钥 (Secrets)
若要体验大模型的全真意图检索：
- 请在 **AI Studio 侧边栏的 Secrets 选项卡** 录入名为 `GEMINI_API_KEY` 的秘钥对（该值已在 `.env.example` 预定义，平台在编译时将借助 Secrets Gradle Plugin 自动将其生成为 `BuildConfig.GEMINI_API_KEY` 注入。切勿手动在 local.properties 填写）。
- 如果没有填写密钥，系统亦会完美启用退化匹配进行流程演示，不会导致任何异常报错。

### 2. 模拟器运行
- 点击界面顶端的运行预览加载 Streaming Android 模拟器，编译完成后应用将自动拉起。
- 鼠标点击或左右滑动即等同于手机触屏，即可开始体验高清晰度的极速切换。

---

## 🧩 模块划分 (Module boundary)

```
com.example
├── MainActivity.kt                      # 主启动入口文件 (绑定 edge-to-edge 与 locator 初始化)
├── data                                 # 数据仓储及底层模型服务层 (Data layer)
│   ├── AdEntity.kt                      # Room 实体表设计 (ads & analytics_events)
│   ├── AdDao.kt                         # 数据访问通道 (聚合流、单条修改及埋点统计写入)
│   ├── AppDatabase.kt                   # 数据库配置容器 (Destructive fallback 装载)
│   ├── AppContainer.kt                  # 服务定位单例 (Service locator for memory repository)
│   ├── TictokAdvertRepository.kt        # 仓库大动脉 (承载曝光、点击、各种互动状态持久更新)
│   └── GeminiHelper.kt                  # 大模型网络引擎 (OkHttp Direct REST 及 Local regex fallback)
└── ui                                   # 表示层及 Compose 精美界面组 (Presentation layer)
    ├── theme                            # 主题库
    │   ├── Color.kt                     # 梦幻数码 Slate Dark 调色方案
    │   ├── Theme.kt                     # Compose主题绑扎
    │   └── Type.kt                      # 格式化和排版字体对
    ├── AdvertViewModel.kt               # 全局状态大动脉 (数据绑定、视频进度、AI 实时同步)
    ├── TictokAdvertApp.kt               # 路由调度核心 (Navigation scaffold)
    ├── HomeScreen.kt                    # 主信息流界面 (大图、小图、视频列表卡、LaunchedEffectPV监控)
    ├── DetailsScreen.kt                 # 卡片内容详情 (自动续播 progress continuation、交互面板)
    ├── SearchScreen.kt                  # AI 检索聊天搜索框 (解析意图结果与 matches 渲染)
    └── StatsScreen.kt                   # 指标效能看板 (CTR 进度条、实时埋点监控流、重置键)
```

---

## 🎨 开发规范 (Development & Styling Guidelines)

为了保证工业级高敏演示和卓越性能，产品开发严格恪守以下规范原则：
- **Edge-to-Edge**：主界面及 Scaffolds 全面注入并继承 `enableEdgeToEdge()` 和 `WindowInsets.safeDrawing`，文字、按键、顶端图标绝不因状态栏或底端导航药丸发生遮盖裁剪。
- **互动友好 (Minimum Touch Target)**：详情页面的 Likes/Stars 互动图标均满足大于或是等于双足的 **48.dp x 48.dp** 极佳操作间距，有效减少错位误点率。
- **自动化支持 (Compose TestTags)**：所有关键点位均挂载 `testTag("...")`，包括 `search_text_input`、`back_button`、`search_submit_button` 等，便于自动化回归。
