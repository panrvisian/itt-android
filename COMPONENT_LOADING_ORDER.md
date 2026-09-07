# ITT 组件加载顺序与性能约束

> 本文是启动、主页面导航和延迟计算的维护规范。修改 Splash、`AppRoot`、Pager、页面数据投影或底栏前必须先阅读。若代码行为与本文不一致，应在同一个提交中修正代码或更新本文并说明性能影响。

## 一、设计目标

ITT 的主页面需要同时满足以下目标：

1. Splash 不使用固定等待时长，设备越快越早进入首页。
2. 首页第一次出现时已经具备真实数据，不先显示一帧默认空状态。
3. 首页到任意远端 Tab 都有已组合的页面外壳，不在切页动画中临时创建整页。
4. 时间轴、备注和统计的全历史扫描、排序、布局或聚合不参与启动，也不在 Pager 移动期间运行。
5. 设置主页可以提前完整组合；设置二级页只在用户点击对应入口后组合。
6. CPU 密集型计算在 `Dispatchers.Default` 执行，数据库与文件 I/O 继续由 Repository 隔离到 I/O 线程。

这里的“外壳”是已经可以参与 Pager 绘制的稳定 UI，包括页面背景、顶部控件、卡片结构和必要的空表格，但不包括记录投影、统计聚合、全历史排序和图片解码。

## 二、启动状态机

```text
系统创建 MainActivity
        │
        ├─ 显示系统 Splash（黑底 + splash_icon 产品标志）
        │
        ├─ ViewModel 等待首页必需数据首次真实发射
        │    ├─ groups
        │    ├─ events
        │    ├─ records
        │    └─ settings
        │
        ├─ Pager radius = 0：只组合完整首页
        │
        ├─ 按帧扩大 beyondViewportPageCount
        │    ├─ radius 1：时间轴控件与空表格
        │    ├─ radius 2：备注日期控件与列表外壳
        │    ├─ radius 3：统计控件、汇总卡片与图表外壳
        │    └─ radius 4：完整设置主页
        │
        ├─ 再等待一个 Compose 帧
        │
        └─ onStartupContentReady → 系统 Splash 自动退出
```

关键实现：

- `MainActivity.startupContentReady` 是系统 Splash 的唯一退出开关。
- `SplashScreen.setKeepOnScreenCondition` 只读取这个轻量布尔值，回调内禁止数据库查询、Compose 状态计算或阻塞操作。
- `MainViewModel.homeContentReady` 只有在四个首页数据源都产生真实首个值后才为 `true`。`stateIn` 的默认空列表和默认设置不能被当作真实加载完成。
- `AppRoot.preloadedPageRadius` 必须逐帧从 0 增加到 4，禁止在第一次组合时直接恢复为固定 `beyondViewportPageCount = 4`。
- 启动流程不能新增 `delay(900)`、`delay(1000)` 一类固定最短等待。允许等待数据或下一帧，因为它们反映真实设备性能和渲染进度。
- Splash 退出后 `beyondViewportPageCount` 保持为 4，让五个主页面外壳继续驻留，确保首页直接跳设置等远距离切换不会临时创建页面。

`noteImages` 当前仍由 ViewModel 提前订阅，但它不属于首页就绪条件；文字/图片备注索引不会在启动阶段构建。

## 三、Pager 状态的职责

Compose Pager 的三个状态不可混用：

| 状态 | 用途 | 禁止用途 |
| --- | --- | --- |
| `targetPage` | 让底栏选中态及时跟随手势目标 | 启动页面重计算 |
| `currentPage` | 判断当前物理位置、边界回弹和引导定位 | 判断重计算是否可以开始 |
| `settledPage` | 页面完全停稳后生成 `contentActive` | 无 |

所有延迟计算统一使用：

```kotlin
val contentActive = startupPreloadComplete && pagerState.settledPage == page
```

禁止改为 `currentPage == page` 或 `targetPage == page`。否则计算会在用户手指仍然移动、跨页动画尚未结束时抢占 CPU，重新造成动画不跟手。

底栏点击只能启动一次 `animateScrollToPage`。`selectedTab` 用于底栏状态，不能再额外通过另一个 `LaunchedEffect(selectedTab)` 重复启动相同 Pager 动画。

## 四、五个主页面的加载策略

| 页面 | Splash 期间 | 页面停稳后 | 离开页面后 |
| --- | --- | --- | --- |
| 首页 | 完整组合真实内容 | 正常响应记录与设置更新 | 保持驻留 |
| 时间轴 | 日期控件、说明、比例视图空表格 | 后台裁剪当天记录、排序并计算重叠轨道 | 停止时钟；保留有效结果直到数据源变化 |
| 备注 | 日期控件和列表外壳 | 后台构建备注索引，再后台筛选当天记录 | 保留有效投影；数据源变化后失效 |
| 统计 | 范围控件、汇总卡片和图表/排行外壳 | 只后台计算当前选中的范围 | 保留当前有效结果；范围或数据变化后失效 |
| 设置 | 完整设置主页 | 无额外主页计算 | 二级页面关闭后从组合中移除 |

### 1. 首页

首页是唯一允许参与首屏准备的完整业务页。它需要：

- 可见分组和事件；
- 事件频率计数与排序；
- 进行中记录；
- 当前设置和主题。

以后若记录量增长导致首页首帧变慢，应优先把“事件频率计数”“进行中记录”改为 DAO 专用查询或持久化摘要，不要通过延长 Splash 时间掩盖问题。

### 2. 时间轴

- 外壳预加载时必须向 `TimelineDayView` 提供空记录列表，保留完整小时表格。
- `buildTimelineItems` 只能在 `contentActive == true` 时调用，并在 `Dispatchers.Default` 执行。
- 当天记录、日期或备注标记变化时使缓存失效并重新计算。
- 每秒时钟仅在时间轴处于活动状态且存在进行中记录时启动。
- 页面离开后必须停止时钟，禁止后台每秒扫描所有记录。

### 3. 备注

- `NoteProjection` 包含图片记录 ID、全部备注记录 ID 和按时间倒序的备注记录。
- 只有时间轴或备注页停稳后才允许构建备注投影，因为时间轴也需要备注星号。
- 投影构建和按日期筛选都在 `Dispatchers.Default` 执行。
- 图片 Bitmap 继续只在查看/编辑具体备注时解码，禁止在备注列表或启动阶段预解码全部图片。
- 首页记录详情判断是否存在图片备注时可以在用户点击后查询当前内存列表；不能为了这个点击路径把完整备注页面提前渲染。

### 4. 统计

- `StatsRangeKind.Today` 是进程启动后的默认范围。
- Splash 期间不调用 `StatsCalculator.compute`。
- 第一次进入统计页只计算当前“天”范围。
- 周、月、学期只在用户点击对应标签后计算，禁止一次进入统计页就预计算四种范围。
- `StatsCalculator.compute` 必须在 `Dispatchers.Default` 执行。
- 当前实现只缓存最后一个仍有效的计算结果；切换到另一个范围后旧范围可以重新计算，不应为了命中缓存而常驻四份大结果。
- 记录列表、事件列表、范围边界变化时旧结果立即视为无效；页面不在前台时等待下次进入再计算。

### 5. 设置

- `SettingsMainScreen` 在 Splash 的最后一个预组合阶段完整创建。
- Appearance、Behavior、HomeDisplay、Statistics、Semester、Data 等二级页面不能随设置主页一起预创建。
- 只有 `activeSettingsPage != Main` 时，才在全屏覆盖层中组合对应二级页面。
- 返回或切换到其他主 Tab 时恢复 `SettingsPage.Main`，释放二级页面组合。

## 五、液态玻璃底栏约束

- 底栏必须跟随 `targetPage` 更新视觉选中态，但页面业务计算必须等 `settledPage`。
- 重力传感器只允许影响绘制层。当前重力方向按 3° 量化，通过 `State<Highlight>` 在 draw 阶段读取，避免传感器噪声重组整个导航栏。
- 禁止在每个高光分别注册重力传感器；基础高光和胶囊高光共享同一个量化角度状态。
- AGSL、Backdrop 和阴影效果的预热依赖外壳按帧组合，不应通过固定 Splash 延时“猜测”预热完成时间。

## 六、线程与缓存规则

| 工作类型 | 执行位置 |
| --- | --- |
| Compose 状态读取、轻量 UI 映射 | Main |
| 全历史筛选、排序、时间轴轨道计算 | `Dispatchers.Default` |
| 统计聚合 | `Dispatchers.Default` |
| Room 事务、导入导出、图片文件操作 | Repository / `Dispatchers.IO` |
| 图片解码 | 现有按需后台加载路径 |

缓存必须同时满足：

1. 缓存结果与源数据或计算 key 绑定。
2. 源数据变化后不能继续显示为有效结果。
3. 页面未活动时不主动重建已失效缓存。
4. 页面重新活动且数据未变化时允许立即复用结果。
5. 后台计算完成前显示稳定外壳或“正在加载”，不能短暂显示错误的“没有记录”。

## 七、修改检查清单

修改主页面或新增 Tab 时逐项确认：

- [ ] 首页首帧没有新增非首页全历史计算。
- [ ] 新页面提供不依赖业务计算的稳定外壳。
- [ ] 外壳按帧加入预组合顺序，没有一次性扩大预加载半径。
- [ ] 重计算由 `settledPage` 对应的 `contentActive` 触发。
- [ ] CPU 密集工作不在 Composable 的 `remember { ... }` 中同步执行。
- [ ] 切页动画只由一个协程驱动。
- [ ] 不用固定延迟控制 Splash 退出。
- [ ] 设置二级页面没有被批量预创建。
- [ ] 离开时间轴后没有继续每秒扫描记录。
- [ ] 统计没有自动计算未选择的范围。
- [ ] 数据变化时缓存正确失效。
- [ ] 已执行编译、Lint、冷启动和首页到设置的远距离跳转验证。

## 八、回归验证

提交前至少执行：

```bash
./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :app:assembleDebug --no-daemon
./gradlew :app:lintDebug --no-daemon
```

有可用模拟器或真机时验证：

1. 强制停止应用并冷启动，确认产品标志 Splash 会自动退出。
2. 首次安装和已有数据两种情况都不能卡在 Splash。
3. Splash 退出后立即滚动首页，检查前三秒是否跟手。
4. 从首页直接点击设置，确认中间页面不会闪白、空白或临时创建导致停顿。
5. 依次进入时间轴、备注、统计，确认先有外壳，再出现正确数据。
6. 统计默认只加载“天”；依次点击周、月、学期，确认对应数据按需出现。
7. 离开时间轴后观察 CPU/帧数据，确认没有持续的每秒页面重算。
8. 使用 `dumpsys gfxinfo` 或 Perfetto 比较修改前后的慢帧、主线程和 CPU 热点。

建议的基础帧检查：

```bash
adb shell dumpsys gfxinfo com.bigbrother.mobile reset
# 执行冷启动、滚动和跨页操作
adb shell dumpsys gfxinfo com.bigbrother.mobile
```

模拟器的软件渲染数据只能用于发现明显回归；最终性能结论应以目标真机的 Perfetto 或 FrameTimeline 数据为准。

## 九、相关代码

- `app/src/main/java/com/bigbrother/mobile/ui/MainActivity.kt`：系统 Splash 生命周期。
- `app/src/main/java/com/bigbrother/mobile/ui/AppViewModel.kt`：首页数据就绪状态。
- `app/src/main/java/com/bigbrother/mobile/ui/AppRoot.kt`：按帧预组合、Pager 激活条件、时间轴和统计计算。
- `app/src/main/java/com/bigbrother/mobile/ui/NoteScreens.kt`：备注当日列表延迟计算。
- `app/src/main/java/com/bigbrother/mobile/ui/MiuixLiquidGlassNavigationBar.kt`：底栏动画与绘制态传感器高光。
- `app/src/main/res/values/themes.xml`：系统 Splash 背景、产品标志和退出后主题。
