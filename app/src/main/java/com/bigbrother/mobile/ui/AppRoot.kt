@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.bigbrother.mobile.ui

import android.net.Uri
import android.content.Intent
import android.content.res.Configuration
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.runtime.withFrameNanos
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bigbrother.mobile.data.AppSettings
import com.bigbrother.mobile.R
import com.bigbrother.mobile.data.EventEntity
import com.bigbrother.mobile.data.EventGridColumns
import com.bigbrother.mobile.data.FontScaleMode
import com.bigbrother.mobile.data.GroupEntity
import com.bigbrother.mobile.data.RecordEntity
import com.bigbrother.mobile.data.ThemeMode
import com.bigbrother.mobile.data.TotalDurationMode
import com.bigbrother.mobile.data.WallpaperMode
import com.bigbrother.mobile.domain.StatsCalculator
import com.bigbrother.mobile.domain.StatsRangeKind
import com.bigbrother.mobile.domain.TimeUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun AppRoot(viewModel: MainViewModel) {
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val events by viewModel.events.collectAsStateWithLifecycle()
    val records by viewModel.records.collectAsStateWithLifecycle()
    val noteImages by viewModel.noteImages.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val statsRange by viewModel.statsRange.collectAsStateWithLifecycle()
    val timelineDate by viewModel.timelineDate.collectAsStateWithLifecycle()
    val notesDate by viewModel.notesDate.collectAsStateWithLifecycle()

    var showAddGroup by rememberSaveable { mutableStateOf(false) }
    var showAddEvent by rememberSaveable { mutableStateOf(false) }
    var showAddEventGroupId by rememberSaveable { mutableStateOf<String?>(null) }
    var manualRecordDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedEvent by remember { mutableStateOf<EventEntity?>(null) }
    var selectedGroup by remember { mutableStateOf<GroupEntity?>(null) }
    var selectedRecord by remember { mutableStateOf<RecordEntity?>(null) }
    var editingRecord by remember { mutableStateOf<RecordEntity?>(null) }
    var noteViewRecord by remember { mutableStateOf<RecordEntity?>(null) }
    var noteEditRecord by remember { mutableStateOf<RecordEntity?>(null) }

    val visibleGroups = remember(groups) { groups.visibleGroupsForUi() }
    val visibleEvents = remember(events) { events.visibleEventsForUi() }
    val eventRecordCounts = remember(records) { records.groupingBy { it.eventId }.eachCount() }
    val sortedEvents = remember(visibleEvents, eventRecordCounts) { visibleEvents.sortedForUi(eventRecordCounts) }
    val eventsByGroup = remember(sortedEvents) { sortedEvents.groupBy { it.groupId } }
    val eventMap = remember(visibleEvents) { visibleEvents.associateBy { it.id } }
    val groupMap = remember(visibleGroups) { visibleGroups.associateBy { it.id } }
    val runningRecords = remember(records) { records.filter { it.endTime == null }.sortedByDescending { it.startTime } }
    val finishedRecords = remember(records) { records.filter { it.endTime != null }.sortedByDescending { it.startTime } }
    val imageRecordIds = remember(noteImages) { noteImages.map { it.recordId }.toSet() }
    val notedRecordIds = remember(records, imageRecordIds) {
        (records.filter { it.noteText.isNotBlank() }.map { it.id } + imageRecordIds).toSet()
    }
    val notedRecords = remember(records, imageRecordIds) {
        records.filter { it.noteText.isNotBlank() || it.id in imageRecordIds }.sortedByDescending { it.startTime }
    }
    val openNote: (RecordEntity) -> Unit = { record ->
        if (record.noteText.isNotBlank() || record.id in notedRecordIds) noteViewRecord = record else noteEditRecord = record
    }

    val tabs = remember { listOf(AppTab.Home, AppTab.Timeline, AppTab.Notes, AppTab.Stats, AppTab.Settings) }
    val pagerState = rememberPagerState(initialPage = tabs.indexOf(selectedTab).coerceAtLeast(0), pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage to pagerState.currentPageOffsetFraction }
            .distinctUntilChanged()
            .collect { (page, offset) ->
                if (abs(offset) < 0.001f) {
                    tabs.getOrNull(page)?.let { if (it != selectedTab) viewModel.selectTab(it) }
                }
            }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    val label = when (tab) {
                        AppTab.Home -> "首页"
                        AppTab.Timeline -> "时间轴"
                        AppTab.Stats -> "统计"
                        AppTab.Notes -> "备注"
                        AppTab.Settings -> "设置"
                    }
                    val icon = when (tab) {
                        AppTab.Home -> R.drawable.ic_home
                        AppTab.Timeline -> R.drawable.ic_timeline
                        AppTab.Stats -> R.drawable.ic_stats
                        AppTab.Notes -> R.drawable.ic_notes
                        AppTab.Settings -> R.drawable.ic_settings
                    }
                    NavigationBarItem(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(index) }
                        },
                        icon = { AppIcon(icon, label) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
        ) {
            WallpaperBackground(settings = settings)
            CompositionLocalProvider(LocalComponentAlpha provides settings.componentAlpha) {
                HorizontalPager(
                    state = pagerState,
                    pageSpacing = 0.dp,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (tabs[page]) {
                AppTab.Home -> HomeScreen(
                    viewModel = viewModel,
                    settings = settings,
                    groups = visibleGroups,
                    events = visibleEvents,
                    eventsByGroup = eventsByGroup,
                    eventRecordCounts = eventRecordCounts,
                    records = records,
                    onEventClick = { selectedEvent = it },
                    onRecordClick = { selectedRecord = it },
                    onRecordEnd = { viewModel.endRecord(it.id) },
                    onAddEventForGroup = { groupId ->
                        showAddEventGroupId = groupId
                        showAddEvent = true
                    },
                    onAddGroup = { showAddGroup = true },
                    onGroupLongPress = { selectedGroup = it }
                )
                AppTab.Timeline -> TimelineScreen(
                    viewModel = viewModel,
                    settings = settings,
                    records = records,
                    notedRecordIds = notedRecordIds,
                    onRecordClick = { selectedRecord = it },
                    onAddManualRecord = { manualRecordDate = it }
                )
                AppTab.Stats -> StatsScreen(
                    settings = settings,
                    events = visibleEvents,
                    records = records,
                    range = statsRange,
                    onRangeChange = viewModel::setStatsRange
                )
                AppTab.Notes -> NotesScreen(
                    notedRecords = notedRecords,
                    imageRecordIds = imageRecordIds,
                    date = notesDate,
                    onDateChange = viewModel::setNotesDate,
                    onOpen = openNote
                )
                        AppTab.Settings -> SettingsScreen(viewModel = viewModel, settings = settings)
                    }
                }
            }
        }
    }

    if (showAddGroup) {
        AddGroupDialog(
            onDismiss = { showAddGroup = false },
            onConfirm = { name, color -> viewModel.addGroup(name, color); showAddGroup = false }
        )
    }

    if (showAddEvent) {
        AddEventDialog(
            groups = visibleGroups,
            initialGroupId = showAddEventGroupId,
            onDismiss = {
                showAddEvent = false
                showAddEventGroupId = null
            },
            onConfirm = { groupId, name ->
                viewModel.addEvent(groupId, name)
                showAddEvent = false
                showAddEventGroupId = null
            }
        )
    }

    selectedGroup?.let { group ->
        val movableGroups = visibleGroups.filterNot { it.isSystem }
        val groupIndex = movableGroups.indexOfFirst { it.id == group.id }
        GroupMenuDialog(
            group = group,
            canMoveUp = groupIndex > 0,
            canMoveDown = groupIndex >= 0 && groupIndex < movableGroups.lastIndex,
            onDismiss = { selectedGroup = null },
            onRename = { name -> viewModel.renameGroup(group.id, name) },
            onChangeColor = { color -> viewModel.changeGroupColor(group.id, color) },
            onMoveUp = {
                viewModel.moveGroup(group.id, -1)
                selectedGroup = null
            },
            onMoveDown = {
                viewModel.moveGroup(group.id, 1)
                selectedGroup = null
            },
            onDelete = {
                viewModel.deleteGroup(group.id)
                selectedGroup = null
            }
        )
    }

    selectedEvent?.let { event ->
        EventMenuDialog(
            event = event,
            groups = visibleGroups,
            onDismiss = { selectedEvent = null },
            onStart = { viewModel.startEvent(event.id); selectedEvent = null },
            onToggleFavorite = { viewModel.toggleFavorite(event.id, !event.isFavorite); selectedEvent = null },
            onRename = { name -> viewModel.renameEvent(event.id, name) },
            onMoveGroup = { groupId -> viewModel.moveEvent(event.id, groupId) },
            onDelete = { viewModel.deleteEvent(event.id); selectedEvent = null }
        )
    }

    selectedRecord?.let { record ->
        RecordDetailDialog(
            record = record,
            event = eventMap[record.eventId],
            group = groupMap[record.groupIdSnapshot],
            onDismiss = { selectedRecord = null },
            onEnd = { viewModel.endRecord(record.id); selectedRecord = null },
            onEdit = {
                editingRecord = record
                selectedRecord = null
            },
            onNote = {
                selectedRecord = null
                openNote(record)
            },
            onDelete = {
                if (record.endTime == null) viewModel.deleteRunningRecord(record.id) else viewModel.deleteHistoryRecord(record.id)
                selectedRecord = null
            }
        )
    }

    manualRecordDate?.let { date ->
        ManualRecordDialog(
            date = date,
            records = records,
            sortedEvents = sortedEvents,
            eventsByGroup = eventsByGroup,
            groups = visibleGroups,
            groupById = groupMap,
            onDismiss = { manualRecordDate = null },
            onConfirm = { eventId, startTime, endTime ->
                viewModel.addManualRecord(eventId, startTime, endTime)
                manualRecordDate = null
            }
        )
    }

    editingRecord?.let { record ->
        RecordEditorDialog(
            title = "编辑记录",
            record = record,
            records = records,
            sortedEvents = sortedEvents,
            eventsByGroup = eventsByGroup,
            groups = visibleGroups,
            groupById = groupMap,
            onDismiss = { editingRecord = null },
            onConfirm = { eventId, startTime, endTime ->
                viewModel.updateRecord(record.id, eventId, startTime, endTime)
                editingRecord = null
            }
        )
    }

    noteViewRecord?.let { record ->
        NoteViewDialog(
            record = record,
            viewModel = viewModel,
            onEdit = {
                noteViewRecord = null
                noteEditRecord = record
            },
            onDismiss = { noteViewRecord = null }
        )
    }

    noteEditRecord?.let { record ->
        NoteEditorDialog(
            record = record,
            viewModel = viewModel,
            onDismiss = { noteEditRecord = null }
        )
    }
}

@Composable
private fun HomeScreen(
    viewModel: MainViewModel,
    settings: AppSettings,
    groups: List<GroupEntity>,
    events: List<EventEntity>,
    eventsByGroup: Map<String, List<EventEntity>>,
    eventRecordCounts: Map<String, Int>,
    records: List<RecordEntity>,
    onEventClick: (EventEntity) -> Unit,
    onRecordClick: (RecordEntity) -> Unit,
    onRecordEnd: (RecordEntity) -> Unit,
    onAddEventForGroup: (String?) -> Unit,
    onAddGroup: () -> Unit,
    onGroupLongPress: (GroupEntity) -> Unit
) {
    val running = remember(records) { records.filter { it.endTime == null }.sortedByDescending { it.startTime } }
    val favorites = remember(events) { events.filter { it.isFavorite && !it.isDeleted } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
    ) {
        if (settings.showClockSection) {
            item { CurrentTimeSection(settings = settings) }
        }
        if (settings.showRunningSection) {
            item {
                RunningRecordsSection(
                    running = running,
                    vibrationEnabled = settings.vibrationEnabled,
                    onRecordClick = onRecordClick,
                    onRecordEnd = onRecordEnd
                )
            }
        }
        if (settings.showFavoriteSection) {
            item {
                SectionCard(title = "\u6536\u85CF / \u5E38\u7528") {
                    if (favorites.isEmpty()) {
                        Text("\u628A\u5E38\u7528\u4E8B\u4EF6\u70B9\u661F\u6807\u540E\u4F1A\u51FA\u73B0\u5728\u8FD9\u91CC", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        EventGrid(
                            events = favorites,
                            groups = groups,
                            settings = settings,
                            eventRecordCounts = eventRecordCounts,
                            onEventClick = onEventClick,
                            onEventLongPress = viewModel::startEvent
                        )
                    }
                }
            }
        }
        if (settings.showGroupedSection) {
            items(groups, key = { it.id }) { group ->
                GroupSection(
                    group = group,
                    settings = settings,
                    events = eventsByGroup[group.id].orEmpty(),
                    eventRecordCounts = eventRecordCounts,
                    onEventClick = onEventClick,
                    onEventLongPress = viewModel::startEvent,
                    onAddEvent = onAddEventForGroup,
                    onAddGroup = onAddGroup,
                    onGroupLongPress = onGroupLongPress
                )
            }
        }
    }
}

@Composable
private fun CurrentTimeSection(settings: AppSettings) {
    val now = produceClock()
    SectionCard(
        title = "Individual Time Trial",
        titleStyle = MaterialTheme.typography.headlineMedium,
        titleAlign = TextAlign.Center
    ) {
        Text(
            TimeUtils.formatClock(now, settings.showDateInClock, settings.use24Hour),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text("\u77ED\u6309\u7BA1\u7406\uFF0C\u957F\u6309 0.5 \u79D2\u5F00\u59CB\u8BA1\u65F6\uFF0C\u957F\u6309\u8FDB\u884C\u4E2D\u8BB0\u5F55\u7ED3\u675F\u8BA1\u65F6", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RunningRecordsSection(
    running: List<RecordEntity>,
    vibrationEnabled: Boolean,
    onRecordClick: (RecordEntity) -> Unit,
    onRecordEnd: (RecordEntity) -> Unit
) {
    val now = produceClock()
    SectionCard(title = "\u8FDB\u884C\u4E2D") {
        if (running.isEmpty()) {
            Text("\u6682\u65E0\u8FDB\u884C\u4E2D\u8BB0\u5F55", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                running.forEach { record ->
                    RecordCard(
                        title = record.eventNameSnapshot,
                        subtitle = "\u5F00\u59CB ${TimeUtils.formatDateTime(record.startTime)} \u00B7 ${formatRunning(record.startTime, now)}",
                        color = colorFromArgb(record.groupColorArgbSnapshot),
                        vibrationEnabled = vibrationEnabled,
                        onClick = { onRecordClick(record) },
                        onLongPress = { onRecordEnd(record) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EventGrid(
    events: List<EventEntity>,
    groups: List<GroupEntity>,
    settings: AppSettings,
    eventRecordCounts: Map<String, Int>,
    showGroupSubtitle: Boolean = true,
    onEventClick: (EventEntity) -> Unit,
    onEventLongPress: (String) -> Unit
) {
    val gridColumns = when (settings.eventGridColumns) {
        EventGridColumns.Auto -> autoEventGridColumns(events)
        EventGridColumns.Two -> 2
        EventGridColumns.Three -> 3
        EventGridColumns.Four -> 4
    }
    val groupMap = remember(groups) { groups.associateBy { it.id } }
    val sorted = remember(events, eventRecordCounts) { events.sortedForUi(eventRecordCounts) }
    val rows = remember(sorted, gridColumns) { buildEventRows(sorted, gridColumns) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max)) {
                row.forEach { cell ->
                    val event = cell.event
                    val group = groupMap[event.groupId]
                    Box(modifier = Modifier.weight(cell.span.toFloat()).fillMaxHeight()) {
                        LongPressEventTile(
                            title = event.name,
                            subtitle = if (showGroupSubtitle) group?.name ?: "\u672A\u5206\u7EC4" else null,
                            color = colorFromArgb(group?.colorArgb ?: 0xFF9E9E9E.toInt()),
                            modifier = Modifier.fillMaxWidth().fillMaxHeight().heightIn(min = if (showGroupSubtitle) 64.dp else 52.dp),
                            vibrationEnabled = settings.vibrationEnabled,
                            onClick = { onEventClick(event) },
                            onLongPress = { onEventLongPress(event.id) }
                        )
                    }
                }
                val usedSpan = row.sumOf { it.span }
                if (usedSpan < gridColumns) Spacer(modifier = Modifier.weight((gridColumns - usedSpan).toFloat()))
            }
        }
    }
}

private fun autoEventGridColumns(events: List<EventEntity>): Int {
    if (events.isEmpty()) return 2
    for (n in 4 downTo 2) {
        val allFit = events.all { event ->
            val span = if (event.name.length > 12) 2 else 1
            span * 12 >= event.name.length * n
        }
        if (allFit) return n
    }
    return 2
}

private data class EventGridCell(val event: EventEntity, val span: Int)

private fun buildEventRows(events: List<EventEntity>, columns: Int): List<List<EventGridCell>> {
    val rows = mutableListOf<MutableList<EventGridCell>>()
    var current = mutableListOf<EventGridCell>()
    var used = 0
    events.forEach { event ->
        val span = if (event.name.length > 12) minOf(2, columns) else 1
        if (used + span > columns && current.isNotEmpty()) {
            rows += current
            current = mutableListOf()
            used = 0
        }
        current += EventGridCell(event, span)
        used += span
        if (used >= columns) {
            rows += current
            current = mutableListOf()
            used = 0
        }
    }
    if (current.isNotEmpty()) rows += current
    return rows
}

@Composable
private fun GroupSection(
    group: GroupEntity,
    settings: AppSettings,
    events: List<EventEntity>,
    eventRecordCounts: Map<String, Int>,
    onEventClick: (EventEntity) -> Unit,
    onEventLongPress: (String) -> Unit,
    onAddEvent: (String) -> Unit,
    onAddGroup: () -> Unit,
    onGroupLongPress: (GroupEntity) -> Unit
) {
    var expanded by rememberSaveable(group.id) { mutableStateOf(true) }
    if (!expanded) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = LocalComponentAlpha.current))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    group.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).pointerInput(group.id) {
                        detectTapGestures(
                            onTap = { expanded = true },
                            onLongPress = { onGroupLongPress(group) }
                        )
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(onClick = { onAddEvent(group.id) }) { AppIcon(R.drawable.ic_add, "添加") }
                TextButton(onClick = { expanded = true }) { Text("展开") }
            }
        }
    } else {
        SectionCard(
            title = group.name,
            onTitleClick = { expanded = false },
            onTitleLongPress = { onGroupLongPress(group) },
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onAddEvent(group.id) }) { AppIcon(R.drawable.ic_add, "添加") }
                    TextButton(onClick = { expanded = false }) { Text("收起") }
                }
            }
        ) {
            if (events.isEmpty()) {
                Text("暂无事件，点击本分组添加", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                EventGrid(
                    events = events,
                    groups = listOf(group),
                    settings = settings,
                    eventRecordCounts = eventRecordCounts,
                    showGroupSubtitle = false,
                    onEventClick = onEventClick,
                    onEventLongPress = onEventLongPress
                )
            }
        }
    }
    if (group.isSystem) {
        Spacer(modifier = Modifier.height(8.dp))
        IconTextButton(
            text = "新建分组",
            iconRes = R.drawable.ic_add,
            onClick = onAddGroup,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
@Composable
private fun TimelineScreen(
    viewModel: MainViewModel,
    settings: AppSettings,
    records: List<RecordEntity>,
    notedRecordIds: Set<String>,
    onRecordClick: (RecordEntity) -> Unit,
    onAddManualRecord: (LocalDate) -> Unit
) {
    val day by viewModel.timelineDate.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            SectionCard(
                title = "日期",
                trailing = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconTextButton("补录", R.drawable.ic_add, onClick = { onAddManualRecord(day) })
                        TextButton(onClick = { showDatePicker = true }) { Text("跳转") }
                    }
                }
            ) {
                Text(
                    TimeUtils.formatDate(TimeUtils.startOfDay(day)),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = { viewModel.setTimelineDate(day.minusDays(1)) }, modifier = Modifier.weight(1f)) { Text("前一天", maxLines = 1) }
                    TextButton(onClick = { viewModel.setTimelineDate(LocalDate.now()) }, modifier = Modifier.weight(1f)) { Text("今天", maxLines = 1) }
                    TextButton(onClick = { viewModel.setTimelineDate(day.plusDays(1)) }, modifier = Modifier.weight(1f)) { Text("后一天", maxLines = 1) }
                }
            }
        }
        item { Text("点击色块查看详情，双指纵向缩放时间轴", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item {
            TimelineContent(
                settings = settings,
                records = records,
                day = day,
                listState = listState,
                notedRecordIds = notedRecordIds,
                onRecordClick = onRecordClick
            )
        }
    }
    if (showDatePicker) {
        DateWheelDialog(
            title = "选择日期",
            initialDate = day,
            onDismiss = { showDatePicker = false },
            onConfirm = {
                viewModel.setTimelineDate(it)
                showDatePicker = false
            }
        )
    }
}

@Composable
private fun TimelineContent(
    settings: AppSettings,
    records: List<RecordEntity>,
    day: LocalDate,
    listState: LazyListState,
    notedRecordIds: Set<String>,
    onRecordClick: (RecordEntity) -> Unit
) {
    val now = produceClock()
    val dayStart = remember(day) { TimeUtils.startOfDay(day) }
    val dayEnd = remember(day) { TimeUtils.startOfDay(day.plusDays(1)) }
    val showNowLine = day == LocalDate.now()
    val dayRecords = remember(records, dayStart, dayEnd, now, notedRecordIds) {
        buildTimelineItems(records, dayStart, dayEnd, now, notedRecordIds)
    }
    var timelineVerticalScale by rememberSaveable(day.toString()) { mutableFloatStateOf(1f) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (dayRecords.isEmpty()) {
            Text("这一天没有记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TimelineDayView(
            items = dayRecords,
            dayStart = dayStart,
            settings = settings,
            now = now,
            showNowLine = showNowLine,
            verticalScale = timelineVerticalScale,
            parentListState = listState,
            onVerticalScaleChange = { timelineVerticalScale = it },
            onRecordClick = onRecordClick
        )
    }
}
@Composable
private fun StatsScreen(

    settings: AppSettings,
    events: List<EventEntity>,
    records: List<RecordEntity>,
    range: StatsRangeKind,
    onRangeChange: (StatsRangeKind) -> Unit
) {
    val bounds = remember(range, settings) {
        StatsCalculator.rangeFor(range, LocalDate.now(), settings.semesterStartDate, settings.weekStartDay, settings.semesterWeeks)
    }
    val result = remember(records, events, bounds) {
        StatsCalculator.compute(records, events, bounds.first, bounds.second)
    }
    val ranges = listOf(StatsRangeKind.Today, StatsRangeKind.Week, StatsRangeKind.Month, StatsRangeKind.Semester)
    val labels = listOf("今天", "本周", "本月", "学期")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
    ) {
        item {
            SectionCard(title = "统计范围") {
                ChoiceChipRow(labels = labels, selectedIndex = ranges.indexOf(range), onSelected = { onRangeChange(ranges[it]) })
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                SummaryCard(modifier = Modifier.weight(1f), title = "去重时长", value = formatDurationToMinute(result.uniqueTotal))
                SummaryCard(modifier = Modifier.weight(1f), title = "累计时长", value = formatDurationToMinute(result.sumTotal))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                SummaryCard(modifier = Modifier.weight(1f), title = "活跃天数", value = "${result.activeDays}")
                SummaryCard(modifier = Modifier.weight(1f), title = "事件数", value = "${result.items.size}")
            }
        }
        item {
            SectionCard(title = "事件排行") {
                if (result.items.isEmpty()) {
                    Text("没有可统计的记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    val maxMillis = max(1L, result.items.maxOf { it.total.toMillis() })
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        result.items.forEach { item ->
                            val progress = item.total.toMillis().toFloat() / maxMillis.toFloat()
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(item.eventName, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(formatDurationToMinute(item.total))
                                }
                                Box(modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)) {
                                    Box(modifier = Modifier
                                        .fillMaxWidth(progress)
                                        .height(8.dp)
                                        .background(colorFromArgb(item.groupColorArgb)))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    viewModel: MainViewModel,
    settings: AppSettings
) {
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri: Uri? ->
        if (uri != null) viewModel.exportCsv(uri)
    }
    val importReplaceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) viewModel.importCsv(uri, false)
    }
    val importMergeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) viewModel.importCsv(uri, true)
    }
    val context = LocalContext.current
    val wallpaperLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            viewModel.setWallpaperUri(uri.toString())
        }
    }
    var showSemesterStartDialog by rememberSaveable { mutableStateOf(false) }
    var showWallpaperEditor by rememberSaveable { mutableStateOf(false) }
    if (showWallpaperEditor) {
        WallpaperEditorDialog(
            settings = settings,
            viewModel = viewModel,
            onPickWallpaper = { wallpaperLauncher.launch(arrayOf("image/*")) },
            onDismiss = { showWallpaperEditor = false }
        )
    }
    if (showSemesterStartDialog) {
        DateWheelDialog(
            title = "学期第一天",
            initialDate = settings.semesterStartDate,
            onDismiss = { showSemesterStartDialog = false },
            onConfirm = {
                viewModel.setSemesterStartDate(it)
                showSemesterStartDialog = false
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
    ) {
        item {
            SectionCard(title = "外观") {
                Text("主题")
                ChoiceChipRow(
                    labels = listOf("跟随系统", "浅色", "深色"),
                    selectedIndex = when (settings.themeMode) {
                        ThemeMode.System -> 0
                        ThemeMode.Light -> 1
                        ThemeMode.Dark -> 2
                    },
                    onSelected = {
                        viewModel.setThemeMode(
                            when (it) {
                                1 -> ThemeMode.Light
                                2 -> ThemeMode.Dark
                                else -> ThemeMode.System
                            }
                        )
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("字体")
                ChoiceChipRow(
                    labels = listOf("跟随系统", "小", "中", "大", "特大"),
                    selectedIndex = when (settings.fontScaleMode) {
                        FontScaleMode.System -> 0
                        FontScaleMode.Small -> 1
                        FontScaleMode.Medium -> 2
                        FontScaleMode.Large -> 3
                        FontScaleMode.XLarge -> 4
                    },
                    onSelected = {
                        viewModel.setFontScaleMode(
                            when (it) {
                                0 -> FontScaleMode.System
                                1 -> FontScaleMode.Small
                                2 -> FontScaleMode.Medium
                                3 -> FontScaleMode.Large
                                4 -> FontScaleMode.XLarge
                                else -> FontScaleMode.Medium
                            }
                        )
                    }
                )
            }
        }
        item {
            SectionCard(title = "壁纸") {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        val wallpaperLabel = when (settings.wallpaperMode) {
                            WallpaperMode.Default -> "默认黑白纯色"
                            WallpaperMode.Image -> "自选图片"
                            WallpaperMode.Solid -> "纯色"
                        }
                        Text("当前：$wallpaperLabel")
                        Text("\u7EC4\u4EF6\u900F\u660E\u5EA6\uFF1A${(settings.componentAlpha * 100).roundToInt()}%", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Slider(
                            value = settings.componentAlpha,
                            onValueChange = { viewModel.setComponentAlpha(it) },
                            valueRange = 0.25f..1f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    TextButton(onClick = { showWallpaperEditor = true }) { Text("调整壁纸") }
                }
            }
        }
        item {
            SectionCard(title = "首页显示") {
                SettingLine("显示时钟", settings.showClockSection) { viewModel.setShowClockSection(it) }
                SettingLine("显示进行中", settings.showRunningSection) { viewModel.setShowRunningSection(it) }
                SettingLine("显示收藏", settings.showFavoriteSection) { viewModel.setShowFavoriteSection(it) }
                SettingLine("显示分组", settings.showGroupedSection) { viewModel.setShowGroupedSection(it) }
                Spacer(modifier = Modifier.height(8.dp))
                Text("事件按钮列数")
                ChoiceChipRow(
                    labels = listOf("自动", "2", "3", "4"),
                    selectedIndex = when (settings.eventGridColumns) {
                        EventGridColumns.Auto -> 0
                        EventGridColumns.Two -> 1
                        EventGridColumns.Three -> 2
                        EventGridColumns.Four -> 3
                    },
                    onSelected = {
                        viewModel.setEventGridColumns(
                            when (it) {
                                1 -> EventGridColumns.Two
                                2 -> EventGridColumns.Three
                                3 -> EventGridColumns.Four
                                else -> EventGridColumns.Auto
                            }
                        )
                    }
                )
            }
        }
        item {
            SectionCard(title = "行为") {
                SettingLine("震动", settings.vibrationEnabled) { viewModel.setVibrationEnabled(it) }
                SettingLine("时间显示日期", settings.showDateInClock) { viewModel.setShowDateInClock(it) }
                SettingLine("24 小时制", settings.use24Hour) { viewModel.setUse24Hour(it) }
                Spacer(modifier = Modifier.height(8.dp))
                Text("收藏自动填充数量：${settings.favoriteAutoFillCount}")
                ChoiceChipRow(
                    labels = listOf("4", "6", "8", "12"),
                    selectedIndex = listOf(4, 6, 8, 12).indexOf(settings.favoriteAutoFillCount).coerceAtLeast(0),
                    onSelected = { viewModel.setFavoriteAutoFillCount(listOf(4, 6, 8, 12)[it]) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("累计口径")
                ChoiceChipRow(
                    labels = listOf("累计", "去重"),
                    selectedIndex = if (settings.totalDurationMode == TotalDurationMode.Sum) 0 else 1,
                    onSelected = { viewModel.setTotalDurationMode(if (it == 1) TotalDurationMode.Unique else TotalDurationMode.Sum) }
                )
            }
        }
        item {
            SectionCard(title = "学期设置") {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("学期第一天")
                        Text(settings.semesterStartDate.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = { showSemesterStartDialog = true }) { Text("修改") }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("共计周数")
                ChoiceChipRow(
                    labels = listOf("16", "18", "20", "24"),
                    selectedIndex = listOf(16, 18, 20, 24).indexOf(settings.semesterWeeks).coerceAtLeast(0),
                    onSelected = { viewModel.setSemesterWeeks(listOf(16, 18, 20, 24)[it]) }
                )
            }
        }
        item {
            SectionCard(title = "导入 / 导出") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    IconTextButton(text = "导出备份", iconRes = R.drawable.ic_export, onClick = { exportLauncher.launch("big_brother_mobile.zip") })
                    IconTextButton(text = "导入覆盖", iconRes = R.drawable.ic_import, onClick = { importReplaceLauncher.launch(arrayOf("application/zip", "text/csv", "text/*", "*/*")) })
                }
                Spacer(modifier = Modifier.height(8.dp))
                IconTextButton(text = "导入合并", iconRes = R.drawable.ic_import, onClick = { importMergeLauncher.launch(arrayOf("application/zip", "text/csv", "text/*", "*/*")) })
            }
        }
    }
}

@Composable
private fun WallpaperBackground(settings: AppSettings, modifier: Modifier = Modifier) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val scale = if (isLandscape) settings.wallpaperLandscapeScale else settings.wallpaperPortraitScale
    val offsetX = if (isLandscape) settings.wallpaperLandscapeOffsetX else settings.wallpaperPortraitOffsetX
    val offsetY = if (isLandscape) settings.wallpaperLandscapeOffsetY else settings.wallpaperPortraitOffsetY
    val isDark = LocalIsDarkTheme.current
    val defaultColor = if (isDark) Color.Black else Color.White
    val solidColor = colorFromArgb(settings.wallpaperSolidColorArgb)
    val backgroundColor = when (settings.wallpaperMode) {
        WallpaperMode.Solid -> solidColor
        else -> defaultColor
    }
    val painter = rememberWallpaperPainter(if (settings.wallpaperMode == WallpaperMode.Image) settings.wallpaperUri else null)

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(backgroundColor), contentAlignment = Alignment.Center) {
        val imagePainter = painter
        if (settings.wallpaperMode == WallpaperMode.Image && imagePainter != null) {
            val density = LocalDensity.current
            val tx = with(density) { maxWidth.toPx() * offsetX }
            val ty = with(density) { maxHeight.toPx() * offsetY }
            Image(
                painter = imagePainter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = tx
                        translationY = ty
                    }
            )
        }
    }
}

@Composable
private fun rememberWallpaperPainter(uriString: String?): Painter? {
    val context = LocalContext.current
    val imageBitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, uriString) {
        value = null
        if (!uriString.isNullOrBlank()) {
            value = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(Uri.parse(uriString))?.use { input ->
                        BitmapFactory.decodeStream(input)?.asImageBitmap()
                    }
                }.getOrNull()
            }
        }
    }
    val bitmap = imageBitmap
    return if (bitmap != null) remember(bitmap) { BitmapPainter(bitmap) } else null
}
@Composable
private fun WallpaperEditorDialog(
    settings: AppSettings,
    viewModel: MainViewModel,
    onPickWallpaper: () -> Unit,
    onDismiss: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val baseScale = if (isLandscape) settings.wallpaperLandscapeScale else settings.wallpaperPortraitScale
    val baseOffsetX = if (isLandscape) settings.wallpaperLandscapeOffsetX else settings.wallpaperPortraitOffsetX
    val baseOffsetY = if (isLandscape) settings.wallpaperLandscapeOffsetY else settings.wallpaperPortraitOffsetY
    var scale by rememberSaveable(isLandscape) { mutableFloatStateOf(baseScale) }
    var offsetX by rememberSaveable(isLandscape) { mutableFloatStateOf(baseOffsetX) }
    var offsetY by rememberSaveable(isLandscape) { mutableFloatStateOf(baseOffsetY) }
    var showSolidPalette by rememberSaveable { mutableStateOf(false) }
    val previewSettings = if (isLandscape) {
        settings.copy(
            wallpaperLandscapeScale = scale,
            wallpaperLandscapeOffsetX = offsetX,
            wallpaperLandscapeOffsetY = offsetY
        )
    } else {
        settings.copy(
            wallpaperPortraitScale = scale,
            wallpaperPortraitOffsetX = offsetX,
            wallpaperPortraitOffsetY = offsetY
        )
    }
    val wallpaperModeText = when (settings.wallpaperMode) {
        WallpaperMode.Default -> "默认黑白纯色"
        WallpaperMode.Image -> "自选图片"
        WallpaperMode.Solid -> "纯色"
    }

    if (showSolidPalette) {
        SolidColorPaletteDialog(
            selectedColorArgb = settings.wallpaperSolidColorArgb,
            onDismiss = { showSolidPalette = false },
            onConfirm = { colorArgb ->
                viewModel.setWallpaperSolidColor(colorArgb)
                showSolidPalette = false
            }
        )
    }

    val dialogMaxHeight = (configuration.screenHeightDp * 0.92f).dp
    val previewAspect = (configuration.screenWidthDp.toFloat() / configuration.screenHeightDp.toFloat()).coerceIn(0.35f, 2.8f)
    val previewHeight = if (isLandscape) {
        (configuration.screenHeightDp * 0.34f).dp.coerceIn(130.dp, 220.dp)
    } else {
        (configuration.screenHeightDp * 0.48f).dp.coerceIn(300.dp, 460.dp)
    }

    SimpleDialog(title = "壁纸调整", onDismiss = onDismiss) {
        Column(modifier = Modifier.heightIn(max = dialogMaxHeight)) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .stopParentScrollAtBounds().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(if (isLandscape) "正在调整横屏" else "正在调整竖屏", color = MaterialTheme.colorScheme.primary)
                Text("当前壁纸：$wallpaperModeText", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onPickWallpaper, modifier = Modifier.weight(1f).heightIn(min = 46.dp)) { Text("自选") }
                    TextButton(onClick = { viewModel.setWallpaperDefault() }, modifier = Modifier.weight(1f).heightIn(min = 46.dp)) { Text("默认") }
                    TextButton(onClick = { showSolidPalette = true }, modifier = Modifier.weight(1f).heightIn(min = 46.dp)) { Text("纯色") }
                }
                Text("拖动图片调整位置，双指缩放调整大小。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("横屏和竖屏的位置与缩放互相独立。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .height(previewHeight)
                            .aspectRatio(previewAspect)
                            .clip(RoundedCornerShape(18.dp))
                            .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
                            .background(Color.Black)
                    ) {
                        val density = LocalDensity.current
                        val widthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
                        val heightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(isLandscape, settings.wallpaperMode) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        if (settings.wallpaperMode == WallpaperMode.Image) {
                                            scale = (scale * zoom).coerceIn(0.5f, 3f)
                                            offsetX = (offsetX + pan.x / widthPx).coerceIn(-0.5f, 0.5f)
                                            offsetY = (offsetY + pan.y / heightPx).coerceIn(-0.5f, 0.5f)
                                        }
                                    }
                                }
                        ) {
                            WallpaperBackground(settings = previewSettings)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) { Text("取消") }
                Button(
                    onClick = {
                        if (isLandscape) viewModel.setWallpaperLandscape(scale, offsetX, offsetY)
                        else viewModel.setWallpaperPortrait(scale, offsetX, offsetY)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp)
                ) { Text("保存") }
            }
        }
    }
}
@Composable
private fun SolidColorPaletteDialog(
    selectedColorArgb: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val palette = remember {
        listOf(
            Color.White, Color.Black, Color(0xFF111827), Color(0xFF374151),
            Color(0xFFEF4444), Color(0xFFF97316), Color(0xFFF59E0B), Color(0xFFEAB308),
            Color(0xFF22C55E), Color(0xFF14B8A6), Color(0xFF0EA5E9), Color(0xFF3B82F6),
            Color(0xFF6366F1), Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFFF43F5E)
        )
    }
    var selected by rememberSaveable(selectedColorArgb) { mutableIntStateOf(selectedColorArgb) }
    SimpleDialog(title = "选择颜色", onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("点击色块选择颜色，然后点保存。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.height(220.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(palette) { color ->
                    val argb = color.toArgb()
                    TextButton(
                        onClick = { selected = argb },
                        modifier = Modifier
                            .height(48.dp)
                            .border(
                                width = if (selected == argb) 3.dp else 1.dp,
                                color = if (selected == argb) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .background(color, RoundedCornerShape(14.dp))
                    ) {
                        if (selected == argb) Text("?", color = if (argb == Color.Black.toArgb()) Color.White else Color.Black)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) { Text("取消") }
                Button(onClick = { onConfirm(selected) }, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) { Text("保存") }
            }
        }
    }
}
@Composable
private fun AppIcon(@DrawableRes iconRes: Int, contentDescription: String?) {
    Icon(
        painter = painterResource(iconRes),
        contentDescription = contentDescription,
        modifier = Modifier.size(20.dp)
    )
}

@Composable
private fun IconText(text: String, @DrawableRes iconRes: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        AppIcon(iconRes, text)
        Text(text)
    }
}

@Composable
private fun IconTextButton(
    text: String,
    @DrawableRes iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    TextButton(onClick = onClick, modifier = modifier, enabled = enabled) {
        IconText(text, iconRes)
    }
}

@Composable
private fun SettingLine(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, modifier = Modifier.weight(1f))
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SummaryCard(modifier: Modifier = Modifier, title: String, value: String) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = LocalComponentAlpha.current))) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun AddGroupDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Int) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    val palette = listOf(Color(0xFF4F46E5), Color(0xFF0EA5E9), Color(0xFF10B981), Color(0xFFF59E0B), Color(0xFFEF4444), Color(0xFF8B5CF6))
    var selectedColor by rememberSaveable { mutableIntStateOf(0) }
    SimpleDialog(title = "新建分组", onDismiss = onDismiss) {
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("分组名称") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(12.dp))
        Text("选择颜色")
        Spacer(modifier = Modifier.height(8.dp))
        ColorSwatchRow(colors = palette, selectedIndex = selectedColor, onSelected = { selectedColor = it })
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("取消") }
            Button(onClick = { onConfirm(name.ifBlank { "新分组" }, palette[selectedColor].toArgb()) }, modifier = Modifier.weight(1f)) { Text("确认") }
        }
    }
}

@Composable
private fun AddEventDialog(
    groups: List<GroupEntity>,
    initialGroupId: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    val defaultGroupId = remember(initialGroupId, groups) {
        val initialMatch = groups.firstOrNull { it.id == initialGroupId }?.id
        initialMatch ?: groups.firstOrNull()?.id.orEmpty()
    }
    var selectedGroupId by remember(defaultGroupId, groups) { mutableStateOf(defaultGroupId) }
    SimpleDialog(title = "新建事件", onDismiss = onDismiss) {
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("事件名称") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(12.dp))
        Text("选择分组")
        Spacer(modifier = Modifier.height(8.dp))
        if (groups.isEmpty()) {
            Text("当前无可选分组", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 220.dp).stopParentScrollAtBounds().verticalScroll(rememberScrollState())) {
                groups.forEach { group ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        RadioButton(selected = selectedGroupId == group.id, onClick = { selectedGroupId = group.id })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(group.name)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("取消") }
            Button(onClick = { if (selectedGroupId.isNotBlank()) onConfirm(selectedGroupId, name.ifBlank { "新事件" }) }, modifier = Modifier.weight(1f)) { Text("确认") }
        }
    }
}

@Composable
private fun GroupMenuDialog(
    group: GroupEntity,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
    onChangeColor: (Int) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    val palette = listOf(Color(0xFF4F46E5), Color(0xFF0EA5E9), Color(0xFF10B981), Color(0xFFF59E0B), Color(0xFFEF4444), Color(0xFF8B5CF6))
    var name by rememberSaveable(group.id) { mutableStateOf(group.name) }
    var selectedColor by rememberSaveable(group.id) { mutableIntStateOf(palette.indexOfFirst { it.toArgb() == group.colorArgb }.coerceAtLeast(0)) }
    SimpleDialog(title = group.name, onDismiss = onDismiss) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("分组名称") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text("分组颜色")
        Spacer(modifier = Modifier.height(8.dp))
        ColorSwatchRow(colors = palette, selectedIndex = selectedColor, onSelected = { selectedColor = it })
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onMoveUp, modifier = Modifier.weight(1f), enabled = canMoveUp) { Text("上移") }
            TextButton(onClick = onMoveDown, modifier = Modifier.weight(1f), enabled = canMoveDown) { Text("下移") }
            if (!group.isSystem) {
                TextButton(onClick = onDelete, modifier = Modifier.weight(1f)) { Text("删除") }
            }
        }
        if (group.isSystem) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("系统分组不能删除或排序", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("取消") }
            Button(
                onClick = {
                    val nextName = name.ifBlank { group.name }
                    if (nextName != group.name) onRename(nextName)
                    val nextColor = palette[selectedColor].toArgb()
                    if (nextColor != group.colorArgb) onChangeColor(nextColor)
                    onDismiss()
                },
                modifier = Modifier.weight(1f)
            ) { Text("保存") }
        }
    }
}

@Composable
private fun EventMenuDialog(
    event: EventEntity,
    groups: List<GroupEntity>,
    onDismiss: () -> Unit,
    onStart: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRename: (String) -> Unit,
    onMoveGroup: (String) -> Unit,
    onDelete: () -> Unit
) {
    var name by rememberSaveable(event.id) { mutableStateOf(event.name) }
    var selectedGroupId by rememberSaveable(event.id) { mutableStateOf(event.groupId) }
    SimpleDialog(title = event.name, onDismiss = onDismiss) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("事件名称") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text("移动到")
        Spacer(modifier = Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 180.dp).stopParentScrollAtBounds().verticalScroll(rememberScrollState())) {
            groups.forEach { group ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    RadioButton(selected = selectedGroupId == group.id, onClick = { selectedGroupId = group.id })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(group.name)
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            IconTextButton("保存", R.drawable.ic_save, onClick = {
                val nextName = name.ifBlank { event.name }
                if (nextName != event.name) onRename(nextName)
                if (selectedGroupId.isNotBlank() && selectedGroupId != event.groupId) onMoveGroup(selectedGroupId)
                onDismiss()
            }, modifier = Modifier.fillMaxWidth())
            IconTextButton(if (event.isFavorite) "取消收藏" else "收藏", R.drawable.ic_favorite, onClick = onToggleFavorite, modifier = Modifier.fillMaxWidth())
            IconTextButton("移动分组", R.drawable.ic_group_add, onClick = {
                if (selectedGroupId.isNotBlank() && selectedGroupId != event.groupId) onMoveGroup(selectedGroupId)
            }, modifier = Modifier.fillMaxWidth(), enabled = selectedGroupId.isNotBlank() && selectedGroupId != event.groupId)
            IconTextButton("开始", R.drawable.ic_start, onClick = onStart, modifier = Modifier.fillMaxWidth())
            IconTextButton("删除", R.drawable.ic_delete, onClick = onDelete, modifier = Modifier.fillMaxWidth())
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("关闭") }
        }
    }
}

@Composable
private fun RecordDetailDialog(
    record: RecordEntity,
    event: EventEntity?,
    group: GroupEntity?,
    onDismiss: () -> Unit,
    onEnd: () -> Unit,
    onEdit: () -> Unit,
    onNote: () -> Unit,
    onDelete: () -> Unit
) {
    val now = produceClock()
    val eventColor = colorFromArgb(event?.let { group?.colorArgb } ?: record.groupColorArgbSnapshot)
    SimpleDialog(
        title = event?.name ?: record.eventNameSnapshot,
        onDismiss = onDismiss,
        titleStyle = MaterialTheme.typography.headlineSmall,
        titleAlign = TextAlign.Center,
        titleBackgroundColor = eventColor,
        titleContentColor = if (eventColor.luminance() > 0.5f) Color.Black else Color.White
    ) {
        Text("分组：${group?.name ?: record.groupNameSnapshot}")
        Spacer(modifier = Modifier.height(8.dp))
        Text("开始：${TimeUtils.formatDateTime(record.startTime)}")
        Spacer(modifier = Modifier.height(4.dp))
        Text("结束：${record.endTime?.let { TimeUtils.formatDateTime(it) } ?: "进行中"}")
        Spacer(modifier = Modifier.height(4.dp))
        Text("时长：${if (record.endTime == null) formatRunning(record.startTime, now) else formatDuration(Duration.ofMillis(record.endTime - record.startTime))}")
        Spacer(modifier = Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (record.endTime == null) {
                IconTextButton("结束", R.drawable.ic_stop, onClick = onEnd, modifier = Modifier.fillMaxWidth())
                IconTextButton("编辑", R.drawable.ic_edit, onClick = onEdit, modifier = Modifier.fillMaxWidth())
                IconTextButton("备注", R.drawable.ic_notes, onClick = onNote, modifier = Modifier.fillMaxWidth())
                IconTextButton("删除", R.drawable.ic_delete, onClick = onDelete, modifier = Modifier.fillMaxWidth())
            } else {
                IconTextButton("编辑", R.drawable.ic_edit, onClick = onEdit, modifier = Modifier.fillMaxWidth())
                IconTextButton("备注", R.drawable.ic_notes, onClick = onNote, modifier = Modifier.fillMaxWidth())
                IconTextButton("删除", R.drawable.ic_delete, onClick = onDelete, modifier = Modifier.fillMaxWidth())
            }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("关闭") }
        }
    }
}
@Composable
private fun ManualRecordDialog(
    date: LocalDate,
    records: List<RecordEntity>,
    sortedEvents: List<EventEntity>,
    eventsByGroup: Map<String, List<EventEntity>>,
    groups: List<GroupEntity>,
    groupById: Map<String, GroupEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String, Long, Long) -> Unit
) {
    val groupIdsWithEvents = remember(groups, sortedEvents, eventsByGroup) {
        groupIdsWithEvents(groups, eventsByGroup, sortedEvents)
    }
    var selectedEventId by rememberSaveable(date.toString()) { mutableStateOf(sortedEvents.firstOrNull()?.id.orEmpty()) }
    var selectedGroupId by rememberSaveable("${date}_manual_group") { mutableStateOf(sortedEvents.firstOrNull()?.groupId.orEmpty()) }
    var showingEventList by rememberSaveable("${date}_manual_event_level") { mutableStateOf(false) }
    val nowDefault = remember { TimeUtils.toLocalDateTime(TimeUtils.now()) }
    var startHour by rememberSaveable(date.toString()) { mutableIntStateOf(8) }
    var startMinute by rememberSaveable(date.toString()) { mutableIntStateOf(0) }
    var endHour by rememberSaveable(date.toString()) { mutableIntStateOf(nowDefault.hour) }
    var endMinute by rememberSaveable(date.toString()) { mutableIntStateOf(nowDefault.minute) }
    val selectedEvent = remember(selectedEventId, sortedEvents) { sortedEvents.firstOrNull { it.id == selectedEventId } }
    val selectedGroupName = selectedEvent?.let { groupById[it.groupId]?.name ?: "未分组" }.orEmpty()
    val crossesDay = endHour < startHour || (endHour == startHour && endMinute < startMinute)
    val lastEndOfDay = remember(date, records) {
        records
            .filter { it.endTime != null }
            .filter { TimeUtils.toLocalDate(it.endTime!!) == date }
            .maxByOrNull { it.endTime!! }
    }
    val lastEndPlusOne = remember(lastEndOfDay) { lastEndOfDay?.endTime?.plus(60_000L) }
    val lastEndPlusOneUsable = remember(lastEndPlusOne, date) {
        lastEndPlusOne != null && TimeUtils.toLocalDate(lastEndPlusOne) == date
    }
    val dialogMaxHeight = (LocalConfiguration.current.screenHeightDp * 0.86f).dp

    LaunchedEffect(sortedEvents) {
        if (sortedEvents.none { it.id == selectedEventId }) {
            val first = sortedEvents.firstOrNull()
            selectedEventId = first?.id.orEmpty()
            selectedGroupId = first?.groupId.orEmpty()
            showingEventList = false
        }
    }

    SimpleDialog(title = "补录记录", onDismiss = onDismiss) {
        Column(modifier = Modifier.heightIn(max = dialogMaxHeight)) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .stopParentScrollAtBounds().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("日期：$date", color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (sortedEvents.isEmpty()) {
                    Text("请先创建事件", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text("事件")
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = LocalComponentAlpha.current))) {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                            Text("当前选择", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                selectedEvent?.name ?: "未选择事件",
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (selectedGroupName.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(selectedGroupName, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }

                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = LocalComponentAlpha.current))) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 230.dp)
                                .stopParentScrollAtBounds().verticalScroll(rememberScrollState())
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (!showingEventList) {
                                Text("先选择分组", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                groupIdsWithEvents.forEach { groupId ->
                                    val group = groupById[groupId]
                                    val count = eventsByGroup[groupId].orEmpty().size
                                    TextButton(
                                        onClick = {
                                            selectedGroupId = groupId
                                            showingEventList = true
                                        },
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp)
                                    ) {
                                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(12.dp).background(colorFromArgb(group?.colorArgb ?: 0xFF9E9E9E.toInt()), shape = RoundedCornerShape(99.dp)))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(group?.name ?: "未分组", modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Start)
                                            Text("$count 个", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            } else {
                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(onClick = { showingEventList = false }, modifier = Modifier.weight(1f)) { Text("返回分组") }
                                    Text(groupById[selectedGroupId]?.name ?: "未分组", color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                eventsByGroup[selectedGroupId].orEmpty().forEach { event ->
                                    TextButton(
                                        onClick = { selectedEventId = event.id },
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp)
                                    ) {
                                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                            RadioButton(selected = selectedEventId == event.id, onClick = { selectedEventId = event.id })
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                if (event.isFavorite) "★ ${event.name}" else event.name,
                                                modifier = Modifier.weight(1f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Start
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Text("开始")
                    TimeAdjustRow(
                        startHour, { startHour = it }, startMinute, { startMinute = it },
                        extraLabel = "上条结束+1分",
                        extraEnabled = lastEndPlusOneUsable,
                        onExtraClick = {
                            lastEndPlusOne?.let { t ->
                                val ldt = TimeUtils.toLocalDateTime(t)
                                startHour = ldt.hour
                                startMinute = ldt.minute
                            }
                        }
                    )
                    TimePickerRow(startHour, { startHour = it }, startMinute, { startMinute = it })
                    Text("结束")
                    TimeAdjustRow(endHour, { endHour = it }, endMinute, { endMinute = it })
                    TimePickerRow(endHour, { endHour = it }, endMinute, { endMinute = it })
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            if (crossesDay && sortedEvents.isNotEmpty()) {
                Text("结束时间早于开始时间，将在次日结束", color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) { Text("取消") }
                Button(
                    onClick = {
                        val start = date.atTime(LocalTime.of(startHour, startMinute)).atZone(TimeUtils.zoneId).toInstant().toEpochMilli()
                        val endDate = if (crossesDay) date.plusDays(1) else date
                        val end = endDate.atTime(LocalTime.of(endHour, endMinute)).atZone(TimeUtils.zoneId).toInstant().toEpochMilli()
                        if (selectedEventId.isNotBlank()) onConfirm(selectedEventId, start, end)
                    },
                    enabled = selectedEventId.isNotBlank(),
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp)
                ) { Text("保存") }
            }
        }
    }
}
@Composable
private fun TimePickerRow(
    hour: Int,
    onHourChange: (Int) -> Unit,
    minute: Int,
    onMinuteChange: (Int) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        NumberWheel(0..23, hour, onHourChange, "小时", Modifier.weight(1f), showLabel = false, compact = true)
        NumberWheel(0..59, minute, onMinuteChange, "分钟", Modifier.weight(1f), showLabel = false, compact = true)
    }
}

private fun adjustTime(hour: Int, minute: Int, deltaMinutes: Int): Pair<Int, Int> {
    val total = (hour * 60 + minute + deltaMinutes).mod(24 * 60)
    return total / 60 to total % 60
}

@Composable
private fun TimeAdjustRow(
    hour: Int,
    onHourChange: (Int) -> Unit,
    minute: Int,
    onMinuteChange: (Int) -> Unit,
    extraLabel: String? = null,
    extraEnabled: Boolean = true,
    onExtraClick: (() -> Unit)? = null
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        if (extraLabel != null && onExtraClick != null) {
            TextButton(
                onClick = onExtraClick,
                enabled = extraEnabled,
                modifier = Modifier.weight(1f)
            ) {
                Text(extraLabel, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        TextButton(
            onClick = {
                val (h, m) = adjustTime(hour, minute, -15)
                onHourChange(h)
                onMinuteChange(m)
            },
            modifier = Modifier.weight(1f)
        ) { Text("-15 分") }
        TextButton(
            onClick = {
                val (h, m) = adjustTime(hour, minute, 15)
                onHourChange(h)
                onMinuteChange(m)
            },
            modifier = Modifier.weight(1f)
        ) { Text("+15 分") }
    }
}

@Composable
private fun NumberWheel(
    values: IntRange,
    selected: Int,
    onSelected: (Int) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    cyclic: Boolean = true,
    formatter: (Int) -> String = { it.toString().padStart(2, '0') },
    showLabel: Boolean = true,
    compact: Boolean = false
) {
    val valueList = remember(values.first, values.last) { values.toList() }
    val itemHeight = 40.dp
    val repeatCount = if (cyclic && valueList.isNotEmpty()) 1000 else 1
    val totalItems = (valueList.size * repeatCount).coerceAtLeast(valueList.size)
    val selectedIndex = valueList.indexOf(selected).coerceAtLeast(0)
    val initialIndex = remember(valueList.firstOrNull(), valueList.lastOrNull(), selectedIndex, totalItems) {
        if (cyclic && valueList.isNotEmpty()) {
            val middle = totalItems / 2
            (middle - middle.mod(valueList.size) + selectedIndex).coerceIn(0, totalItems - 1)
        } else {
            selectedIndex
        }
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val centeredItemInfo by remember(listState) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) {
                null
            } else {
                val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
                visibleItems.minByOrNull { item -> abs((item.offset + item.size / 2f) - viewportCenter) }
            }
        }
    }
    val centeredIndex = centeredItemInfo?.index ?: selectedIndex
    val centeredValue = if (valueList.isEmpty()) selected else valueList[centeredIndex.mod(valueList.size)]

    LaunchedEffect(selected, valueList, listState) {
        if (valueList.isEmpty()) return@LaunchedEffect
        val layoutInfo = listState.layoutInfo
        val visibleItems = layoutInfo.visibleItemsInfo
        if (visibleItems.isEmpty()) return@LaunchedEffect
        val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
        val centered = visibleItems.minByOrNull { item -> abs((item.offset + item.size / 2f) - viewportCenter) } ?: return@LaunchedEffect
        val centeredValueNow = valueList[centered.index.mod(valueList.size)]
        if (centeredValueNow == selected) return@LaunchedEffect
        var delta = ((selected - centeredValueNow) + valueList.size) % valueList.size
        if (delta > valueList.size / 2) delta -= valueList.size
        val targetIndex = (centered.index + delta).coerceIn(0, totalItems - 1)
        listState.scrollToItem(targetIndex)
        val infoAfter = listState.layoutInfo
        val visibleAfter = infoAfter.visibleItemsInfo
        if (visibleAfter.isNotEmpty()) {
            val centerAfter = (infoAfter.viewportStartOffset + infoAfter.viewportEndOffset) / 2f
            val targetItem = visibleAfter.minByOrNull { item -> abs((item.offset + item.size / 2f) - centerAfter) }
            if (targetItem != null) {
                listState.scrollBy((targetItem.offset + targetItem.size / 2f) - centerAfter)
            }
        }
    }

    LaunchedEffect(centeredValue, valueList) {
        if (valueList.isNotEmpty() && centeredValue != selected) onSelected(centeredValue)
    }

    LaunchedEffect(listState, valueList) {
        snapshotFlow { listState.isScrollInProgress }.distinctUntilChanged().collect { scrolling ->
            if (!scrolling && valueList.isNotEmpty()) {
                val layoutInfo = listState.layoutInfo
                val visibleItems = layoutInfo.visibleItemsInfo
                val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
                val target = visibleItems.minByOrNull { item -> abs((item.offset + item.size / 2f) - viewportCenter) }
                if (target != null) {
                    val delta = (target.offset + target.size / 2f) - viewportCenter
                    if (abs(delta) > 0.5f) listState.animateScrollBy(delta)
                    val snappedLayoutInfo = listState.layoutInfo
                    val snappedCenter = (snappedLayoutInfo.viewportStartOffset + snappedLayoutInfo.viewportEndOffset) / 2f
                    val snappedTarget = snappedLayoutInfo.visibleItemsInfo.minByOrNull { item -> abs((item.offset + item.size / 2f) - snappedCenter) }
                    val finalValue = valueList[(snappedTarget?.index ?: target.index).mod(valueList.size)]
                    if (finalValue != selected) onSelected(finalValue)
                }
            }
        }
    }

    Column(modifier = modifier) {
        if (showLabel) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
        }
        Box(modifier = Modifier.height(if (compact) itemHeight * 2 else itemHeight * 5).fillMaxWidth()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().stopParentScrollAtBounds(),
                contentPadding = PaddingValues(vertical = if (compact) itemHeight / 2 else itemHeight * 2)
            ) {
                items(totalItems) { index ->
                    val value = valueList[index.mod(valueList.size)]
                    val isSelected = index == centeredIndex
                    Box(modifier = Modifier.fillMaxWidth().height(itemHeight), contentAlignment = Alignment.Center) {
                        Text(
                            text = formatter(value),
                            style = if (isSelected) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(itemHeight)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
            )
        }
    }
}
@Composable
internal fun DateWheelDialog(
    title: String,
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit
) {
    var yearText by rememberSaveable(title, initialDate.toString()) { mutableStateOf(initialDate.year.toString()) }
    var month by rememberSaveable(title, initialDate.toString()) { mutableIntStateOf(initialDate.monthValue) }
    var day by rememberSaveable(title, initialDate.toString()) { mutableIntStateOf(initialDate.dayOfMonth) }
    val year = yearText.toIntOrNull()?.coerceIn(1900, 2100) ?: LocalDate.now().year
    val maxDay = remember(year, month) { YearMonth.of(year, month).lengthOfMonth() }
    LaunchedEffect(maxDay) {
        if (day > maxDay) day = maxDay
    }

    SimpleDialog(title = title, onDismiss = onDismiss) {
        OutlinedTextField(
            value = yearText,
            onValueChange = { input -> yearText = input.filter { it.isDigit() }.take(4) },
            label = { Text("年份") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            NumberWheel(1..12, month, { month = it }, "月", Modifier.weight(1f), cyclic = true, formatter = { it.toString() })
            NumberWheel(1..maxDay, day, { day = it.coerceAtMost(maxDay) }, "日", Modifier.weight(1f), cyclic = true, formatter = { it.toString() })
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("取消") }
            Button(
                onClick = { onConfirm(LocalDate.of(year, month, day.coerceAtMost(maxDay))) },
                modifier = Modifier.weight(1f),
                enabled = yearText.length == 4
            ) { Text("确认") }
        }
    }
}

@Composable
private fun RecordEditorDialog(
    title: String,
    record: RecordEntity?,
    records: List<RecordEntity>,
    sortedEvents: List<EventEntity>,
    eventsByGroup: Map<String, List<EventEntity>>,
    groups: List<GroupEntity>,
    groupById: Map<String, GroupEntity>,
    requireEndTime: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (String, Long, Long?) -> Unit
) {
    val groupIdsWithEvents = remember(groups, sortedEvents, eventsByGroup) {
        groupIdsWithEvents(groups, eventsByGroup, sortedEvents)
    }
    val date = remember(record?.startTime) { TimeUtils.toLocalDate(record?.startTime ?: TimeUtils.now()) }
    val startDateTime = remember(record?.startTime) { TimeUtils.toLocalDateTime(record?.startTime ?: TimeUtils.now()) }
    val endDateTime = remember(record?.endTime, record?.startTime) {
        record?.endTime?.let { TimeUtils.toLocalDateTime(it) } ?: startDateTime.plusHours(1)
    }
    val defaultEventId = record?.eventId?.takeIf { id -> sortedEvents.any { it.id == id } } ?: sortedEvents.firstOrNull()?.id.orEmpty()
    var selectedEventId by rememberSaveable(record?.id ?: "new", defaultEventId) { mutableStateOf(defaultEventId) }
    var selectedGroupId by rememberSaveable("${record?.id ?: "new"}_edit_group") { mutableStateOf(sortedEvents.firstOrNull { it.id == defaultEventId }?.groupId ?: sortedEvents.firstOrNull()?.groupId.orEmpty()) }
    var showingEventList by rememberSaveable("${record?.id ?: "new"}_edit_event_level") { mutableStateOf(false) }
    var startHour by rememberSaveable("${record?.id ?: "new"}_start_hour") { mutableIntStateOf(startDateTime.hour) }
    var startMinute by rememberSaveable("${record?.id ?: "new"}_start_minute") { mutableIntStateOf(startDateTime.minute) }
    var endHour by rememberSaveable("${record?.id ?: "new"}_end_hour") { mutableIntStateOf(endDateTime.hour) }
    var endMinute by rememberSaveable("${record?.id ?: "new"}_end_minute") { mutableIntStateOf(endDateTime.minute) }
    var keepRunning by rememberSaveable("${record?.id ?: "new"}_keep_running") { mutableStateOf(record?.endTime == null && !requireEndTime) }
    val selectedEvent = remember(selectedEventId, sortedEvents) { sortedEvents.firstOrNull { it.id == selectedEventId } }
    val selectedGroupName = selectedEvent?.let { groupById[it.groupId]?.name ?: "未分组" }.orEmpty()
    val crossesDay = !keepRunning && (endHour < startHour || (endHour == startHour && endMinute < startMinute))
    val canConfirm = selectedEventId.isNotBlank() && sortedEvents.isNotEmpty()
    val dialogMaxHeight = (LocalConfiguration.current.screenHeightDp * 0.86f).dp
    val recordStart = record?.startTime ?: TimeUtils.now()
    val previousEndPlusOne = remember(recordStart, records, date) {
        val previous = records
            .filter { it.endTime != null && it.id != record?.id }
            .filter { it.endTime!! < recordStart }
            .filter { TimeUtils.toLocalDate(it.endTime!!) == date }
            .maxByOrNull { it.endTime!! }
        previous?.endTime?.plus(60_000L)
    }
    val previousEndPlusOneUsable = remember(previousEndPlusOne, date) {
        previousEndPlusOne != null && TimeUtils.toLocalDate(previousEndPlusOne) == date
    }

    LaunchedEffect(sortedEvents) {
        if (sortedEvents.none { it.id == selectedEventId }) {
            val first = sortedEvents.firstOrNull()
            selectedEventId = first?.id.orEmpty()
            selectedGroupId = first?.groupId.orEmpty()
            showingEventList = false
        }
    }

    SimpleDialog(title = title, onDismiss = onDismiss) {
        Column(modifier = Modifier.heightIn(max = dialogMaxHeight)) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .stopParentScrollAtBounds().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("日期：$date", color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (sortedEvents.isEmpty()) {
                    Text("请先创建事件", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text("事件")
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = LocalComponentAlpha.current))) {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                            Text("当前选择", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                selectedEvent?.name ?: "未选择事件",
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (selectedGroupName.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(selectedGroupName, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }

                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = LocalComponentAlpha.current))) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 230.dp)
                                .stopParentScrollAtBounds().verticalScroll(rememberScrollState())
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (!showingEventList) {
                                Text("先选择分组", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                groupIdsWithEvents.forEach { groupId ->
                                    val group = groupById[groupId]
                                    val count = eventsByGroup[groupId].orEmpty().size
                                    TextButton(
                                        onClick = {
                                            selectedGroupId = groupId
                                            showingEventList = true
                                        },
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp)
                                    ) {
                                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(12.dp).background(colorFromArgb(group?.colorArgb ?: 0xFF9E9E9E.toInt()), shape = RoundedCornerShape(99.dp)))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(group?.name ?: "未分组", modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Start)
                                            Text("$count 个", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            } else {
                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(onClick = { showingEventList = false }, modifier = Modifier.weight(1f)) { Text("返回分组") }
                                    Text(groupById[selectedGroupId]?.name ?: "未分组", color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                eventsByGroup[selectedGroupId].orEmpty().forEach { event ->
                                    TextButton(
                                        onClick = { selectedEventId = event.id },
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp)
                                    ) {
                                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                            RadioButton(selected = selectedEventId == event.id, onClick = { selectedEventId = event.id })
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                if (event.isFavorite) "★ ${event.name}" else event.name,
                                                modifier = Modifier.weight(1f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Start
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Text("开始")
                    TimeAdjustRow(
                        startHour, { startHour = it }, startMinute, { startMinute = it },
                        extraLabel = "上条结束+1分",
                        extraEnabled = previousEndPlusOneUsable,
                        onExtraClick = {
                            previousEndPlusOne?.let { t ->
                                val ldt = TimeUtils.toLocalDateTime(t)
                                startHour = ldt.hour
                                startMinute = ldt.minute
                            }
                        }
                    )
                    TimePickerRow(startHour, { startHour = it }, startMinute, { startMinute = it })
                    if (record?.endTime == null && !requireEndTime) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Checkbox(checked = keepRunning, onCheckedChange = { keepRunning = it })
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("保持进行中")
                        }
                    }
                    if (!keepRunning) {
                        Text("结束")
                        TimeAdjustRow(endHour, { endHour = it }, endMinute, { endMinute = it })
                        TimePickerRow(endHour, { endHour = it }, endMinute, { endMinute = it })
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            if (crossesDay && sortedEvents.isNotEmpty()) {
                Text("结束时间早于开始时间，将在次日结束", color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) { Text("取消") }
                Button(
                    onClick = {
                        val start = date.atTime(LocalTime.of(startHour, startMinute)).atZone(TimeUtils.zoneId).toInstant().toEpochMilli()
                        val end = if (keepRunning) {
                            null
                        } else {
                            val endDate = if (crossesDay) date.plusDays(1) else date
                            endDate.atTime(LocalTime.of(endHour, endMinute)).atZone(TimeUtils.zoneId).toInstant().toEpochMilli()
                        }
                        if (selectedEventId.isNotBlank()) onConfirm(selectedEventId, start, end)
                    },
                    enabled = canConfirm,
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp)
                ) { Text("保存") }
            }
        }
    }
}

data class TimelineRecordUi(
    val record: RecordEntity,
    val startTime: Long,
    val endTime: Long,
    val lane: Int = 0,
    val laneCount: Int = 1,
    val hasNote: Boolean = false
)

private val eventUiComparator = compareByDescending<EventEntity> { it.isFavorite }.thenBy { it.sortOrder }.thenBy { it.name }

private fun List<GroupEntity>.visibleGroupsForUi(): List<GroupEntity> =
    filterNot { it.isDeleted }.sortedWith(compareBy<GroupEntity> { it.isSystem }.thenBy { it.sortOrder }.thenBy { it.name })

private fun List<EventEntity>.visibleEventsForUi(): List<EventEntity> = filterNot { it.isDeleted }

private fun List<EventEntity>.sortedForUi(eventRecordCounts: Map<String, Int> = emptyMap()): List<EventEntity> =
    sortedWith(compareByDescending<EventEntity> { eventRecordCounts[it.id] ?: 0 }.then(eventUiComparator))

private fun groupIdsWithEvents(
    groups: List<GroupEntity>,
    eventsByGroup: Map<String, List<EventEntity>>,
    sortedEvents: List<EventEntity>
): List<String> {
    val knownIds = groups.map { it.id }.toSet()
    val knownGroupIds = groups.filter { eventsByGroup[it.id].orEmpty().isNotEmpty() }.map { it.id }
    val missingGroupIds = sortedEvents.map { it.groupId }.distinct().filter { it !in knownIds }
    return knownGroupIds + missingGroupIds
}

private fun buildTimelineItems(
    records: List<RecordEntity>,
    dayStart: Long,
    dayEnd: Long,
    now: Long,
    notedRecordIds: Set<String>
): List<TimelineRecordUi> {
    val clipped = records.mapNotNull { record ->
        val actualEnd = record.endTime ?: now
        val clippedStart = maxOf(record.startTime, dayStart)
        val clippedEnd = minOf(actualEnd, dayEnd)
        if (actualEnd <= dayStart || record.startTime >= dayEnd || clippedEnd <= clippedStart) null
        else TimelineRecordUi(
            record = record,
            startTime = clippedStart,
            endTime = clippedEnd,
            hasNote = record.noteText.isNotBlank() || record.id in notedRecordIds
        )
    }.sortedBy { it.startTime }
    return positionTimelineRecords(clipped)
}

private fun positionTimelineRecords(items: List<TimelineRecordUi>): List<TimelineRecordUi> {
    val result = mutableListOf<TimelineRecordUi>()
    var index = 0
    while (index < items.size) {
        val cluster = mutableListOf<TimelineRecordUi>()
        var clusterEnd = items[index].endTime
        while (index < items.size && (cluster.isEmpty() || items[index].startTime < clusterEnd)) {
            val item = items[index]
            cluster += item
            if (item.endTime > clusterEnd) clusterEnd = item.endTime
            index++
        }
        val laneEnds = mutableListOf<Long>()
        val positioned = cluster.map { item ->
            val lane = laneEnds.indexOfFirst { it <= item.startTime }.let { if (it >= 0) it else laneEnds.size }
            if (lane == laneEnds.size) laneEnds += item.endTime else laneEnds[lane] = item.endTime
            item.copy(lane = lane)
        }
        val laneCount = laneEnds.size.coerceAtLeast(1)
        result += positioned.map { it.copy(laneCount = laneCount) }
    }
    return result.sortedBy { it.startTime }
}

private val stopParentScrollAtBoundsConnection = object : NestedScrollConnection {
    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset = Offset(0f, available.y)
    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity = Velocity(0f, available.y)
}

private fun Modifier.stopParentScrollAtBounds(enabled: Boolean = true): Modifier =
    if (enabled) nestedScroll(stopParentScrollAtBoundsConnection) else this

private fun Modifier.twoFingerVerticalZoom(onZoom: (Float) -> Unit): Modifier = pointerInput(onZoom) {
    awaitEachGesture {
        var wasTwoFingerGesture = false
        do {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val pressedChanges = event.changes.filter { it.pressed }
            if (pressedChanges.size >= 2) {
                wasTwoFingerGesture = true
                val first = pressedChanges[0]
                val second = pressedChanges[1]
                val previousDistanceY = abs(first.previousPosition.y - second.previousPosition.y)
                val currentDistanceY = abs(first.position.y - second.position.y)
                if (previousDistanceY > 8f && currentDistanceY > 8f) {
                    val zoom = currentDistanceY / previousDistanceY
                    if (zoom.isFinite() && abs(zoom - 1f) > 0.003f) {
                        onZoom(zoom)
                    }
                }
                for (change in event.changes) change.consume()
            } else if (wasTwoFingerGesture) {
                for (change in event.changes) change.consume()
            }
        } while (event.changes.any { it.pressed })
    }
}
@Composable
private fun TimelineDayView(
    items: List<TimelineRecordUi>,
    dayStart: Long,
    settings: AppSettings,
    now: Long,
    showNowLine: Boolean,
    verticalScale: Float,
    parentListState: LazyListState,
    onVerticalScaleChange: (Float) -> Unit,
    onRecordClick: (RecordEntity) -> Unit
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val scope = rememberCoroutineScope()
    var timelineTopInWindow by remember { mutableFloatStateOf(0f) }
    val scale = verticalScale.coerceIn(0.7f, 72f)
    val labelWidth = 54.dp
    val minuteHeight = 1.dp * scale
    val timelineHeight = minuteHeight * 1440f
    Card(
        modifier = Modifier
            .onGloballyPositioned { coordinates -> timelineTopInWindow = coordinates.positionInWindow().y }
            .twoFingerVerticalZoom { zoom ->
                val oldScale = scale
                val newScale = (oldScale * zoom).coerceIn(0.7f, 72f)
                if (abs(newScale - oldScale) > 0.001f) {
                    val oldMinuteHeightPx = with(density) { (1.dp * oldScale).toPx() }
                    val newMinuteHeightPx = with(density) { (1.dp * newScale).toPx() }
                    val topPaddingPx = with(density) { 8.dp.toPx() }
                    val screenCenterY = with(density) { configuration.screenHeightDp.dp.toPx() / 2f }
                    val centerYInTimeline = (screenCenterY - timelineTopInWindow - topPaddingPx).coerceIn(0f, oldMinuteHeightPx * 1440f)
                    val centerMinutes = if (oldMinuteHeightPx > 0f) centerYInTimeline / oldMinuteHeightPx else 0f
                    val scrollDelta = centerMinutes * (newMinuteHeightPx - oldMinuteHeightPx)
                    onVerticalScaleChange(newScale)
                    if (abs(scrollDelta) > 0.5f) {
                        scope.launch {
                            withFrameNanos { }
                            parentListState.scrollBy(scrollDelta)
                        }
                    }
                }
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = LocalComponentAlpha.current))
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(timelineHeight + 16.dp).padding(vertical = 8.dp)) {
            val contentWidth = maxWidth - labelWidth - 8.dp
            repeat(25) { hour ->
                val y = minuteHeight * (hour * 60f)
                Text(
                    text = hour.toString().padStart(2, '0') + ":00",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.offset(y = y).width(labelWidth),
                    textAlign = TextAlign.End
                )
                Box(
                    modifier = Modifier
                        .offset(x = labelWidth + 6.dp, y = y + 8.dp)
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
            items.forEach { item ->
                val topMinutes = ((item.startTime - dayStart).toDouble() / 60000.0).coerceIn(0.0, 1440.0)
                val durationMinutes = ((item.endTime - item.startTime).toDouble() / 60000.0).coerceAtLeast(1.0)
                val blockHeight = (minuteHeight * durationMinutes.toFloat()).let { if (it < 8.dp) 8.dp else it }
                val laneWidth = contentWidth / item.laneCount
                TimelineRecordBlock(
                    item = item,
                    settings = settings,
                    modifier = Modifier
                        .offset(x = labelWidth + 6.dp + laneWidth * item.lane, y = minuteHeight * topMinutes.toFloat())
                        .width(laneWidth - 4.dp)
                        .height(blockHeight),
                    blockHeight = blockHeight,
                    showDetails = blockHeight >= 24.dp,
                    onClick = { onRecordClick(item.record) }
                )
            }
            if (showNowLine) {
                val nowMinutes = ((now - dayStart).toDouble() / 60000.0).coerceIn(0.0, 1440.0)
                Box(
                    modifier = Modifier
                        .offset(x = labelWidth + 6.dp, y = minuteHeight * nowMinutes.toFloat())
                        .width(contentWidth)
                        .height(2.dp)
                        .background(Color.Red)
                )
                Text(
                    text = TimeUtils.formatTime(now, settings.use24Hour),
                    color = Color.Red,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.offset(x = 0.dp, y = minuteHeight * nowMinutes.toFloat() - 8.dp).width(labelWidth),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}@Composable
private fun TimelineRecordBlock(
    item: TimelineRecordUi,
    settings: AppSettings,
    modifier: Modifier,
    blockHeight: Dp,
    showDetails: Boolean,
    onClick: () -> Unit
) {
    val color = colorFromArgb(item.record.groupColorArgbSnapshot)
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.2f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.55f)),
        onClick = onClick
    ) {
        if (showDetails) {
            val noteText = remember(item.record.id, item.record.noteText, item.hasNote) {
                if (item.record.noteText.isNotBlank()) item.record.noteText
                else if (item.hasNote) "[\u56FE\u7247]" else ""
            }
            val density = LocalDensity.current
            val labelStyle = MaterialTheme.typography.labelSmall
            val lineHeightPx = with(density) { labelStyle.lineHeight.toPx() }
            val blockHeightPx = with(density) { blockHeight.toPx() }
            val verticalPaddingPx = with(density) { 3.dp.toPx() * 2 }
            val noteMaxLines = ((blockHeightPx - verticalPaddingPx - lineHeightPx) / lineHeightPx)
                .toInt()
                .coerceAtLeast(0)

            Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (item.hasNote) "\u2605 ${item.record.eventNameSnapshot}" else item.record.eventNameSnapshot,
                        style = labelStyle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        TimeUtils.formatTime(item.startTime, settings.use24Hour) + "\u2192" + TimeUtils.formatTime(item.endTime, settings.use24Hour),
                        style = labelStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (noteText.isNotBlank() && noteMaxLines > 0) {
                    Text(
                        noteText,
                        style = labelStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = noteMaxLines,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineBlockCard(
    item: TimelineRecordUi,
    settings: AppSettings,
    now: Long,
    onClick: () -> Unit
) {
    val duration = Duration.ofMillis(item.endTime - item.startTime)
    val durationMinutes = duration.toMinutes().toInt().coerceAtLeast(1)
    val blockHeight = (durationMinutes * 2).coerceIn(72, 220).dp
    val color = colorFromArgb(item.record.groupColorArgbSnapshot)
    Card(
        modifier = Modifier.fillMaxWidth().height(blockHeight),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.14f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(item.record.eventNameSnapshot, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (item.record.endTime == null) {
                    "${TimeUtils.formatClock(item.startTime, settings.showDateInClock, settings.use24Hour)} → 进行中"
                } else {
                    "${TimeUtils.formatClock(item.startTime, settings.showDateInClock, settings.use24Hour)} → ${TimeUtils.formatClock(item.endTime, settings.showDateInClock, settings.use24Hour)}"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "分组：${item.record.groupNameSnapshot} · ${if (item.record.endTime == null) formatRunning(item.record.startTime, now) else formatDuration(duration)}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun SimpleDialog(
    title: String,
    onDismiss: () -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    titleStyle: TextStyle = MaterialTheme.typography.titleMedium,
    titleAlign: TextAlign = TextAlign.Start,
    titleBackgroundColor: Color? = null,
    titleContentColor: Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.widthIn(min = 280.dp, max = 420.dp),
            colors = CardDefaults.cardColors(containerColor = containerColor)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (titleBackgroundColor != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(titleBackgroundColor)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            title,
                            style = titleStyle,
                            textAlign = TextAlign.Center,
                            color = titleContentColor,
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Text(
                        title,
                        style = titleStyle,
                        textAlign = titleAlign,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                content()
            }
        }
    }
}
@Composable
private fun produceClock(): Long {
    val state = remember { androidx.compose.runtime.mutableLongStateOf(TimeUtils.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            state.longValue = TimeUtils.now()
            delay(1000)
        }
    }
    return state.longValue
}

internal fun colorFromArgb(argb: Int): Color = Color(argb)

private fun formatDuration(duration: Duration): String {
    val totalSeconds = duration.seconds.coerceAtLeast(0)
    val days = totalSeconds / 86_400
    val hours = (totalSeconds % 86_400) / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return buildString {
        if (days > 0) append(days).append("天")
        if (hours > 0 || days > 0) append(hours).append("时")
        if (minutes > 0 || hours > 0 || days > 0) append(minutes).append("分")
        append(seconds).append("秒")
    }
}

private fun formatDurationToMinute(duration: Duration): String {
    val totalMinutes = duration.toMinutes().coerceAtLeast(0)
    val days = totalMinutes / 1440
    val hours = (totalMinutes % 1440) / 60
    val minutes = totalMinutes % 60
    return buildString {
        if (days > 0) append(days).append("天")
        if (hours > 0 || days > 0) append(hours).append("时")
        append(minutes).append("分")
    }
}

private fun formatRunning(startTime: Long, now: Long = TimeUtils.now()): String = formatDuration(Duration.ofMillis(now - startTime))

private fun timelineSubtitle(record: RecordEntity, settings: AppSettings): String {
    return if (record.endTime == null) {
        "进行中 · ${TimeUtils.formatClock(record.startTime, settings.showDateInClock, settings.use24Hour)} · ${formatRunning(record.startTime)}"
    } else {
        "${TimeUtils.formatClock(record.startTime, settings.showDateInClock, settings.use24Hour)} → ${TimeUtils.formatClock(record.endTime, settings.showDateInClock, settings.use24Hour)}"
    }
}


































