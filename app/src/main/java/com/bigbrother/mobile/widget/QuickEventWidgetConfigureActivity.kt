@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.bigbrother.mobile.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bigbrother.mobile.BigBrotherApp
import com.bigbrother.mobile.data.AppSettings
import com.bigbrother.mobile.data.EventEntity
import com.bigbrother.mobile.data.GroupEntity
import com.bigbrother.mobile.ui.BigBrotherTheme

class QuickEventWidgetConfigureActivity : ComponentActivity() {
    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    private var gridMode: Boolean = false
    private var gridSlotIndex: Int = -1
    private var gridManagementMode: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        setResult(Activity.RESULT_CANCELED)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val providerClassName = AppWidgetManager.getInstance(this)
            .getAppWidgetInfo(appWidgetId)
            ?.provider
            ?.className
        gridMode = intent.getBooleanExtra(QuickEventWidgetConfigContract.EXTRA_GRID_MODE, false) ||
            providerClassName == QuickEventGridWidgetProvider::class.java.name
        gridSlotIndex = intent.getIntExtra(
            QuickEventWidgetConfigContract.EXTRA_GRID_SLOT_INDEX,
            -1
        )
        gridManagementMode = gridMode && (
            intent.getBooleanExtra(QuickEventWidgetConfigContract.EXTRA_GRID_MANAGEMENT, false) ||
                gridSlotIndex !in 0 until QuickEventWidgetStore.GRID_SLOT_COUNT
            )

        val repository = (application as BigBrotherApp).container.repository
        val initialEventId = when {
            !gridMode -> QuickEventWidgetStore.eventId(this, appWidgetId)
            gridSlotIndex in 0 until QuickEventWidgetStore.GRID_SLOT_COUNT ->
                QuickEventWidgetStore.gridEventId(this, appWidgetId, gridSlotIndex)
            else -> null
        }
        val initialGridEventIds = if (gridMode) {
            QuickEventWidgetStore.gridEventIds(this, appWidgetId)
        } else {
            emptyList()
        }

        setContent {
            LaunchedEffect(Unit) { repository.initialize() }
            val settings by repository.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
            val groups by repository.groups.collectAsStateWithLifecycle(initialValue = emptyList())
            val events by repository.events.collectAsStateWithLifecycle(initialValue = emptyList())
            BigBrotherTheme(settings = settings) {
                if (gridManagementMode) {
                    QuickEventGridWidgetManagementScreen(
                        groups = groups,
                        events = events,
                        initialEventIds = initialGridEventIds,
                        onCancel = { finish() },
                        onSave = ::saveGridEvents
                    )
                } else {
                    QuickEventWidgetConfigureScreen(
                        groups = groups,
                        events = events,
                        initialEventId = initialEventId,
                        onCancel = { finish() },
                        onSave = ::saveEvent,
                        title = if (gridMode) "选择小组件事件" else "设置桌面小组件",
                        saveLabel = if (gridMode) "保存" else "添加"
                    )
                }
            }
        }
    }

    private fun saveEvent(eventId: String) {
        if (gridMode && gridSlotIndex in 0 until QuickEventWidgetStore.GRID_SLOT_COUNT) {
            QuickEventWidgetStore.saveGridEventId(this, appWidgetId, gridSlotIndex, eventId)
        } else {
            QuickEventWidgetStore.saveEventId(this, appWidgetId, eventId)
        }
        QuickEventWidgetProvider.requestRefresh(this)
        val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(Activity.RESULT_OK, result)
        finish()
    }

    private fun saveGridEvents(eventIds: List<String?>) {
        QuickEventWidgetStore.saveGridEventIds(this, appWidgetId, eventIds)
        QuickEventWidgetProvider.requestRefresh(this)
        val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(Activity.RESULT_OK, result)
        finish()
    }
}

@Composable
private fun QuickEventGridWidgetManagementScreen(
    groups: List<GroupEntity>,
    events: List<EventEntity>,
    initialEventIds: List<String?>,
    onCancel: () -> Unit,
    onSave: (List<String?>) -> Unit
) {
    var slotEventIds by remember(initialEventIds) { mutableStateOf(initialEventIds.toList()) }
    var editingSlotIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    val visibleEvents = remember(events) { events.filterNot { it.isDeleted } }
    val groupById = remember(groups) { groups.associateBy { it.id } }
    val selectedSlot = editingSlotIndex

    if (selectedSlot != null) {
        QuickEventWidgetConfigureScreen(
            groups = groups,
            events = events,
            initialEventId = slotEventIds.getOrNull(selectedSlot),
            onCancel = { editingSlotIndex = null },
            onSave = { eventId ->
                val updated = slotEventIds.toMutableList()
                while (updated.size < QuickEventWidgetStore.GRID_SLOT_COUNT) {
                    updated += null
                }
                updated[selectedSlot] = eventId
                slotEventIds = updated
                editingSlotIndex = null
            },
            title = "选择第 ${selectedSlot + 1} 个事件",
            saveLabel = "保存"
        )
    } else {
        Scaffold(
            topBar = { TopAppBar(title = { Text("编辑 4×2 小组件") }) }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "设置 7 个事件格，右下角固定为编辑",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items((0 until QuickEventWidgetStore.GRID_SLOT_COUNT).toList()) { slotIndex ->
                        val eventId = slotEventIds.getOrNull(slotIndex)
                        val event = visibleEvents.firstOrNull { it.id == eventId }
                        val group = event?.let { groupById[it.groupId] }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { editingSlotIndex = slotIndex },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(
                                            color = group?.let { Color(it.colorArgb) }
                                                ?: MaterialTheme.colorScheme.outline,
                                            shape = RoundedCornerShape(99.dp)
                                        )
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "事件 ${slotIndex + 1}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = when {
                                            event != null -> event.name
                                            eventId != null -> "事件不可用"
                                            else -> "未分配"
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                TextButton(onClick = { editingSlotIndex = slotIndex }) {
                                    Text(if (event == null) "选择" else "更换")
                                }
                                if (eventId != null) {
                                    TextButton(
                                        onClick = {
                                            val updated = slotEventIds.toMutableList()
                                            while (updated.size < QuickEventWidgetStore.GRID_SLOT_COUNT) {
                                                updated += null
                                            }
                                            updated[slotIndex] = null
                                            slotEventIds = updated
                                        }
                                    ) {
                                        Text("清空")
                                    }
                                }
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                        Text("取消")
                    }
                    Button(onClick = { onSave(slotEventIds) }, modifier = Modifier.weight(1f)) {
                        Text("完成")
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickEventWidgetConfigureScreen(
    groups: List<GroupEntity>,
    events: List<EventEntity>,
    initialEventId: String?,
    onCancel: () -> Unit,
    onSave: (String) -> Unit,
    title: String = "设置桌面小组件",
    saveLabel: String = "添加"
) {
    val visibleGroups = remember(groups) {
        groups.filterNot { it.isDeleted }
            .sortedWith(compareBy<GroupEntity> { it.isSystem }.thenBy { it.sortOrder }.thenBy { it.name })
    }
    val visibleEvents = remember(events) { events.filterNot { it.isDeleted } }
    val initialEvent = remember(initialEventId, visibleEvents) {
        visibleEvents.firstOrNull { it.id == initialEventId }
    }
    var selectedGroupId by rememberSaveable(initialEvent?.groupId) {
        mutableStateOf(initialEvent?.groupId ?: visibleGroups.firstOrNull()?.id)
    }
    var selectedEventId by rememberSaveable(initialEvent?.id) {
        mutableStateOf(initialEvent?.id)
    }
    val groupEvents = remember(selectedGroupId, visibleEvents) {
        visibleEvents.filter { it.groupId == selectedGroupId }
            .sortedWith(compareByDescending<EventEntity> { it.isFavorite }.thenBy { it.sortOrder }.thenBy { it.name })
    }

    LaunchedEffect(visibleGroups, initialEvent?.groupId) {
        if (selectedGroupId == null || visibleGroups.none { it.id == selectedGroupId }) {
            selectedGroupId = initialEvent?.groupId ?: visibleGroups.firstOrNull()?.id
        }
    }

    LaunchedEffect(selectedGroupId, groupEvents) {
        if (selectedEventId == null || groupEvents.none { it.id == selectedEventId }) {
            selectedEventId = groupEvents.firstOrNull()?.id
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(title) }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("选择分组", style = MaterialTheme.typography.titleMedium)
            if (visibleGroups.isEmpty()) {
                Text("暂无可用分组", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 190.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(visibleGroups, key = { it.id }) { group ->
                        GroupChoiceRow(
                            group = group,
                            selected = group.id == selectedGroupId,
                            onClick = {
                                selectedGroupId = group.id
                                if (selectedEventId == null || visibleEvents.none {
                                        it.groupId == group.id && it.id == selectedEventId
                                    }
                                ) {
                                    selectedEventId = null
                                }
                            }
                        )
                    }
                }
            }

            Text("选择事件", style = MaterialTheme.typography.titleMedium)
            if (selectedGroupId == null || groupEvents.isEmpty()) {
                Text("该分组暂无事件", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(groupEvents, key = { it.id }) { event ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedEventId = event.id },
                            colors = CardDefaults.cardColors(
                                containerColor = if (event.id == selectedEventId) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerLow
                                }
                            )
                        ) {
                            Text(
                                text = event.name,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Text("取消")
                }
                Button(
                    onClick = { selectedEventId?.let(onSave) },
                    enabled = selectedEventId != null,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(saveLabel)
                }
            }
        }
    }
}

@Composable
private fun GroupChoiceRow(
    group: GroupEntity,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(Color(group.colorArgb), RoundedCornerShape(99.dp))
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(group.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}