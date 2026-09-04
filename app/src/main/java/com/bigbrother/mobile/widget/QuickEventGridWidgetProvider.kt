package com.bigbrother.mobile.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.bigbrother.mobile.BigBrotherApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** 4×2 widget made from seven shared 1×1 event cells and one edit cell. */
class QuickEventGridWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        launchAsync {
            updateWidgets(context, appWidgetManager, appWidgetIds)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_TOGGLE_SLOT -> {
                val appWidgetId = intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID
                )
                val slotIndex = intent.getIntExtra(
                    QuickEventWidgetConfigContract.EXTRA_GRID_SLOT_INDEX,
                    -1
                )
                if (
                    appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID &&
                    slotIndex in 0 until QuickEventWidgetStore.GRID_SLOT_COUNT
                ) {
                    launchAsync {
                        toggleEvent(context, appWidgetId, slotIndex)
                        updateAllWidgets(context)
                    }
                }
            }

            ACTION_REFRESH -> {
                launchAsync {
                    updateAllWidgets(context)
                }
            }

            else -> super.onReceive(context, intent)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach { QuickEventWidgetStore.remove(context, it) }
        super.onDeleted(context, appWidgetIds)
    }

    override fun onRestored(context: Context, oldWidgetIds: IntArray, newWidgetIds: IntArray) {
        oldWidgetIds.forEachIndexed { index, oldId ->
            val newId = newWidgetIds.getOrNull(index) ?: return@forEachIndexed
            QuickEventWidgetStore.copy(context, oldId, newId)
            QuickEventWidgetStore.remove(context, oldId)
        }
        super.onRestored(context, oldWidgetIds, newWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        launchAsync {
            updateWidgets(context, appWidgetManager, intArrayOf(appWidgetId))
        }
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    private fun launchAsync(block: suspend () -> Unit) {
        val pendingResult = goAsync()
        scope.launch {
            try {
                block()
            } catch (_: Throwable) {
                // Widget updates must not crash the host process.
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun toggleEvent(context: Context, appWidgetId: Int, slotIndex: Int) {
        val eventId = QuickEventWidgetStore.gridEventId(context, appWidgetId, slotIndex) ?: return
        val repository = (context.applicationContext as BigBrotherApp).container.repository
        repository.toggleEvent(eventId)
    }

    private suspend fun updateAllWidgets(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, QuickEventGridWidgetProvider::class.java)
        val ids = manager.getAppWidgetIds(component)
        updateWidgets(context, manager, ids)
    }

    private suspend fun updateWidgets(
        context: Context,
        manager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        if (appWidgetIds.isEmpty()) return
        val repository = (context.applicationContext as BigBrotherApp).container.repository
        val events = repository.events.first()
        val groups = repository.groups.first()
        val records = repository.records.first()

        appWidgetIds.forEach { appWidgetId ->
            val views = RemoteViews(context.packageName, com.bigbrother.mobile.R.layout.widget_quick_event_grid)
            val configuredEventIds = QuickEventWidgetStore.gridEventIds(context, appWidgetId)

            for (slotIndex in 0 until QuickEventWidgetStore.GRID_SLOT_COUNT) {
                val eventId = configuredEventIds[slotIndex]
                val cell = when {
                    eventId == null -> QuickEventWidgetCellRenderer.unconfigured(
                        context = context,
                        pendingIntent = configureSlotPendingIntent(context, appWidgetId, slotIndex),
                        text = "＋",
                        contentDescription = "未分配事件，点击选择"
                    )

                    else -> {
                        val event = events.firstOrNull { it.id == eventId && !it.isDeleted }
                        if (event == null) {
                            QuickEventWidgetCellRenderer.unavailable(
                                context,
                                configureSlotPendingIntent(context, appWidgetId, slotIndex)
                            )
                        } else {
                            val group = groups.firstOrNull { it.id == event.groupId }
                            val running = records.any { it.eventId == event.id && it.endTime == null }
                            QuickEventWidgetCellRenderer.configured(
                                context = context,
                                event = event,
                                group = group,
                                running = running,
                                pendingIntent = togglePendingIntent(context, appWidgetId, slotIndex)
                            )
                        }
                    }
                }
                views.addView(CELL_CONTAINER_IDS[slotIndex], cell)
            }

            views.addView(
                CELL_CONTAINER_IDS[EDIT_SLOT_INDEX],
                QuickEventWidgetCellRenderer.edit(
                    context,
                    editPendingIntent(context, appWidgetId)
                )
            )
            manager.updateAppWidget(appWidgetId, views)
        }
    }

    private fun togglePendingIntent(
        context: Context,
        appWidgetId: Int,
        slotIndex: Int
    ): PendingIntent {
        val intent = Intent(context, QuickEventGridWidgetProvider::class.java)
            .setAction(ACTION_TOGGLE_SLOT)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            .putExtra(QuickEventWidgetConfigContract.EXTRA_GRID_SLOT_INDEX, slotIndex)
        return PendingIntent.getBroadcast(
            context,
            TOGGLE_REQUEST_CODE_BASE + appWidgetId * 8 + slotIndex,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun configureSlotPendingIntent(
        context: Context,
        appWidgetId: Int,
        slotIndex: Int
    ): PendingIntent {
        val intent = Intent(context, QuickEventWidgetConfigureActivity::class.java)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            .putExtra(QuickEventWidgetConfigContract.EXTRA_GRID_MODE, true)
            .putExtra(QuickEventWidgetConfigContract.EXTRA_GRID_SLOT_INDEX, slotIndex)
        return PendingIntent.getActivity(
            context,
            CONFIGURE_REQUEST_CODE_BASE + appWidgetId * 8 + slotIndex,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun editPendingIntent(context: Context, appWidgetId: Int): PendingIntent {
        val intent = Intent(context, QuickEventWidgetConfigureActivity::class.java)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            .putExtra(QuickEventWidgetConfigContract.EXTRA_GRID_MODE, true)
            .putExtra(QuickEventWidgetConfigContract.EXTRA_GRID_MANAGEMENT, true)
        return PendingIntent.getActivity(
            context,
            CONFIGURE_REQUEST_CODE_BASE + appWidgetId * 8 + EDIT_SLOT_INDEX,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val ACTION_REFRESH = "com.bigbrother.mobile.widget.action.GRID_REFRESH"
        private const val ACTION_TOGGLE_SLOT = "com.bigbrother.mobile.widget.action.GRID_TOGGLE_SLOT"
        private const val TOGGLE_REQUEST_CODE_BASE = 30_000
        private const val CONFIGURE_REQUEST_CODE_BASE = 40_000
        private const val EDIT_SLOT_INDEX = 7
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val CELL_CONTAINER_IDS = intArrayOf(
            com.bigbrother.mobile.R.id.widget_grid_cell_0,
            com.bigbrother.mobile.R.id.widget_grid_cell_1,
            com.bigbrother.mobile.R.id.widget_grid_cell_2,
            com.bigbrother.mobile.R.id.widget_grid_cell_3,
            com.bigbrother.mobile.R.id.widget_grid_cell_4,
            com.bigbrother.mobile.R.id.widget_grid_cell_5,
            com.bigbrother.mobile.R.id.widget_grid_cell_6,
            com.bigbrother.mobile.R.id.widget_grid_cell_7
        )
    }
}