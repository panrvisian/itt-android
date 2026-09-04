package com.bigbrother.mobile.widget

import android.app.PendingIntent
import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import com.bigbrother.mobile.R
import com.bigbrother.mobile.data.EventEntity
import com.bigbrother.mobile.data.GroupEntity

/** Shared rendering for the 1×1 event cell used by both widget sizes. */
internal object QuickEventWidgetCellRenderer {
    const val DEFAULT_GROUP_COLOR = 0xFF9E9E9E.toInt()

    fun configured(
        context: Context,
        event: EventEntity,
        group: GroupEntity?,
        running: Boolean,
        pendingIntent: PendingIntent
    ): RemoteViews {
        val backgroundColor = group?.colorArgb ?: DEFAULT_GROUP_COLOR
        val textColor = contrastingColor(backgroundColor)
        val views = baseViews(context, backgroundColor, textColor)

        // Keep the full name and let the one-line TextView add an ellipsis when needed.
        views.setViewVisibility(R.id.widget_name_single, View.VISIBLE)
        views.setTextViewText(R.id.widget_name_single, normalizedName(event.name))

        views.setViewVisibility(R.id.widget_status_dot, if (running) View.VISIBLE else View.GONE)
        views.setTextColor(R.id.widget_status_dot, textColor)
        views.setContentDescription(
            R.id.widget_root,
            if (running) {
                "${event.name}，进行中，点击结束"
            } else {
                "${event.name}，点击开始"
            }
        )
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
        return views
    }

    fun unconfigured(
        context: Context,
        pendingIntent: PendingIntent,
        text: String = "选择事件",
        contentDescription: String = "选择小组件事件"
    ): RemoteViews =
        placeholder(
            context = context,
            backgroundColor = DEFAULT_GROUP_COLOR,
            textColor = Color.WHITE,
            text = text,
            contentDescription = contentDescription,
            pendingIntent = pendingIntent
        )

    fun unavailable(context: Context, pendingIntent: PendingIntent): RemoteViews =
        placeholder(
            context = context,
            backgroundColor = DEFAULT_GROUP_COLOR,
            textColor = Color.WHITE,
            text = "不可用",
            contentDescription = "事件不可用，点击重新选择",
            pendingIntent = pendingIntent
        )

    fun edit(context: Context, pendingIntent: PendingIntent): RemoteViews =
        placeholder(
            context = context,
            backgroundColor = 0xFF616161.toInt(),
            textColor = Color.WHITE,
            text = "编辑",
            contentDescription = "编辑快速计时小组件",
            pendingIntent = pendingIntent
        )

    private fun placeholder(
        context: Context,
        backgroundColor: Int,
        textColor: Int,
        text: String,
        contentDescription: String,
        pendingIntent: PendingIntent
    ): RemoteViews {
        val views = baseViews(context, backgroundColor, textColor)
        views.setViewVisibility(R.id.widget_name_single, View.VISIBLE)
        views.setTextViewText(R.id.widget_name_single, text)
        views.setViewVisibility(R.id.widget_status_dot, View.GONE)
        views.setContentDescription(R.id.widget_root, contentDescription)
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
        return views
    }

    private fun baseViews(context: Context, backgroundColor: Int, textColor: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_quick_event)
        views.setInt(R.id.widget_root, "setBackgroundColor", backgroundColor)
        views.setTextColor(R.id.widget_name_single, textColor)
        views.setTextColor(R.id.widget_status_dot, textColor)
        return views
    }

    private fun normalizedName(rawName: String): String =
        rawName.replace(Regex("\\s+"), " ").trim().ifEmpty { "未命名" }

    private fun contrastingColor(color: Int): Int {
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        val luminance = (red * 299 + green * 587 + blue * 114) / 1000
        return if (luminance >= 160) Color.BLACK else Color.WHITE
    }
}