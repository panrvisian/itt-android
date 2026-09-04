package com.bigbrother.mobile.widget

import android.content.Context

/** Stores the event selected by each desktop widget instance. */
internal object QuickEventWidgetStore {
    const val GRID_SLOT_COUNT = 7

    private const val PREFERENCES_NAME = "quick_event_widget_preferences"
    private const val EVENT_ID_PREFIX = "event_id_"
    private const val GRID_EVENT_ID_PREFIX = "grid_event_id_"

    private fun preferences(context: Context) =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun eventId(context: Context, appWidgetId: Int): String? =
        preferences(context).getString(EVENT_ID_PREFIX + appWidgetId, null)

    fun saveEventId(context: Context, appWidgetId: Int, eventId: String) {
        preferences(context).edit()
            .putString(EVENT_ID_PREFIX + appWidgetId, eventId)
            .apply()
    }

    fun gridEventId(context: Context, appWidgetId: Int, slotIndex: Int): String? {
        if (slotIndex !in 0 until GRID_SLOT_COUNT) return null
        return preferences(context).getString(gridKey(appWidgetId, slotIndex), null)
    }

    fun gridEventIds(context: Context, appWidgetId: Int): List<String?> =
        (0 until GRID_SLOT_COUNT).map { slotIndex ->
            gridEventId(context, appWidgetId, slotIndex)
        }

    fun saveGridEventId(context: Context, appWidgetId: Int, slotIndex: Int, eventId: String) {
        if (slotIndex !in 0 until GRID_SLOT_COUNT) return
        preferences(context).edit()
            .putString(gridKey(appWidgetId, slotIndex), eventId)
            .apply()
    }

    fun saveGridEventIds(context: Context, appWidgetId: Int, eventIds: List<String?>) {
        preferences(context).edit().apply {
            for (slotIndex in 0 until GRID_SLOT_COUNT) {
                val key = gridKey(appWidgetId, slotIndex)
                val eventId = eventIds.getOrNull(slotIndex)
                if (eventId == null) {
                    remove(key)
                } else {
                    putString(key, eventId)
                }
            }
        }.apply()
    }

    fun remove(context: Context, appWidgetId: Int) {
        preferences(context).edit().apply {
            remove(EVENT_ID_PREFIX + appWidgetId)
            for (slotIndex in 0 until GRID_SLOT_COUNT) {
                remove(gridKey(appWidgetId, slotIndex))
            }
        }.apply()
    }

    fun copy(context: Context, oldAppWidgetId: Int, newAppWidgetId: Int) {
        val preferences = preferences(context)
        val editor = preferences.edit()
        preferences.getString(EVENT_ID_PREFIX + oldAppWidgetId, null)?.let {
            editor.putString(EVENT_ID_PREFIX + newAppWidgetId, it)
        }
        for (slotIndex in 0 until GRID_SLOT_COUNT) {
            preferences.getString(gridKey(oldAppWidgetId, slotIndex), null)?.let {
                editor.putString(gridKey(newAppWidgetId, slotIndex), it)
            }
        }
        editor.apply()
    }

    private fun gridKey(appWidgetId: Int, slotIndex: Int): String =
        "$GRID_EVENT_ID_PREFIX${appWidgetId}_$slotIndex"
}