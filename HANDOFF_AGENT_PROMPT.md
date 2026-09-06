# ITT 安卓应用交接说明

> 最近更新：2026-09-05。本文只描述当前有效状态，不作为版本更新日志。

## 一、项目快照

- 项目路径：`F:\Projects\itt-android`
- 应用：ITT（Individual Time Trial）
- 技术栈：Kotlin、Jetpack Compose Material3、MiuiX、Room、DataStore、ViewModel + Repository
- Android 配置：applicationId `com.bigbrother.mobile`，minSdk 31，targetSdk 37，compileSdk 37
- 当前正式版本：`versionName = "2.11"`，`versionCode = 16`
- 当前开发分支：`ui`，基于 `main` 的 `78a1963`
- UI 改动只在 `ui` 分支提交，完成阶段性验收后再通过 Pull Request 合入 `main`
- `origin`：`git@github.com:panrvisian/itt-android.git`
- GitHub 仓库为公开仓库；当前正式标签和 Release 为 `v2.11`
- 当前分支在 `v2.11` 正式版本之后继续维护；开始工作前先查看 `git status`、`git log -1` 和 `git diff`，不要覆盖已有修改。

当前正式发布文件：

- APK：`ITT-v2.11-build-20260829-2240-debug.apk`
- 大小：27,091,795 字节
- SHA-256：`BF3690B2008C628F9FB3687351F741F4AC61F8E603318E25A38C9B8BE2F83AE3`

## 二、构建环境

- JDK：要求 JDK 21，由当前主机的 `JAVA_HOME` 指向实际安装目录
- Android SDK：由当前主机的 `ANDROID_SDK_ROOT` 或 `ANDROID_HOME` 指定；也可由 `local.properties` 提供本机路径
- Gradle Wrapper：9.7.1
- Android Gradle Plugin：9.3.2；Kotlin：2.4.10；KSP：2.3.10；Kotlin/JVM 目标：21
- Compose BOM：2026.08.00；Compose Compiler 使用 Kotlin Compose 插件 2.4.10

常用命令：

```powershell
# Replace these examples with paths on the current host when needed.
$env:JAVA_HOME = 'C:\Path\To\jdk-21'
$env:ANDROID_SDK_ROOT = 'C:\Path\To\Android\Sdk'
$env:ANDROID_HOME = $env:ANDROID_SDK_ROOT
$env:Path = "$env:JAVA_HOME\bin;$env:ANDROID_SDK_ROOT\platform-tools;$env:Path"
Set-Location '<repository>\itt-android'
.\gradlew.bat :app:compileDebugKotlin --no-daemon
.\gradlew.bat :app:assembleDebug --no-daemon
```

SDK Manager 必须安装：Android SDK Platform 37.0、Android SDK Build Tools 37.0.0、Android SDK Platform-Tools。命令行可执行：

```powershell
& "$env:ANDROID_SDK_ROOT\cmdline-tools\latest\bin\sdkmanager.bat" `
  --sdk_root="$env:ANDROID_SDK_ROOT" `
  'platforms;android-37.0' 'build-tools;37.0.0' 'platform-tools'
```

构建产物：

- Gradle Debug APK：`app\build\outputs\apk\debug\app-debug.apk`
- 需要交付时复制到项目根目录，命名为 `ITT-v<版本>-build-yyyyMMdd-HHmm-debug.apk`

环境注意事项：

- `auto_install/common.ps1` 统一解析 JDK 和 Android SDK；其他主机按 `AUTO_INSTALL.md` 配置环境变量即可。
- `local.properties`、构建产物和 APK 不提交到 Git。

## 三、代码与数据结构

### 1. 关键文件

- `app/build.gradle.kts`：Android、版本和依赖配置
- `app/src/main/java/com/bigbrother/mobile/data/AppModels.kt`：分组、事件、记录、备注图片和设置模型
- `data/AppDatabase.kt`：Room 数据库、DAO 和迁移
- `data/AppRepository.kt`：记录 CRUD、跨天切分、克隆、备注、草稿和备份导入导出
- `data/NoteDraftStore.kt`：备注草稿 DataStore
- `data/CsvCodec.kt`：CSV 编解码及旧备份兼容
- `data/SettingsStore.kt`：应用设置 DataStore
- `domain/StatsCalculator.kt`：统计范围、事件统计和分组时长统计
- `ui/AppViewModel.kt`：界面状态及 Repository 调用入口
- `ui/AppRoot.kt`：首页、时间轴、统计、设置和主要弹窗
- `ui/AppBottomBar.kt`：统一使用 MiuiX 固态导航栏或官方示例适配的悬浮液态玻璃导航栏
- `ui/Miuix*.kt`：基于 Apache-2.0 的 compose-miuix-ui `v0.9.2` / AndroidLiquidGlass 示例，提供底栏阻尼拖动、内容遮罩、Backdrop、折射、色散和高光
- `ui/NoteScreens.kt`：备注列表、查看和编辑界面
- `ui/AppComponents.kt`：通用卡片、长按事件块和选择控件
- `ui/Theme.kt`：主题、字号和全局圆角；`ui/MainActivity.kt`：应用入口
- `widget/`：1×1 和 4×2 桌面小组件
- `AndroidManifest.xml` 与 `res/xml/file_paths.xml`：拍照备注使用的 FileProvider
- `auto_install/common.ps1`、`auto_install/deploy.ps1`、`auto_install/menu.ps1`、`auto_install/wifi-adb.ps1`：跨主机工具解析和自动部署
- `README.md`、`README.en.md`、`BUILD_APK.md`、`AUTO_INSTALL.md`：协作者使用说明

### 2. Room 与备注文件

- Room 当前版本为 2，数据库文件名为 `big_brother_mobile.db`。
- `AppContainer` 注册 `MIGRATION_1_2`，同时保留 `fallbackToDestructiveMigration()`；以后升级数据库时必须优先补充明确迁移，避免意外清空用户数据。
- `MIGRATION_1_2` 为 `records` 增加非空 `noteText` 字段，并创建 `note_images` 表。
- `NoteImageEntity` 只保存 recordId、文件名和排序；图片二进制不写入数据库。
- `RecordEntity` 保存事件和分组名称、分组 ID、颜色快照；重命名事件、重命名分组、修改分组颜色或移动事件时，Repository 会同步已有记录快照。
- 已保存图片目录：`filesDir/notes/<recordId>/<fileName>`
- 草稿图片目录：`filesDir/notes_draft/<recordId>/<fileName>`
- 正式保存备注时，草稿图片复制到正式目录，重建该记录的 `note_images`，随后清除草稿状态和草稿目录。
- 删除记录时同时删除该记录的备注图片、草稿状态和相关文件。

### 3. 备份格式

- 导出文件为 zip，固定包含 `big_brother_mobile.csv`，并按 `notes/<recordId>/<fileName>` 保存存在的备注图片。
- CSV 的 `[RECORDS]` 段包含第 10 列 `note`，并包含 `[NOTE_IMAGES]` 段：`record_id,file_name,sort_order`。
- CSV 解析器支持引号内逗号、换行和转义引号。
- 导入支持 zip 和旧纯 CSV，并提供“导入覆盖”和“导入合并”。
- 写入 zip 内图片时必须保留 canonical path 校验，防止文件写出应用文件目录。

## 四、当前功能规则

### 1. 首页、分组与记录

- 首页事件按记录频率降序排列；同频率时再按收藏、手动顺序和名称排列。
- 普通开始、补录和克隆产生的记录都参与频率统计。
- 跨天记录只统计 `isContinuation = false` 的首段，续段不增加首页频率。
- 事件块长按 0.5 秒开始记录；进行中记录长按 0.5 秒结束。两者都有进度反馈，并按设置决定是否震动；短按仍打开详情或执行原有短按操作。
- “未分组”是系统分组，固定在分组列表底部且不可排序；其下方固定保留“新建分组”入口。

记录详情当前按钮顺序：

- 进行中记录：结束、编辑、克隆、备注、删除、关闭
- 已结束记录：编辑、克隆、备注、删除、关闭

### 2. 补录和记录编辑

- 补录和记录编辑都使用“先选分组，再选分组内事件”的两级选择结构，长内容可滚动。
- 补录的“上条结束+1分”取所选日期内结束时间最晚的记录；加一分钟后仍在该日期才可用。
- 编辑记录的“上条结束+1分”排除当前记录，只查找结束时间严格早于本条原开始时间、结束日期与本条开始日期相同的最近记录；加一分钟后仍在同一天才可用。
- 结束时间早于开始时间时，解释为次日结束。
- 编辑进行中记录时可选择“保持进行中”；否则需要设置结束时间。
- 时间选择轮以视口中心项作为当前值，滚动结束后自动吸附；外部值变化时同步滚动到对应项，保证选择值与显示值一致。

### 3. 时间轴视图

- 时间轴提供“比例”和“紧凑”两种视图，默认比例视图。
- 切换按钮位于“补录”左侧：比例视图下显示“紧凑”，紧凑视图下显示“比例”。
- 视图选择使用 `rememberSaveable`，未写入 DataStore；全新启动默认比例视图。
- 比例视图的纵向缩放初始值为 1，范围为 `0.7～36`。最大缩放时每分钟约 36dp，两分钟记录约 72dp，可容纳色块文字。
- 比例视图按色块可用宽度测量名称和完整起止时间：足够时同一行，不足时分两行，各自仍放不下时使用省略号。
- 备注始终位于名称和时间下方，颜色使用 `onSurfaceVariant`；可显示行数根据标题占用行数和色块当前高度动态计算，剩余内容使用省略号。
- 备注星号只作为时间轴色块的备注标记，不显示在首页记录卡片、详情弹窗或备注列表。
- 紧凑视图不按时间比例确定色块位置和高度，记录按开始时间从上到下排列。
- 紧凑色块固定显示名称、起止时间两行，保留上下内边距；有备注时只在名称前显示星号，不显示正文；文字过长使用省略号。
- 紧凑视图仍按实际时间判断重叠，并沿用横向分列和宽度规则。
- 紧凑视图不显示小时刻度、横线、当前时间线，也不启用缩放。
- 当前时间线只在比例视图且所选日期为今天时显示。

### 4. 时间轴手势

- 多指缩放以全部有效触点的中心所对应的时间为锚点，同时处理缩放和上下平移，不处理旋转。
- 三指及以上仍按全部有效触点计算中心、距离、缩放和平移。
- 多指移动或缩放累计超过系统 `touchSlop` 后才接管并消费移动事件，避免轻微抖动影响色块点击。
- 普通单指手势不由时间轴拦截，直接使用外层 `LazyColumn` 的原生拖动、惯性和边界效果。
- 多指手势已经开始后若只剩一根手指，该手指可以继续连续滚动；重新记录速度后，松手产生惯性。
- 缩放和平移通过 `LazyListState.dispatchRawDelta` 同步修正滚动位置，每个触摸事件只更新一次；不使用缩放动画、逐帧滚动协程或后台循环。

### 5. 克隆记录

- 时间轴记录详情中的“克隆”对进行中和已结束记录都可用。
- 克隆弹窗先选择分组，再选择事件；默认优先选中源记录所属事件，长列表可滚动。
- 确认时 Repository 在事务内校验源记录、目标事件和目标分组；目标不存在或目标事件已删除时不创建记录。
- 新记录使用目标事件和目标分组的当前名称、分组和颜色快照，源记录保持不变。
- 已结束记录保留源记录开始和结束时间，并执行跨天切分。
- 进行中记录保留源记录开始时间，结束时间保持 `null`，克隆时暂不执行跨天切分，主界面立即出现新的进行中记录。
- 克隆不复制备注文字、备注图片或图片文件。

### 6. 备注

- 记录备注支持纯文字、纯图片或文字加图片；纯图片在列表和时间轴正文中显示 `[图片]`。
- 无备注时点击详情中的“备注”直接进入编辑器；已有文字或图片时先进入查看页，再通过“修改备注”进入编辑器。
- 备注查看页文字在上、图片在下，标题使用记录分组颜色。
- 备注页按记录 `startTime` 所在日期筛选，日期状态与时间轴独立；顶部提供前一天、今天、后一天和跳转，没有补录按钮。
- 备注列表显示左侧分组色条、事件名称、单行备注预览和记录时间。预览中的换行转为空格，过长时省略；纯图片显示 `[图片]`。
- 普通备注文字框默认从一行开始自动增高，达到高度上限后内部滚动，并使用顶部渐变提示上方仍有内容。
- 图片来源选择为“从相册选择”或“拍照”。相册使用 `PickMultipleVisualMedia(10)`，系统不支持时由 Activity Result API 回退到文档选择器；拍照使用 FileProvider 在 cacheDir 创建临时文件，成功后复制进草稿目录并删除临时文件。
- 普通编辑页显示图片缩略图，删除缩略图时同步删除草稿目录中的文件。
- 全屏文字编辑只显示文字输入和“收起”，不显示图片缩略图或添加图片入口。
- 退出编辑器但未正式保存时自动保存当前文字和草稿图片；即使内容为空也可能留下空草稿。
- 打开编辑器时优先恢复已有草稿；没有草稿时把已保存的备注文字和图片复制为新的编辑草稿。
- 进行中记录的草稿持续保留；已结束记录结束超过一天后清理；记录已不存在时也清理。

### 7. 统计

- 统计范围标签为“天、周、月、学期”，与日期导航合并在同一卡片中；范围选择器居中位于顶部，下方显示当前日期范围、跳转按钮和前后导航。
- 天、周、月共用 `AppViewModel.statsDate` 作为统计基准日期；切换范围不重置日期。该状态只保存在当前进程内，应用进程重启后恢复当天。
- 天显示基准日期；周根据设置中的每周起始日显示所在周的起止日期；月显示基准日期所在年月。
- 天、周、月分别提供前一范围、当前范围、后一范围按钮，并可通过“跳转”日期选择器修改基准日期；“今天／本周／本月”按钮会把基准日期恢复为当天。
- 学期继续由设置中的学期起始日期和学期周数决定，显示学期起止日期，不使用共用基准日期，也不提供前后导航或跳转。
- 记录时间会裁剪到所选范围；进行中记录以当前时间作为临时结束时间。
- 汇总卡片显示完整统计范围内的去重时长、累计时长、活跃天数和事件数，不随分组详情过滤。
- 去重时长目前按分钟集合去重，不是毫秒级区间合并。
- 默认“分组时间占比”饼图按分组累计时长绘制；分组按累计时长降序、名称升序排列，图例显示名称、累计时长和一位小数百分比。
- 点击分组扇形或分组图例后，原位置切换为该分组内各事件的时间占比饼图，不打开新页面，也不改变当前统计范围和基准日期。
- 分组详情中的事件扇形统一使用该分组颜色，以轮廓线分隔；选中事件使用更粗轮廓线。扇形和事件图例都可点击，选中后显示事件名称、累计时长和组内占比。
- 进入分组详情后，“事件排行”只显示该分组内的事件；右上角“返回”恢复完整分组饼图和全部事件排行。切换范围后若所选分组已无记录，会自动返回完整统计。
- 分组及其事件关系根据记录保存时的 `groupIdSnapshot`、分组名称和颜色快照计算，不根据事件当前所在分组回改历史归属。
- 事件排行按累计时长降序、记录段次数降序、事件名称升序排列。
- 重叠记录分别计入各自事件和分组，因此饼图分母采用累计时长。
- 跨天续段继续参与时长统计，确保范围内实际时长不丢失；只有首页频率排除了续段。

### 8. 外观与导航

- 底部导航顺序：首页、时间轴、备注、统计、设置。
- 设置首页使用接近 KernelSU 的大标题和分组圆角卡片；原“外观”入口已改为“主题设置”，说明文字为“自定义更多主题选项”。
- 主题设置使用“跟随系统 / 浅色 / 深色”三个横向按钮，并可选择 Material 或 MiuiX 风格；新安装默认 MiuiX，MiuiX `0.9.2` 已作为真实组件和主题依赖接入。
- 颜色与层次优化：Miuix 风格浅色模式采用 `#F2F2F7` 柔和背景与 `#FFFFFF` 纯白卡片，深色模式采用 `#121212` 舒适背景与 `#1C1C1E` 提升卡片，彻底解决原全白刺眼和全黑无层次的问题；Material Design 风格严格遵循 Google 最新 Material 3（Pixel 原生设置）规范，浅色使用 `#F8F9FA` Surface 和 `#F0F4F9` Container，深色使用 `#111318` Surface 和 `#1D2024` Container，保持与 Miuix 相同的页面布局结构，仅更换 M3 UI 控件元素（Card、Switch、Slider、SegmentedControl）。在主题根节点与 `MiuixCard` 内部显式注入 `androidx.compose.material3.LocalContentColor provides colorScheme.onSurface`，彻底解决了深色模式下部分标题与选项文本因缺失全局 ContentColor 导致退回默认黑色的问题。
- 阻尼回弹与固定标题：全应用列表与弹窗接入 MiuiX 的 `Modifier.overScrollVertical()` 阻尼弹簧回弹；设置主页“设置”大标题置于固定 Header 区域，不参与下拉拉伸，仅下方卡片列表拉伸回弹。
- 设置二级界面 GPU 纹理层离屏渲染与视差平移：二级页面跳转开启 `CompositingStrategy.Offscreen`，将主界面与进入的二级页面分别冻结为 GPU 纹理图层，在 RenderThread 上直接进行 120Hz 视差滑移，主界面同步向左平移 25%，全程零 CPU Recomposition / Layout Pass；根容器补全主题背景色，使底栏 `layerBackdrop` 采样始终稳定，彻底消除闪黑与掉帧。
- 性能与流畅度优化：`HorizontalPager` 开启相邻页预渲染（`beyondViewportPageCount = 1`），避免切页时实时 Compose 界面导致的卡顿掉帧；精简列表 `remember` 缓存代理与 `TimeWheel` 嵌套手势拦截；Room 数据库与文件 I/O 由原生事务执行器极速处理，带来 120Hz 满帧切页与手势响应体验。
- Monet 默认开启；未指定强调色时使用系统动态色，指定预设强调色后以该颜色生成主题。关闭 Monet 时隐藏强调色选择并恢复 MiuiX 默认明暗配色。Monet 与强调色设置由 DataStore 持久化并进入 CSV 备份。
- 底栏可选择悬浮或贴合屏幕底部的 MiuiX 导航栏；液态玻璃默认开启且只在悬浮底栏生效。主页面只能通过底栏切换，`HorizontalPager` 的手势翻页关闭，避免与横向滑杆冲突。
- 悬浮液态玻璃底栏使用满圆角，以 MiuiX 官方示例的双层内容遮罩实现选中项着色，并使用 `DampedDragAnimation`、组合 Backdrop、模糊、鲜艳度、AGSL lens 和高光完成切换及按压效果；相关适配代码保留 Apache-2.0 来源声明。
- 没有复制 KernelSU 的 GPL 源码；实现来自 Apache-2.0 的 compose-miuix-ui 示例及其上游 AndroidLiquidGlass，只参考 KernelSU 的集成方式。
- 悬浮模式下页面完整绘制至系统导航栏，滚动内容额外保留 120dp 底部可达区域，因此内容会真实出现在玻璃后方；固态模式继续由 Scaffold Insets 与系统导航区融合。
- 字号使用 7 档吸附滑杆，默认跟随系统；其他档位均相对于系统 fontScale 调整，不使用无级缩放。
- 继续支持自定义字号、壁纸、纯色背景、组件透明度、整页玻璃效果和模糊度。
- 壁纸的横屏和竖屏缩放、横向偏移、纵向偏移分别保存。
- 新增或修改界面时需要适配应用内字号和系统字号，避免写死文字区域高度；弹窗长内容应可滚动。
- 主界面启用 Android 边到边显示，内容可延伸到系统导航栏和手势横条区域，不隐藏手势横条。
- `ui/MainActivity.kt` 在应用入口调用 `enableEdgeToEdge()`；`ui/Theme.kt` 统一设置系统栏为透明，关闭导航栏对比度强制和分隔线，并根据当前主题设置系统栏图标颜色。
- 现有 Compose `Scaffold` 和系统 Insets 处理继续保留，底部导航及其他底部控件不能被手势横条遮挡。

## 五、跨天记录规则

- `AppRepository.normalizeOvernightInTransaction` 按系统时区和自然日拆分跨天记录。
- 切分会删除原记录并为每一段生成新 ID；首段为 `isContinuation = false`，后续段为 `true`。如果输入记录本身已经是续段，新生成段继续保持续段标记。
- 已结束记录每段都有结束时间；进行中跨天记录的最后一段保持 `endTime = null`。
- 各段时间不重叠，因此事件和分组时长统计必须包含全部段。
- 备注文字随 record copy 保留在各段；备注图片和图片文件重新归属第一段。
- 补录、编辑已结束记录、结束进行中记录和克隆已结束记录后都会执行现有跨天切分规则。
- 首页事件频率只计算首段，不能按数据库记录段总数直接计数。

## 六、当前开发状态

当前 `ui` 基于 `main` 的 `78a1963`，版本号仍为 `v2.11 / 16`。本轮 UI 改动包括：

- 设置页改为 KernelSU 风格的分组卡片二级菜单：主题设置、行为、首页显示、统计、学期设置、导入 / 导出；进入二级页时向左平移，返回时向右平移；系统返回在二级页返回设置首页；新手引导是设置首页直接入口。
- 主题设置页包含三按钮明暗模式、Material / MiuiX、Monet 与强调色、悬浮底栏和液态玻璃选项；这些值保存在 DataStore，并进入 CSV 设置备份。
- 壁纸、主题、字体、组件透明度、整页玻璃效果和模糊度仍归入主题设置。透明度界面为 `0%` 不透明、`100%` 全透明；内部 `componentAlpha` 保存实际不透明度，范围 `0..1`。
- 新增 `AppSettings.glassEffectEnabled`、`wallpaperBlurRadius`，由 `SettingsStore` 持久化和 `AppViewModel` 修改；模糊度为 `0..40dp`，滑块为 `0..100%`，玻璃效果关闭时滑块禁用。
- 玻璃效果开启后，图片壁纸按设置模糊，主要卡片使用更透明背景和细边框。
- 自选壁纸按实际图片尺寸计算铺满比例，支持拖动和双指缩放取景；缩放下限 `1f`，偏移按可移动范围计算，不留白。
- `Theme.kt` 全局形状：small `14dp`、medium `20dp`、large `28dp`。
- 新手引导按具体目标定位：首页时钟区、时间轴日期控件、备注日期控件、统计饼图、设置菜单项；切页后等待目标布局完成再显示，目标不存在不显示遮罩；提示卡按目标位置上下避让。

验证状态：

- 当前版本已经由用户确认可构建并正常运行；本次提交按用户要求不再重复执行编译或真机检查。
- 未推送、未创建 PR 或 Release；版本号保持 `2.11 / 16`。
## 七、协作与维护方式

- 用户提出文件或代码修改时，先等待用户说“全部说完”，再确认疑问、完整复述计划，得到确认后执行。
- 不修改与当前任务无关的代码，不覆盖工作区已有修改。
- 是否升版、构建 APK、提交、推送或创建 Release，以用户本次明确要求为准。
- 修改 Handoff 时直接更新已有章节，删除失效内容，不再按日期持续追加历史章节。
- 当前代码和构建文件是实现细节的最终依据；Handoff 记录的是不容易从单个文件看出的跨模块规则和当前工作状态。
- 文件使用 UTF-8 保存；完成必要验证后直接汇报结果，由用户人工检查。
