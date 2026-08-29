# ITT 安卓应用交接 Prompt（完整工作上下文）

> 初始生成：2026-08-15；最近更新：2026-08-29。你是 ITT（Individual Time Trial）安卓应用（Kotlin + Jetpack Compose Material3）的开发维护者。
> 本文件是完整的交接上下文，请全部阅读后再开始任何工作。与用户的所有沟通**必须使用中文**，简明、务实，不用比喻、不用表情符号。

---

## 一、项目与代码库

- 项目路径：`D:\Administrator\Documents\Big-Brother\android-mobile`
- 技术栈：Kotlin + Jetpack Compose（Material3）、Room（版本 2）、DataStore Preferences、CSV/zip 导入导出、ViewModel + Repository 模式
- 应用信息：applicationId `com.bigbrother.mobile`，minSdk 31，targetSdk 34，compileSdk 34，Gradle 8.7，Kotlin compiler 1.5.14
- 当前版本：`versionName = "2.11"`，`versionCode = 16`（位于 `app/build.gradle.kts`）
- 版本规则：功能改动 → versionName 小版本 +1 且 versionCode +1；纯 bug 修复 → 可不升
- 所有 Kotlin/XML 必须 UTF-8 保存；编辑后检查无 U+FFFD / `���`（乱码）

## 二、构建环境（重要，2026-08-15 已迁移 JDK）

- ✅ **新 JDK（必须用这个）**：`C:\Program Files\Eclipse Adoptium\jdk-17.0.20+8`
- ❌ **旧 JDK 已删除，禁止引用**：`D:\Administrator\Documents\Big-Brother\.local\jdk17-extract\jdk-17.0.20+8`
- 注意：本机 pwsh 新进程**不继承**机器级环境变量（实测 `$env:JAVA_HOME` 为空、`java` 不在 PATH），每次构建命令必须**显式设置**：

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-17.0.20+8'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
cd 'D:\Administrator\Documents\Big-Brother\android-mobile'
.\gradlew.bat :app:compileDebugKotlin --no-daemon   # 快速编译检查
.\gradlew.bat :app:assembleDebug --no-daemon        # 完整打包
```

- APK 输出：`app\build\outputs\apk\debug\app-debug.apk`，交付时复制为 `ITT-v2.11-build-yyyyMMdd-HHmm-debug.apk`（放项目根目录），再汇报路径
- 构建已知坑：若上一次构建被中断，会遗留 java 进程锁住 build 目录，报 `Failed to clean up output files for task ':app:mergeDebugResources'` 或 `Gradle build daemon has been stopped`。解决：`Get-Process java` 找到残留进程 → `Stop-Process -Id <pid> -Force` → 重试构建
- 当前 JDK 在本环境可能出现 `Unable to establish loopback connection`。若出现：从 `lib/src.zip` 提取 `java.base/sun/nio/ch/PipeImpl.java`，将 `this.preferUnixDomain = preferUnixDomain;` 临时改为 `this.preferUnixDomain = false;`，用 `javac --patch-module java.base=<源码目录> -d <输出目录> <PipeImpl.java>` 编译临时补丁，再临时设置 `JAVA_TOOL_OPTIONS=--patch-module=java.base=<输出目录>` 后运行 Gradle；构建完成后删除补丁目录，不修改项目 `gradle.properties`
- 每次实际改动后必须真实构建成功才能向用户汇报 APK 路径

## 三、与用户的沟通规则（用户明确要求）

1. 回复一律中文，简明务实，不用比喻、不用 emoji
2. 开始新功能前：先确认疑问 → 完整复述实现计划 → **等用户回复「确认 / 开始修改」** 后才动代码
3. 遇到不确定的地方先问，不擅自决定
4. 用户要求构建时才构建（或任务需要交付时），且**必须真正构建成功**后再报告
5. 不碰与本任务无关的功能
6. 开工前确保改动在不同字号（App 设置里的 小/中/大/特大 + 系统字号）下都能正常显示：文字用 sp/Material 排版，不写死文字高度，容器用 minLines/heightIn 自适应

## 四、已实现功能与当前状态（v2.11，全部已完成并构建通过）

### 1. 备注（Notes）功能（本次会话主体，已完成）
- **数据层**：
  - `RecordEntity` 新增 `noteText: String = ""` 字段
  - 新增 `NoteImageEntity(id, recordId, fileName, sortOrder)`，表 `note_images`
  - Room 版本 1 → 2，`MIGRATION_1_2`（见 AppDatabase.kt）：`ALTER TABLE records ADD COLUMN noteText TEXT NOT NULL DEFAULT ''` + 建 `note_images` 表；AppContainer 用 `.addMigrations(...)` + `fallbackToDestructiveMigration()`
- **图片存储**：已保存图片在 `filesDir/notes/<recordId>/<fileName>`（fileName 只是文件名，如 `uuid.jpg`）；草稿图片在 `filesDir/notes_draft/<recordId>/<fileName>`；数据库只存文件名
- **草稿**：`NoteDraftStore.kt`（独立 DataStore，名 `note_drafts`，key 前缀 `note_draft_exists_/note_draft_text_/note_draft_images_`，图片名用逗号拼接）。退出编辑器未保存 → 自动存草稿；已保存的备注不被草稿覆盖。启动清理规则（已确认）：**进行中记录（endTime==null）草稿永久保留；已结束记录结束时间超过 1 天删除草稿**；记录已删除的草稿也删
- **编辑页**（`NoteEditorDialog`）：文本框（默认一行、自动增高、上限滚动、顶部渐变淡出提示溢出）+ 右侧「添加图片」→ 弹选择框「从相册选择 / 拍照 / 取消」；相册用 `PickMultipleVisualMedia(10)`（内部自动回退 OpenDocument），拍照用 `TakePicture` + FileProvider（临时文件在 cacheDir，拍完复制进草稿目录后删除）；文本框上方「展开」→ 全屏编辑（全屏不显示图片上传与缩略图，右下角「收起」）；底部「保存」；已选图片缩略图带右上角 × 可删除。纯文本/纯图片/两者皆可
- **查看页**（`NoteViewDialog`）：文字在上、图片缩略图在下、底部「修改备注」进编辑页；标题带分组色底（同详情弹窗风格）
- **入口**：`RecordDetailDialog` 的「备注」按钮放在「编辑」按钮**下方**（用户确认的位置）；无备注 → 直接进编辑页；有备注 → 先进查看页
- **备注 Tab**（底部导航第 5 个，位置在 统计 之前，即 首页/时间轴/备注/统计/设置）：
  - 顶部复制了时间轴的日期块（「日期」卡片 + 跳转日期选择器 + 前一天/今天/后一天），**无「补录」按钮**；列表按所选日期过滤（开始时间在该天的备注记录）；日期状态在 ViewModel（`notesDate`），与时间轴独立
  - 每行卡片：左侧分组色条（同「正在进行」RecordCard 风格）+ 第一行「事件名称 · 备注预览」（预览用 `bodySmall` 小一号字体、单行、换行符转空格、过长省略号）+ 第二行记录时间（`bodySmall` 灰）；**纯图片备注显示 `[图片]`**
- **时间轴色块**：有备注的记录事件名前加 ★（仅当色块高度足够显示文字时）；色块内名称/时间行下方用深灰（`onSurfaceVariant`）、同字号（`labelSmall`）显示备注正文，自动换行，**行数按色块当前高度动态计算**（跟随双指纵向缩放），放不下余下内容省略号；纯图片备注显示 `[图片]`
- **导出/导入改为 zip**（已确认方案 A）：zip 内含 `big_brother_mobile.csv` + `notes/<recordId>/<fileName>` 图片；导入自动识别 zip（PK 头）或纯 CSV（兼容旧文件，无图片）；CSV 新增 RECORDS 第 10 列 note + `[NOTE_IMAGES]` 段；设置页按钮「导出备份」（`application/zip`，文件名 `.zip`）、「导入覆盖 / 导入合并」（接受 zip 与 csv）

### 2. 长按交互（已完成）
- 事件长按开始：1 秒 → **0.5 秒**（`LongPressEventTile` 的 `tween(500)`，进度条同步）
- 实装长按结束：首页「正在进行」记录卡片长按 0.5 秒（带进度填充 + 震动反馈）直接结束该记录（`RecordCard` 新增 `onLongPress` + `vibrationEnabled` 参数，`pointerInput` + `detectTapGestures` 自定义 500ms 计时，与事件块同款逻辑）；短按仍是打开详情弹窗；备注页的 RecordCard 调用未传 onLongPress，不受影响
- 首页时钟下方提示文字已改为：「短按管理，长按 0.5 秒开始计时，长按进行中记录结束计时」

### 3. 编辑记录弹窗（已完成）
- `RecordEditorDialog`（主页「正在进行」记录详情 → 编辑）的「开始」时间卡片也加了「上条结束+1分」按钮，与时间轴「补录」一致
- 语义（与补录略不同，勿强行合并）：编辑时「上条」= 结束时间**严格早于**本条记录开始时间、且在同一天（本条开始日）、排除自身、结束时间最大的记录；+1 分钟仍须在同一天才可用
- 说明：两个弹窗的时间卡片本就走共享的 `TimeAdjustRow`/`TimePickerRow`，因此只补了三个 extra 参数；**整体合并两个弹窗未做**（已向用户说明可行但风险大，用户未确认）

### 4. 其他本次会话改动
- 底部导航：备注 在 统计 之前
- 新增图标 `res/drawable/ic_notes.xml`（note 图标）
- `AndroidManifest.xml` 新增 FileProvider（authorities `${applicationId}.fileprovider`，`res/xml/file_paths.xml` 配 cache-path）

### 5. 时间轴显示优化（v2.9，2026-08-28，已完成）
- **比例视图文字布局**：根据色块可用宽度测量名称和完整起止时间；宽度足够时同一行显示，宽度不足时名称与起止时间分两行；各自单行仍放不下时使用省略号。备注始终位于名称和时间下方，备注最大行数根据标题实际占用的一行或两行重新计算
- **紧凑视图**：时间轴日期卡片中，在「补录」左侧新增切换按钮；比例视图下按钮显示「紧凑」，紧凑视图下显示「比例」。默认使用比例视图
- 紧凑视图不按开始时间位置和持续时长比例绘制，记录按开始时间从上到下紧凑排列；每个色块固定显示两行：名称、起止时间，并增加上下内边距以适配不同字号
- 紧凑视图中，有备注时仅在名称前显示 ★，不显示备注正文；名称和起止时间放不下时使用省略号
- 紧凑视图继续按照实际时间判断记录重叠，并沿用原有横向分列与宽度规则；点击色块仍打开记录详情
- 紧凑视图不显示小时刻度、横线和当前时间红线，也不启用双指缩放
- 删除未引用的 `res/drawable/ic_history.xml`，历史功能仍保持移除状态

### 6. 记录克隆功能（v2.10，2026-08-29，已完成）
- **入口**：点击时间轴色块后打开 `RecordDetailDialog`，点击「克隆」按钮；进行中和已结束记录都可以克隆
- **选择弹窗**：`CloneRecordDialog` 按分组显示可用事件，支持单选、确认、取消和长列表滚动；默认优先选中原记录所属事件
- **目标事件**：确认后新记录使用目标事件的 `eventId`、名称快照和分组快照；原记录不变。Repository 在 Room 事务中校验源记录、目标事件和分组，目标事件不存在或已删除时不创建
- **已结束记录**：新记录的 `startTime`、`endTime` 与原记录相同；插入后继续执行既有 `normalizeOvernightInTransaction()` 跨天拆分规则
- **进行中记录**：新记录的 `startTime` 与原记录相同，`endTime = null`；不调用跨天拆分，因此主界面会显示新的进行中事件
- **备注和图片**：克隆记录使用默认空 `noteText`，不复制 `note_images` 和图片文件
- **涉及文件**：`ui/AppRoot.kt`、`ui/AppViewModel.kt`、`data/AppRepository.kt`、`res/drawable/ic_clone.xml`、`app/build.gradle.kts`

### 7. 时间轴缩放与滚动手势（v2.11，2026-08-29，已完成）
- **缩放范围**：比例视图纵向缩放限制为 `0.7～36`；最大缩放时每分钟高度为 36dp，2 分钟记录高度为 72dp，能够容纳色块文字
- **多指缩放**：统一使用 Compose 手势计算函数 `calculateCentroid`、`calculateCentroidSize`、`calculatePan`、`calculateZoom`；不处理旋转
- **缩放锚点**：使用多指中心点对应的时间作为锚点，缩放过程中该时间尽量保持在触点位置；不再以屏幕中心为锚点
- **双指平移**：双指相对距离不变时可上下平移；距离和位置同时变化时，同时处理缩放和平移；三指及以上按所有有效触点共同计算
- **纯单指滚动**：时间轴不拦截普通单指事件，直接交给外层 `LazyColumn`，因此拖动、惯性减速和边界效果与其他页面一致
- **多指转单指**：多指手势已开始后若只剩一个触点，由时间轴继续处理该次连续滑动；重新记录剩余触点速度，松手后使用 `ScrollableDefaults.flingBehavior()` 产生惯性
- **触摸阈值**：多指平移或缩放超过系统 `touchSlop` 后才接管并消费移动事件，避免轻微抖动影响色块点击
- **同步更新**：缩放和平移通过 `LazyListState.dispatchRawDelta` 同步修正滚动位置；不使用动画、不启动逐帧滚动协程或后台循环
- **已清理旧实现**：删除旧的前两个触点纵向距离算法、屏幕中心定位、`TimelineScrollCompensation`、`withFrameNanos` 等代码

## 五、关键文件与代码结构

- `app/build.gradle.kts`：版本 2.11 / 16；依赖：Room 2.6.1、DataStore 1.1.1、navigation-compose 2.7.7、compose-bom 2024.06.00、activity-compose 1.9.0、core-splashscreen 1.0.1、lifecycle 2.8.4
- `data/AppModels.kt`：`GroupEntity` / `EventEntity` / `RecordEntity`（含 noteText）/ `NoteImageEntity` / `AppSettings` / `AppBundle`（含 `noteImages: List<NoteImageEntity>`）/ `newId()`
- `data/AppDatabase.kt`：Room v2，`GroupDao`/`EventDao`/`RecordDao`（含 `updateNoteText`）/`NoteImageDao`（`observeAll/getByRecord/getAllOnce/insert/insertAll/deleteByRecord/deleteAll`），`MIGRATION_1_2`
- `data/AppRepository.kt`：全部 CRUD + 备注相关（`loadNoteView/loadNoteEditor/copyImageToDraft/removeDraftImageFile/saveNoteDraft/saveNote/cleanupNoteDrafts` + `noteImageFile/draftImageFile` 文件访问器）+ zip 导出导入（`buildZip/isZip/readZip/writeNoteImageFile`，路径校验防 zip-slip）+ `normalizeOvernightInTransaction`（跨天切分时 `reassignNoteImages` 把图片改挂到第一段）+ `deleteNoteData`（删记录时清备注/草稿）+ `updateRecord` 保留 noteText + `cloneRecord`（事务内按目标事件创建新记录，不复制备注和图片）
- `data/NoteDraftStore.kt`：草稿 DataStore（`hasDraft/loadText/loadImages/save/clear/clearAll/allDraftRecordIds`）
- `data/SettingsStore.kt`：AppSettings DataStore（未改动）
- `data/CsvCodec.kt`：`export/parse`；RECORDS 第 10 列 note；`[NOTE_IMAGES]` 段（record_id,file_name,sort_order）；`csvRecordSequence` 逐字符状态机（支持引号内换行/逗号/转义引号），取代旧的行级 splitCsv
- `ui/AppViewModel.kt`：`AppTab` 枚举 = Home/Timeline/Stats/Notes/Settings；flows：`groups/events/records/noteImages/settings`；`timelineDate`、`notesDate`；备注相关 suspend 方法透传 repository；`cloneRecord(recordId, eventId)` 透传记录克隆操作
- `ui/AppRoot.kt`（约 2744 行）：AppRoot（tabs/pager/对话框状态）、HomeScreen、RunningRecordsSection（长按结束）、CurrentTimeSection（提示文字）、GroupSection、TimelineScreen/TimelineContent、TimelineDayView（比例视图，双指缩放锚点=屏幕中心时间）、TimelineRecordBlock（名称/时间响应式一行或两行、备注正文动态行数）、CompactTimelineView/CompactTimelineRecordBlock（紧凑视图）、StatsScreen、SettingsScreen（zip 导出导入）、WallpaperBackground/编辑器、AddGroup/AddEvent/GroupMenu/EventMenu/RecordDetailDialog（备注按钮在编辑下方，含克隆按钮和 CloneRecordDialog）/ManualRecordDialog（上条结束+1分）/RecordEditorDialog（上条结束+1分）/DateWheelDialog（internal）/SimpleDialog（internal）、NumberWheel（居中吸附）、TimePickerRow、TimeAdjustRow、`visibleGroupsForUi()`（未分组系统组沉底不可排序）、`sortedForUi()`、`formatDuration/formatDurationToMinute/formatRunning`、`colorFromArgb`（internal）
- `ui/AppComponents.kt`：`LocalComponentAlpha`、`SectionCard`、`LongPressEventTile`（长按 0.5s）、`RecordCard`（新增 onLongPress 500ms + 进度填充 + vibrationEnabled）、`ChoiceChipRow`、`ColorSwatchRow`
- `ui/NoteScreens.kt`：`NotesScreen`（日期块 + 过滤 + `NoteListRow`）、`NoteListRow`（名称 · 预览 AnnotatedString）、`NoteViewDialog`、`NoteEditorDialog`（相册/拍照选择、全屏）、`FullscreenNoteEditor`、`rememberFileImage`
- `ui/Theme.kt`（`BigBrotherTheme`，自定义字体档通过 Density.fontScale 实现）、`ui/MainActivity.kt`（启动闪屏）
- `AndroidManifest.xml`：FileProvider；`res/xml/file_paths.xml`；`res/drawable/ic_notes.xml`；`res/drawable/ic_clone.xml`
- `res/drawable/ic_history.xml` 已于 v2.9 删除；历史功能已移除，不要恢复

## 六、已确认的产品决策（不要擅自更改）

1. 导出/导入：zip（CSV + 图片文件夹）；纯 CSV 兼容旧文件
2. 草稿清理：进行中记录草稿永久保留；已结束超 1 天删除
3. 备注 Tab 每行：事件名称 + 记录时间 + 左侧分组色条；预览单行、换行符转空格、过长省略号；纯图片显示 `[图片]`
4. 全屏输入：不显示图片缩略图、无上传按钮
5. 五角星：**只在时间轴色块**事件名前加（详情弹窗、首页卡片、备注列表都不加）
6. 「备注」按钮：详情弹窗里放在「编辑」按钮**下方**
7. 时间轴色块备注：深灰字、自动换行、行数随色块高度动态计算、余下省略号；纯图片显示 `[图片]`
8. 备注 Tab 顶部日期块：复制时间轴的（前一天/今天/后一天/跳转），无「补录」
9. 底部导航顺序：首页 / 时间轴 / 备注 / 统计 / 设置
10. 长按开始 = 0.5 秒；长按进行中卡片 = 结束（0.5 秒，带反馈）
11. 图片来源选择框：相册 / 拍照
12. 编辑记录弹窗开始卡片也有「上条结束+1分」
13. 时间轴默认使用比例视图；按钮用于在比例视图与紧凑视图之间切换
14. 比例视图：宽度足够时名称和起止时间同一行，宽度不足时分两行；备注永远位于名称和时间下方
15. 紧凑视图：按开始时间紧凑排列，每个色块显示名称和起止时间两行；备注只显示名称前的 ★，不显示正文
16. 紧凑视图仍按实际时间判断重叠并分列，但不显示时间刻度、当前时间线，也不启用缩放
17. 记录详情弹窗提供「克隆」按钮；克隆弹窗按分组选择目标事件，支持确认或取消
18. 克隆已结束记录时，开始时间和结束时间保持与原记录相同
19. 克隆进行中记录时，开始时间保持与原记录相同，结束时间保持为空，主界面显示新的进行中事件
20. 克隆不复制原记录的备注文本、备注图片和图片文件；原记录不变

## 七、用户「特别在意」的约束（不能破坏）

- 不改动无关功能；不恢复历史（history）功能；不删除「补录」入口；不删除「未分组」下方的「新建分组」按钮
- 「未分组」固定在最底部且不可排序；时间轮居中吸附、选择与显示同步；比例视图双指缩放锚点必须是屏幕中心对应的时间；紧凑视图保持重叠分列规则
- 无中文乱码；弹窗内容可读；字体大小变化下界面正常；APK 必须真实构建成功

## 八、已知限制与注意事项

- 跨天记录被 `normalizeOvernight` 切分时，noteText 随 copy 保留到各段，note_images 改挂到第一段并移动文件目录
- 草稿为空时退出也会留一个空草稿（无害，按清理规则处理）
- `PickMultipleVisualMedia` 内部自动回退 OpenDocument（满足「相册优先、文档回退」）
- 编辑页图片删除会同时删草稿目录文件；保存时草稿目录清空
- 时间轴紧凑/比例选择目前是 `rememberSaveable` 界面状态，未写入 DataStore；全新启动默认比例视图
- 未做：`ManualRecordDialog` 与 `RecordEditorDialog` 的完整合并（用户已知悉，未确认）

## 九、典型工作流（新功能）

1. 中文回复，先列疑问/不确定点让用户拍板
2. 复述完整实现计划，等「确认」
3. 实现（UTF-8、注意字号适配）
4. 功能改动则在当前 2.11/16 基础上继续递增版本号
5. `assembleDebug` 构建成功 → 按当前版本复制 `ITT-v<版本>-build-yyyyMMdd-HHmm-debug.apk` 到项目根目录
6. 汇报 APK 路径 + 改动清单 + 需要用户知悉的取舍；若中途被构建残留进程卡住，杀 java 进程重试

## 十、当前状态快照

- 当前正式发布 APK：`ITT-v2.11-build-20260829-2240-debug.apk`（项目根目录）
- APK 大小：27,091,795 字节；SHA-256：`BF3690B2008C628F9FB3687351F741F4AC61F8E603318E25A38C9B8BE2F83AE3`
- v2.10 记录克隆功能和 v2.11 时间轴手势优化均已完成；`:app:assembleDebug --rerun-tasks --no-daemon` 构建成功
- 全部当前修改以 `v2.11：优化时间轴缩放与滚动手势` 提交并推送到 `origin/main`；已创建并推送 `v2.11` 标签，已创建 GitHub Release 并上传上述 APK
- 当前标签为 `v2.8`、`v2.11`；GitHub Release 为 v2.8、v2.11；未单独创建 v2.9/v2.10 标签和 Release
- 仓库托管于 GitHub 私有仓库 `panrvisian/itt-android`；中英双语 README 为 `README.md` / `README.en.md`
- 交接起点：直接继续用户下一条指令即可

---

## 十一、2026-08 会话补充（Git 与 GitHub 上线）

> 本节由 2026-08-16 / 08-20 会话补充，记录环境验证、Git 初始化与 GitHub 上线情况。

### 1. 环境验证结论（2026-08-16）

- JDK 17（`C:\Program Files\Eclipse Adoptium\jdk-17.0.20+8`）、Android SDK（`D:\Administrator\Documents\Big-Brother\.local\android-sdk`，platform 34、build-tools 34.0.0）、Gradle 8.7、Gradle 依赖缓存均可用
- 实测 `:app:assembleDebug --rerun-tasks --no-daemon` 构建成功；产物与 `ITT-v2.8-build-20260815-1111-debug.apk` 完全一致（SHA-256 `3D53784625B8A0067BB66B415FB3426B5FFAF5BDC991072CCBEAA3B4D6AFF71F`）
- Android Studio 未安装；SDK 无模拟器（emulator/system-images 缺失）；`adb devices` 无连接设备 → 只能编译打包，暂无法真机/模拟器运行验证
- 项目无单元测试/设备测试源码（`testDebugUnitTest` 为 NO-SOURCE）
- `lintDebug` 通过：29 个 Warning、0 Error（2026-08-16 的历史结果；当时包含 `ic_history.xml` 未引用警告，该资源已于 v2.9 删除）

### 2. Git 仓库（2026-08-16 初始化）

- 仓库位置：`android-mobile`（仅 App 相关部分；`..\.local` SDK 与 `bin/obj/publish` 的 .NET 项目不在仓库内）
- 分支 `main`；首次提交 `6d5fda0`（v2.8 快照）；标签 `v2.8`
- `.gitignore`：排除 `.gradle/`、`**/build/`、`local.properties`、`.idea/`、`*.iml`、`*.apk`、`*.aab`、`*.keystore` 等
- 本仓库提交身份：`Panrvisian <panrvisian@gmail.com>`
- 注意：`android-mobile` 已加入 Administrator 的 `safe.directory` 白名单；Codex 沙箱进程与真实用户进程环境不同，git/gh 操作必须在真实用户环境执行

### 3. GitHub 上线（2026-08-16，私有仓库）

- 仓库：`panrvisian/itt-android`（私有）
- 推送方式：SSH（`git@github.com:panrvisian/itt-android.git`）。原因：本机 DNS 将 `github.com` 解析到 `20.205.243.166`，该 IP 的 443 端口被网络干扰不可用；`api.github.com`（20.205.243.168）与 SSH 22 端口可用
- SSH 密钥：`C:\Users\Administrator\.ssh\id_ed25519`（私钥，必须保密）/ `id_ed25519.pub`（公钥，已上传 GitHub）；gh 的 `git_protocol = ssh`
- README：`README.md`（中文默认）+ `README.en.md`（英文），顶部互相链接；提交 `8357173`
- Release：`v2.8` 已发布，附件 `ITT-v2.8-build-20260815-1111-debug.apk`（27,026,249 字节）；私有仓库仅 owner 与 Read 权限协作者可下载

### 4. 令牌与安全（2026-08-20）

- 曾有一个经典令牌（`ghp_` 开头）泄露到聊天，已删除；随后用新令牌重新 `gh auth login`
- 2026-08-20 已验证：SSH 认证成功（`Hi panrvisian!`）、`gh auth status` 正常、`git ls-remote` 可读 `main` 与 `v2.8`
- 令牌不得粘贴到聊天或写入文件；SSH 私钥不得外传

### 5. 后续工作流（含 Git/GitHub）

1. 中文回复，先列疑问让用户拍板 → 复述计划 → 等「确认」
2. 实现（UTF-8、字号适配）→ 功能改动在当前 2.11/16 基础上继续递增版本号
3. `assembleDebug` 构建成功 → 按需复制 APK 到项目根目录
4. `git add .` → `git commit -m '说明'` → `git push`（SSH）
5. 需要交付安装包时：`gh release create <版本> <apk路径> --title ... --notes ...`（私有仓库，仅授权协作者可下载）


---

## 十二、2026-08-28 v2.9 时间轴更新

- 比例视图完成名称、起止时间的宽度自适应布局；备注固定显示在名称和时间下方
- 新增紧凑视图及「紧凑 / 比例」切换按钮；紧凑视图按开始时间排列，保留重叠分列规则，不显示备注正文
- 删除未引用的 `ic_history.xml`
- 版本升级为 `2.9 / 14`
- `:app:assembleDebug --no-daemon` 构建成功；交付 APK 为 `ITT-v2.9-build-20260827-0112-debug.apk`
- 功能提交 `ce158be` 与此前交接文档提交 `0f7cba9` 已推送到 `origin/main`
- 截至 v2.9 更新时，功能代码已推送至 `origin/main`，未创建 v2.9 标签和 GitHub Release；后续正式发布状态见 v2.11 更新记录
---

## 十三、2026-08-29 v2.10 记录克隆功能更新

- 本次已完成时间轴色块详情中的「克隆」功能，版本从 `2.9 / 14` 升级为 `2.10 / 15`
- `RecordDetailDialog` 在「编辑」下方增加「克隆」按钮；进行中记录和已结束记录均显示该按钮
- `CloneRecordDialog` 负责按分组列出事件、单选目标事件、确认或取消；默认优先选择原记录所属事件
- `MainViewModel.cloneRecord(recordId, eventId)` 调用 `AppRepository.cloneRecord(recordId, eventId)`
- `AppRepository.cloneRecord` 在数据库事务中读取源记录、目标事件和目标分组，创建新的 `RecordEntity`：目标事件快照来自目标事件，时间来自源记录，备注默认为空，不复制备注图片；目标事件不存在或已删除时直接放弃创建
- 已结束源记录：复制相同的开始和结束时间，并执行既有跨天拆分规则
- 进行中源记录：复制相同的开始时间，结束时间为 `null`，不执行跨天拆分；主界面会出现新的进行中记录
- 新增图标：`app/src/main/res/drawable/ic_clone.xml`
- 相关代码：`app/src/main/java/com/bigbrother/mobile/ui/AppRoot.kt`、`app/src/main/java/com/bigbrother/mobile/ui/AppViewModel.kt`、`app/src/main/java/com/bigbrother/mobile/data/AppRepository.kt`
- 本次构建成功：`:app:assembleDebug --no-daemon`
- 交付 APK：`D:\Administrator\Documents\Big-Brother\android-mobile\ITT-v2.10-build-20260829-1007-debug.apk`
- APK 大小：27,075,411 字节；SHA-256：`EE9B13D1C247ACE5AAEC949E7E103C8037E709334CE4E5CAEBE7D393613399A5`
- 构建期间因本机 JDK 的 Unix domain socket 回环连接问题使用了临时 `PipeImpl` 补丁；补丁已在构建后删除，项目 `gradle.properties` 未修改
- v2.10 功能随后与 v2.11 时间轴手势优化合并提交；未单独创建 v2.10 标签或 GitHub Release
---

## 十四、2026-08-29 v2.11 时间轴缩放与滚动更新

- 版本从 `2.10 / 15` 升级为 `2.11 / 16`
- 最大纵向缩放限制为 36，2 分钟记录在最大缩放下高度为 72dp
- 多指缩放使用触点中心对应时间作为锚点，同时支持多指上下平移；三指及以上按全部有效触点计算
- 普通单指滚动完全交由外层 `LazyColumn` 原生处理，惯性与其他页面保持一致
- 多指手势结束后剩余单指仍可连续滑动并在松手后产生惯性
- 多指操作超过系统触摸阈值后才接管触摸，轻微抖动不影响色块点击
- 相关代码集中在 `app/src/main/java/com/bigbrother/mobile/ui/AppRoot.kt`
- 完整构建命令：`:app:assembleDebug --rerun-tasks --no-daemon`，构建成功
- 正式 APK：`D:\Administrator\Documents\Big-Brother\android-mobile\ITT-v2.11-build-20260829-2240-debug.apk`
- APK 大小：27,091,795 字节；SHA-256：`BF3690B2008C628F9FB3687351F741F4AC61F8E603318E25A38C9B8BE2F83AE3`
- 构建使用临时 `PipeImpl` 回环补丁，构建完成后已删除；项目配置未写入补丁
- 全部当前修改以 `v2.11：优化时间轴缩放与滚动手势` 提交并推送到 `origin/main`
- 已创建并推送 `v2.11` 标签；已创建 GitHub Release，上传正式 APK并填写中文更新说明
