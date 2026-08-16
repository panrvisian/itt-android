# ITT 安卓应用交接 Prompt（完整工作上下文）

> 生成时间：2026-08-15。你是 ITT（Individual Time Trial）安卓应用（Kotlin + Jetpack Compose Material3）的开发维护者。
> 本文件是完整的交接上下文，请全部阅读后再开始任何工作。与用户的所有沟通**必须使用中文**，简明、务实，不用比喻、不用表情符号。

---

## 一、项目与代码库

- 项目路径：`D:\Administrator\Documents\Big-Brother\android-mobile`
- 技术栈：Kotlin + Jetpack Compose（Material3）、Room（版本 2）、DataStore Preferences、CSV/zip 导入导出、ViewModel + Repository 模式
- 应用信息：applicationId `com.bigbrother.mobile`，minSdk 31，targetSdk 34，compileSdk 34，Gradle 8.7，Kotlin compiler 1.5.14
- 当前版本：`versionName = "2.8"`，`versionCode = 13`（位于 `app/build.gradle.kts`）
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

- APK 输出：`app\build\outputs\apk\debug\app-debug.apk`，交付时复制为 `ITT-v2.8-build-yyyyMMdd-HHmm-debug.apk`（放项目根目录），再汇报路径
- 构建已知坑：若上一次构建被中断，会遗留 java 进程锁住 build 目录，报 `Failed to clean up output files for task ':app:mergeDebugResources'` 或 `Gradle build daemon has been stopped`。解决：`Get-Process java` 找到残留进程 → `Stop-Process -Id <pid> -Force` → 重试构建
- 每次实际改动后必须真实构建成功才能向用户汇报 APK 路径

## 三、与用户的沟通规则（用户明确要求）

1. 回复一律中文，简明务实，不用比喻、不用 emoji
2. 开始新功能前：先确认疑问 → 完整复述实现计划 → **等用户回复「确认 / 开始修改」** 后才动代码
3. 遇到不确定的地方先问，不擅自决定
4. 用户要求构建时才构建（或任务需要交付时），且**必须真正构建成功**后再报告
5. 不碰与本任务无关的功能
6. 开工前确保改动在不同字号（App 设置里的 小/中/大/特大 + 系统字号）下都能正常显示：文字用 sp/Material 排版，不写死文字高度，容器用 minLines/heightIn 自适应

## 四、已实现功能与当前状态（v2.8，全部已完成并构建通过）

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

## 五、关键文件与代码结构

- `app/build.gradle.kts`：版本 2.8 / 13；依赖：Room 2.6.1、DataStore 1.1.1、navigation-compose 2.7.7、compose-bom 2024.06.00、activity-compose 1.9.0、core-splashscreen 1.0.1、lifecycle 2.8.4
- `data/AppModels.kt`：`GroupEntity` / `EventEntity` / `RecordEntity`（含 noteText）/ `NoteImageEntity` / `AppSettings` / `AppBundle`（含 `noteImages: List<NoteImageEntity>`）/ `newId()`
- `data/AppDatabase.kt`：Room v2，`GroupDao`/`EventDao`/`RecordDao`（含 `updateNoteText`）/`NoteImageDao`（`observeAll/getByRecord/getAllOnce/insert/insertAll/deleteByRecord/deleteAll`），`MIGRATION_1_2`
- `data/AppRepository.kt`：全部 CRUD + 备注相关（`loadNoteView/loadNoteEditor/copyImageToDraft/removeDraftImageFile/saveNoteDraft/saveNote/cleanupNoteDrafts` + `noteImageFile/draftImageFile` 文件访问器）+ zip 导出导入（`buildZip/isZip/readZip/writeNoteImageFile`，路径校验防 zip-slip）+ `normalizeOvernightInTransaction`（跨天切分时 `reassignNoteImages` 把图片改挂到第一段）+ `deleteNoteData`（删记录时清备注/草稿）+ `updateRecord` 保留 noteText
- `data/NoteDraftStore.kt`：草稿 DataStore（`hasDraft/loadText/loadImages/save/clear/clearAll/allDraftRecordIds`）
- `data/SettingsStore.kt`：AppSettings DataStore（未改动）
- `data/CsvCodec.kt`：`export/parse`；RECORDS 第 10 列 note；`[NOTE_IMAGES]` 段（record_id,file_name,sort_order）；`csvRecordSequence` 逐字符状态机（支持引号内换行/逗号/转义引号），取代旧的行级 splitCsv
- `ui/AppViewModel.kt`：`AppTab` 枚举 = Home/Timeline/Stats/Notes/Settings；flows：`groups/events/records/noteImages/settings`；`timelineDate`、`notesDate`；备注相关 suspend 方法透传 repository
- `ui/AppRoot.kt`（约 2572 行）：AppRoot（tabs/pager/对话框状态）、HomeScreen、RunningRecordsSection（长按结束）、CurrentTimeSection（提示文字）、GroupSection、TimelineScreen/TimelineContent/TimelineDayView（双指缩放锚点=屏幕中心时间）/TimelineRecordBlock（备注正文动态行数）、StatsScreen、SettingsScreen（zip 导出导入）、WallpaperBackground/编辑器、AddGroup/AddEvent/GroupMenu/EventMenu/RecordDetailDialog（备注按钮在编辑下方）/ManualRecordDialog（上条结束+1分）/RecordEditorDialog（上条结束+1分）/DateWheelDialog（internal）/SimpleDialog（internal）、NumberWheel（居中吸附）、TimePickerRow、TimeAdjustRow、`visibleGroupsForUi()`（未分组系统组沉底不可排序）、`sortedForUi()`、`formatDuration/formatDurationToMinute/formatRunning`、`colorFromArgb`（internal）
- `ui/AppComponents.kt`：`LocalComponentAlpha`、`SectionCard`、`LongPressEventTile`（长按 0.5s）、`RecordCard`（新增 onLongPress 500ms + 进度填充 + vibrationEnabled）、`ChoiceChipRow`、`ColorSwatchRow`
- `ui/NoteScreens.kt`：`NotesScreen`（日期块 + 过滤 + `NoteListRow`）、`NoteListRow`（名称 · 预览 AnnotatedString）、`NoteViewDialog`、`NoteEditorDialog`（相册/拍照选择、全屏）、`FullscreenNoteEditor`、`rememberFileImage`
- `ui/Theme.kt`（`BigBrotherTheme`，自定义字体档通过 Density.fontScale 实现）、`ui/MainActivity.kt`（启动闪屏）
- `AndroidManifest.xml`：FileProvider；`res/xml/file_paths.xml`；`res/drawable/ic_notes.xml`
- ⚠️ `res/drawable/ic_history.xml` 仍存在但**无引用**（历史功能已移除，不要恢复）

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

## 七、用户「特别在意」的约束（不能破坏）

- 不改动无关功能；不恢复历史（history）功能；不删除「补录」入口；不删除「未分组」下方的「新建分组」按钮
- 「未分组」固定在最底部且不可排序；时间轮居中吸附、选择与显示同步；时间轴双指缩放锚点必须是屏幕中心对应的时间
- 无中文乱码；弹窗内容可读；字体大小变化下界面正常；APK 必须真实构建成功

## 八、已知限制与注意事项

- 跨天记录被 `normalizeOvernight` 切分时，noteText 随 copy 保留到各段，note_images 改挂到第一段并移动文件目录
- 草稿为空时退出也会留一个空草稿（无害，按清理规则处理）
- `PickMultipleVisualMedia` 内部自动回退 OpenDocument（满足「相册优先、文档回退」）
- 编辑页图片删除会同时删草稿目录文件；保存时草稿目录清空
- 未做：`ManualRecordDialog` 与 `RecordEditorDialog` 的完整合并（用户已知悉，未确认）

## 九、典型工作流（新功能）

1. 中文回复，先列疑问/不确定点让用户拍板
2. 复述完整实现计划，等「确认」
3. 实现（UTF-8、注意字号适配）
4. 功能改动则升版本（当前 2.8/13 → 2.9/14）
5. `assembleDebug` 构建成功 → 复制 `ITT-v2.8/2.9-build-yyyyMMdd-HHmm-debug.apk` 到项目根目录
6. 汇报 APK 路径 + 改动清单 + 需要用户知悉的取舍；若中途被构建残留进程卡住，杀 java 进程重试

## 十、当前状态快照

- 最后一次交付 APK：`ITT-v2.8-build-20260815-1111-debug.apk`（项目根目录）
- 无未完成的任务；全部已实现功能构建通过
- 交接起点：直接继续用户下一条指令即可
