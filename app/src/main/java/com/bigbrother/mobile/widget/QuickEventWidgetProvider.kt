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

class QuickEventWidgetProvider : AppWidgetProvider() {
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
            ACTION_TOGGLE -> {
                val appWidgetId = intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID
                )
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    launchAsync {
                        toggleEvent(context, appWidgetId)
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

    private suspend fun toggleEvent(context: Context, appWidgetId: Int) {
        val eventId = QuickEventWidgetStore.eventId(context, appWidgetId) ?: return
        val repository = (context.applicationContext as BigBrotherApp).container.repository
        repository.toggleEvent(eventId)
    }

    private suspend fun updateAllWidgets(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, QuickEventWidgetProvider::class.java)
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
            val eventId = QuickEventWidgetStore.eventId(context, appWidgetId)
            val views = when {
                eventId == null -> QuickEventWidgetCellRenderer.unconfigured(
                    context,
                    configurePendingIntent(context, appWidgetId)
                )

                else -> {
                    val event = events.firstOrNull { it.id == eventId && !it.isDeleted }
                    if (event == null) {
                        QuickEventWidgetCellRenderer.unavailable(
                            context,
                            configurePendingIntent(context, appWidgetId)
                        )
                    } else {
                        val group = groups.firstOrNull { it.id == event.groupId }
                        val running = records.any { it.eventId == event.id && it.endTime == null }
                        QuickEventWidgetCellRenderer.configured(
                            context = context,
                            event = event,
                            group = group,
                            running = running,
                            pendingIntent = togglePendingIntent(context, appWidgetId)
                        )
                    }
                }
            }
            manager.updateAppWidget(appWidgetId, views)
        }
    }

    private fun togglePendingIntent(context: Context, appWidgetId: Int): PendingIntent {
        val intent = Intent(context, QuickEventWidgetProvider::class.java)
            .setAction(ACTION_TOGGLE)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        return PendingIntent.getBroadcast(
            context,
            TOGGLE_REQUEST_CODE_BASE + appWidgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun configurePendingIntent(context: Context, appWidgetId: Int): PendingIntent {
        val intent = Intent(context, QuickEventWidgetConfigureActivity::class.java)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        return PendingIntent.getActivity(
            context,
            CONFIGURE_REQUEST_CODE_BASE + appWidgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val ACTION_REFRESH = "com.bigbrother.mobile.widget.action.REFRESH"
        private const val ACTION_TOGGLE = "com.bigbrother.mobile.widget.action.TOGGLE"
        private const val TOGGLE_REQUEST_CODE_BASE = 10_000
        private const val CONFIGURE_REQUEST_CODE_BASE = 20_000
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun requestRefresh(context: Context) {
            val appContext = context.applicationContext
            appContext.sendBroadcast(
                Intent(appContext, QuickEventWidgetProvider::class.java)
                    .setAction(ACTION_REFRESH)
            )
            appContext.sendBroadcast(
                Intent(appContext, QuickEventGridWidgetProvider::class.java)
                    .setAction(QuickEventGridWidgetProvider.ACTION_REFRESH)
            )
        }
    }
}