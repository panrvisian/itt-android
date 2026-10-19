@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.bigbrother.mobile.ui

import android.app.DatePickerDialog
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PlayArrow
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.CardDefaults as MiuixCardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bigbrother.mobile.R
import com.bigbrother.mobile.data.EventEntity
import com.bigbrother.mobile.data.GroupEntity
import com.bigbrother.mobile.data.NoteViewState
import com.bigbrother.mobile.data.RecordEntity
import com.bigbrother.mobile.data.TaskEntity
import com.bigbrother.mobile.data.TaskType
import com.bigbrother.mobile.domain.TimeUtils
import top.yukonga.miuix.kmp.utils.overScrollVertical
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

@Composable
fun NotesScreen(
    viewModel: MainViewModel,
    notedRecords: List<RecordEntity>,
    imageRecordIds: Set<String>,
    groups: List<GroupEntity>,
    events: List<EventEntity>,
    tasks: List<TaskEntity>,
    contentActive: Boolean,
    contentLoaded: Boolean,
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    onOpen: (RecordEntity) -> Unit,
    onRegisterOnboardingTarget: (OnboardingTarget, Rect) -> Unit
) {
    var selectedSection by rememberSaveable { mutableIntStateOf(0) }
    var showCreateTaskDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TaskEntity?>(null) }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = statusBarTop + 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            KernelSegmentedControl(
                labels = listOf("待办清单", "打卡备注"),
                selectedIndex = selectedSection,
                onSelected = { selectedSection = it }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (selectedSection == 0) {
                TasksSection(
                    viewModel = viewModel,
                    events = events,
                    tasks = tasks,
                    onAddTask = { showCreateTaskDialog = true },
                    onEditTask = { editingTask = it }
                )
            } else {
                NotesSection(
                    notedRecords = notedRecords,
                    imageRecordIds = imageRecordIds,
                    contentActive = contentActive,
                    contentLoaded = contentLoaded,
                    date = date,
                    onDateChange = onDateChange,
                    onOpen = onOpen,
                    statusBarTop = 0.dp,
                    onRegisterOnboardingTarget = onRegisterOnboardingTarget
                )
            }
        }
    }

    if (showCreateTaskDialog) {
        val existingLocations = remember(tasks) { tasks.mapNotNull { it.location }.filter { it.isNotBlank() }.distinct() }
        val existingPeople = remember(tasks) { tasks.mapNotNull { it.person }.filter { it.isNotBlank() }.distinct() }

        TaskEditDialog(
            groups = groups,
            events = events,
            existingLocations = existingLocations,
            existingPeople = existingPeople,
            onDismiss = { showCreateTaskDialog = false },
            onConfirm = { title, eventId, location, person, taskType, targetDateEpochDay ->
                showCreateTaskDialog = false
                viewModel.addTask(title, eventId, location, person, taskType, targetDateEpochDay)
            }
        )
    }

    if (editingTask != null) {
        val existingLocations = remember(tasks) { tasks.mapNotNull { it.location }.filter { it.isNotBlank() }.distinct() }
        val existingPeople = remember(tasks) { tasks.mapNotNull { it.person }.filter { it.isNotBlank() }.distinct() }

        TaskEditDialog(
            initialTask = editingTask,
            groups = groups,
            events = events,
            existingLocations = existingLocations,
            existingPeople = existingPeople,
            onDismiss = { editingTask = null },
            onConfirm = { title, eventId, location, person, taskType, targetDateEpochDay ->
                val taskId = editingTask!!.id
                editingTask = null
                viewModel.updateTask(taskId, title, eventId, location, person, taskType, targetDateEpochDay)
            }
        )
    }
}

@Composable
private fun TasksSection(
    viewModel: MainViewModel,
    events: List<EventEntity>,
    tasks: List<TaskEntity>,
    onAddTask: () -> Unit,
    onEditTask: (TaskEntity) -> Unit
) {
    val eventMap = remember(events) { events.associateBy { it.id } }
    val (pendingTasks, completedTasks) = remember(tasks) {
        tasks.partition { !it.isCompleted }
    }
    var showCompleted by rememberSaveable { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .overScrollVertical(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 8.dp,
            end = 16.dp,
            bottom = LocalMainBottomBarPadding.current
        )
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "我的待办 (${pendingTasks.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Button(
                    onClick = onAddTask,
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("新建待办")
                }
            }
        }

        if (pendingTasks.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = LocalComponentAlpha.current))
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暂无未完成待办，点击右上角新建",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(pendingTasks, key = { it.id }) { task ->
                val event = task.eventId?.let { eventMap[it] }
                TaskItemCard(
                    task = task,
                    event = event,
                    onClick = { onEditTask(task) },
                    onToggle = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.toggleTaskCompleted(task.id)
                    },
                    onStartEvent = {
                        viewModel.startTaskAsEvent(task.id)
                    },
                    onDelete = {
                        viewModel.deleteTask(task.id)
                    }
                )
            }
        }

        if (completedTasks.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showCompleted = !showCompleted }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "已完成 (${completedTasks.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = if (showCompleted) "收起 ▴" else "展开 ▾",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (showCompleted) {
                items(completedTasks, key = { it.id }) { task ->
                    val event = task.eventId?.let { eventMap[it] }
                    TaskItemCard(
                        task = task,
                        event = event,
                        onClick = { onEditTask(task) },
                        onToggle = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.toggleTaskCompleted(task.id)
                        },
                        onStartEvent = {
                            viewModel.startTaskAsEvent(task.id)
                        },
                        onDelete = {
                            viewModel.deleteTask(task.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
internal fun TaskItemCard(
    task: TaskEntity,
    event: EventEntity?,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onStartEvent: () -> Unit,
    onDelete: () -> Unit
) {
    val today = remember { LocalDate.now() }
    val targetDate = remember(task.targetDateEpochDay) {
        task.targetDateEpochDay?.let { LocalDate.ofEpochDay(it) }
    }
    val isOverdue = remember(targetDate, task.isCompleted, task.taskType) {
        task.taskType == TaskType.LongTerm && targetDate != null && !task.isCompleted && today.isAfter(targetDate)
    }

    val cardBg = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = (MaterialTheme.colorScheme.surfaceContainerLow.alpha * LocalComponentAlpha.current).coerceIn(0f, 1f))
    val borderColor = if (isOverdue) MaterialTheme.colorScheme.error.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)

    MiuixCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(BorderStroke(1.dp, borderColor), shape = RoundedCornerShape(22.dp)),
        cornerRadius = 22.dp,
        colors = MiuixCardDefaults.defaultColors(
            color = cardBg,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggle() }
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    fontWeight = if (task.isCompleted) FontWeight.Normal else FontWeight.SemiBold,
                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (event != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "📌 ${event.name}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (!task.location.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest
                        ) {
                            Text(
                                text = "📍 ${task.location}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (!task.person.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest
                        ) {
                            Text(
                                text = "👤 ${task.person}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (task.taskType == TaskType.LongTerm && targetDate != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isOverdue) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHighest
                        ) {
                            Text(
                                text = if (isOverdue) "⚠️ 截止 $targetDate (已逾期)" else "⏰ 截止 $targetDate",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isOverdue) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else if (task.taskType == TaskType.ShortTerm && targetDate != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest
                        ) {
                            Text(
                                text = "📅 预计 $targetDate",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onStartEvent,
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "启动打卡",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "删除待办",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun TaskEditDialog(
    initialTask: TaskEntity? = null,
    groups: List<GroupEntity>,
    events: List<EventEntity>,
    existingLocations: List<String>,
    existingPeople: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        eventId: String?,
        location: String?,
        person: String?,
        taskType: TaskType,
        targetDateEpochDay: Long?
    ) -> Unit
) {
    var title by remember(initialTask) { mutableStateOf(initialTask?.title.orEmpty()) }
    var selectedEventId by remember(initialTask) { mutableStateOf(initialTask?.eventId) }
    var location by remember(initialTask) { mutableStateOf(initialTask?.location.orEmpty()) }
    var person by remember(initialTask) { mutableStateOf(initialTask?.person.orEmpty()) }
    var taskType by remember(initialTask) { mutableStateOf(initialTask?.taskType ?: TaskType.Normal) }
    var targetDate by remember(initialTask) {
        mutableStateOf(initialTask?.targetDateEpochDay?.let { LocalDate.ofEpochDay(it) } ?: LocalDate.now().plusDays(1))
    }
    var showDatePicker by remember { mutableStateOf(false) }

    val groupById = remember(groups) { groups.associateBy { it.id } }

    val isEdit = initialTask != null
    val dialogTitle = if (isEdit) "编辑待办 / 备忘" else "新建待办 / 备忘"
    val confirmButtonText = if (isEdit) "保存修改" else "创建"

    val context = LocalContext.current

    if (showDatePicker) {
        val datePicker = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                targetDate = LocalDate.of(year, month + 1, dayOfMonth)
                showDatePicker = false
            },
            targetDate.year,
            targetDate.monthValue - 1,
            targetDate.dayOfMonth
        )
        DisposableEffect(Unit) {
            datePicker.show()
            datePicker.setOnDismissListener { showDatePicker = false }
            onDispose { datePicker.dismiss() }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = dialogTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("输入待办/备忘内容，如：整理学术论文资料") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("关联事件库（打卡时自动记录）：", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                UnifiedEventSelector(
                    selectedEventId = selectedEventId,
                    sortedEvents = events,
                    groups = groups,
                    groupById = groupById,
                    allowUnlinked = true,
                    onEventSelect = { selectedEventId = it }
                )

                Text("任务属性与时间：", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                KernelSegmentedControl(
                    labels = listOf("普通", "长期(截止)", "短期(预计)"),
                    selectedIndex = when (taskType) {
                        TaskType.LongTerm -> 1
                        TaskType.ShortTerm -> 2
                        else -> 0
                    },
                    onSelected = { index ->
                        taskType = when (index) {
                            1 -> TaskType.LongTerm
                            2 -> TaskType.ShortTerm
                            else -> TaskType.Normal
                        }
                    }
                )

                if (taskType != TaskType.Normal) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (taskType == TaskType.LongTerm) "截止日期：" else "预计完成日期：",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        OutlinedButton(onClick = { showDatePicker = true }) {
                            Text(targetDate.toString(), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Text("关联地点：", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    placeholder = { Text("如：图书馆 / 办公室 / 健身房") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (existingLocations.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        existingLocations.forEach { loc ->
                            FilterChip(
                                selected = location == loc,
                                onClick = { location = loc },
                                label = { Text(loc) }
                            )
                        }
                    }
                }

                Text("关联人物：", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = person,
                    onValueChange = { person = it },
                    placeholder = { Text("如：张三 / 导师 / 组长") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (existingPeople.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        existingPeople.forEach { p ->
                            FilterChip(
                                selected = person == p,
                                onClick = { person = p },
                                label = { Text(p) }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onConfirm(
                                    title,
                                    selectedEventId,
                                    location.ifBlank { null },
                                    person.ifBlank { null },
                                    taskType,
                                    if (taskType != TaskType.Normal) targetDate.toEpochDay() else null
                                )
                            }
                        },
                        enabled = title.isNotBlank()
                    ) {
                        Text(confirmButtonText)
                    }
                }
            }
        }
    }
}

@Composable
private fun NotesSection(
    notedRecords: List<RecordEntity>,
    imageRecordIds: Set<String>,
    contentActive: Boolean,
    contentLoaded: Boolean,
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    onOpen: (RecordEntity) -> Unit,
    statusBarTop: Dp,
    onRegisterOnboardingTarget: (OnboardingTarget, Rect) -> Unit
) {
    var calendarExpanded by rememberSaveable { mutableStateOf(false) }
    val calculationKey = remember(notedRecords, date) { Any() }
    var calculation by remember { mutableStateOf<Pair<Any, List<RecordEntity>>?>(null) }
    LaunchedEffect(contentLoaded, calculationKey) {
        if (!contentLoaded) return@LaunchedEffect
        val recordsForDay = withContext(Dispatchers.Default) {
            notedRecords.filter { TimeUtils.toLocalDate(it.startTime) == date }
        }
        calculation = calculationKey to recordsForDay
    }
    val dayRecords = calculation?.takeIf { it.first === calculationKey }?.second

    Column(modifier = Modifier.fillMaxSize()) {
        MiuixCalendarHeader(
            selectedDate = date,
            mode = CalendarHeaderMode.Day,
            expanded = calendarExpanded,
            onExpandedChange = { calendarExpanded = it },
            onDateSelected = onDateChange,
            modifier = Modifier
                .onGloballyPositioned { coordinates ->
                    onRegisterOnboardingTarget(OnboardingTarget.NotesControls, coordinates.boundsInRoot())
                }
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .collapseCalendarOnBodyInteraction(calendarExpanded) { calendarExpanded = false }
                .overScrollVertical(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp,
                top = 8.dp,
                end = 16.dp,
                bottom = LocalMainBottomBarPadding.current
            )
        ) {
            if (!contentActive) {
                item { Text("打开备注页后加载记录", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else if (!contentLoaded) {
                item { Text("正在构建备注索引…", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else if (dayRecords == null) {
                item { Text("正在加载备注…", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else if (dayRecords.isEmpty()) {
                item { Text("这一天没有备注", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(dayRecords, key = { it.id }) { record ->
                    NoteListRow(
                        record = record,
                        isImageOnly = record.noteText.isBlank() && record.id in imageRecordIds,
                        onClick = { onOpen(record) }
                    )
                }
            }
        }
    }
}

@Composable
internal fun NoteListRow(
    record: RecordEntity,
    isImageOnly: Boolean,
    onClick: () -> Unit
) {
    val preview = if (isImageOnly) {
        "[图片]"
    } else {
        record.noteText.replace('\n', ' ').replace('\r', ' ').trim()
    }
    val titleAnnotated = buildAnnotatedString {
        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = MaterialTheme.typography.bodyLarge.fontSize)) {
            append(record.eventNameSnapshot)
        }
        if (preview.isNotBlank()) {
            withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = MaterialTheme.typography.bodySmall.fontSize)) {
                append(" · ")
                append(preview)
            }
        }
    }
    val cardBg = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = (MaterialTheme.colorScheme.surfaceContainerLow.alpha * LocalComponentAlpha.current).coerceIn(0f, 1f))

    MiuixCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)), shape = RoundedCornerShape(22.dp)),
        cornerRadius = 22.dp,
        colors = MiuixCardDefaults.defaultColors(
            color = cardBg,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(width = 8.dp, height = 44.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(colorFromArgb(record.groupColorArgbSnapshot))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    titleAnnotated,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "开始 ${TimeUtils.formatDateTime(record.startTime)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun NoteViewDialog(
    record: RecordEntity,
    viewModel: MainViewModel,
    onEdit: () -> Unit,
    onDismiss: () -> Unit
) {
    var state by remember { mutableStateOf(NoteViewState("", emptyList())) }
    LaunchedEffect(record.id) {
        state = viewModel.loadNoteView(record.id)
    }
    val color = colorFromArgb(record.groupColorArgbSnapshot)
    SimpleDialog(
        title = record.eventNameSnapshot,
        onDismiss = onDismiss,
        titleStyle = MaterialTheme.typography.headlineSmall,
        titleAlign = TextAlign.Center,
        titleBackgroundColor = color,
        titleContentColor = if (color.luminance() > 0.5f) Color.Black else Color.White
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(
                modifier = Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()).overScrollVertical(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.text.isNotBlank()) {
                    Text(state.text, style = MaterialTheme.typography.bodyLarge)
                }
                if (state.imageNames.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.imageNames.forEach { name ->
                            val file = viewModel.noteImageFile(record.id, name)
                            val bitmap = rememberFileImage(file)
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                )
                            }
                        }
                    }
                }
                if (state.text.isBlank() && state.imageNames.isEmpty()) {
                    Text("暂无备注内容", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("修改备注")
                }

                OutlinedButton(
                    onClick = {
                        viewModel.clearNote(record.id)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "一键删除",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("一键删除")
                }
            }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("关闭") }
        }
    }
}

@Composable
fun NoteEditorDialog(
    record: RecordEntity,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    var imageNames by remember { mutableStateOf<List<String>>(emptyList()) }
    var fullscreen by rememberSaveable { mutableStateOf(false) }
    var committed by remember { mutableStateOf(false) }
    var showImageSource by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(record.id) {
        val state = viewModel.loadNoteEditor(record.id)
        text = state.text
        imageNames = state.imageNames
    }

    DisposableEffect(record.id) {
        onDispose {
            if (!committed) {
                val t = text
                val imgs = imageNames
                scope.launch { viewModel.saveNoteDraft(record.id, t, imgs) }
            }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                uris.forEach { uri ->
                    val name = viewModel.copyNoteImageToDraft(record.id, uri)
                    if (name != null) imageNames = imageNames + name
                }
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = pendingCameraUri
        pendingCameraUri = null
        if (uri != null) {
            if (success) {
                scope.launch {
                    val name = viewModel.copyNoteImageToDraft(record.id, uri)
                    if (name != null) imageNames = imageNames + name
                    runCatching { context.contentResolver.delete(uri, null, null) }
                }
            } else {
                runCatching { context.contentResolver.delete(uri, null, null) }
            }
        }
    }

    val takePhoto = {
        val tempFile = runCatching { File.createTempFile("note_", ".jpg", context.cacheDir) }.getOrNull()
        if (tempFile != null) {
            val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", tempFile)
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        }
    }

    if (fullscreen) {
        FullscreenNoteEditor(
            text = text,
            onTextChange = { text = it },
            onShrink = { fullscreen = false }
        )
    } else {
        SimpleDialog(title = record.eventNameSnapshot, onDismiss = onDismiss) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { fullscreen = true }) { Text("展开") }
                }
                Box {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
                        placeholder = { Text("输入备注内容") },
                        minLines = 1
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .height(20.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { showImageSource = true }) {
                        Icon(painterResource(R.drawable.ic_add), contentDescription = "添加图片", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("添加图片")
                    }
                }
                if (imageNames.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        imageNames.forEach { name ->
                            val file = viewModel.draftImageFile(record.id, name)
                            val bitmap = rememberFileImage(file)
                            Box {
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(80.dp).clip(RoundedCornerShape(10.dp))
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        imageNames = imageNames - name
                                        scope.launch { viewModel.removeDraftImageFile(record.id, name) }
                                    },
                                    modifier = Modifier.align(Alignment.TopEnd).size(24.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_delete),
                                        contentDescription = "删除图片",
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) { Text("取消") }
                    Button(
                        onClick = {
                            scope.launch {
                                viewModel.saveNote(record.id, text, imageNames)
                                committed = true
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1f).heightIn(min = 52.dp)
                    ) { Text("保存") }
                }
            }
        }
    }

    if (showImageSource) {
        SimpleDialog(title = "添加图片", onDismiss = { showImageSource = false }) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = {
                        showImageSource = false
                        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("从相册选择") }
                TextButton(
                    onClick = {
                        showImageSource = false
                        takePhoto()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("拍照") }
                TextButton(onClick = { showImageSource = false }, modifier = Modifier.fillMaxWidth()) { Text("取消") }
            }
        }
    }
}

@Composable
private fun FullscreenNoteEditor(
    text: String,
    onTextChange: (String) -> Unit,
    onShrink: () -> Unit
) {
    Dialog(
        onDismissRequest = onShrink,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp),
                    placeholder = { Text("输入备注内容") }
                )
                Row(
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onShrink, modifier = Modifier.padding(12.dp)) { Text("收起") }
                }
            }
        }
    }
}

@Composable
private fun rememberFileImage(file: File?): ImageBitmap? {
    val bitmap by produceState<ImageBitmap?>(null, file?.absolutePath) {
        value = null
        if (file != null && file.exists()) {
            value = withContext(Dispatchers.IO) {
                runCatching { BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() }.getOrNull()
            }
        }
    }
    return bitmap
}
