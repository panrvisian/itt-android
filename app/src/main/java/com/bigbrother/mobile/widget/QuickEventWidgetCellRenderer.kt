package com.bigbrother.mobile.widget

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.SystemClock
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
        runningStartTime: Long?,
        bgAlpha: Float = QuickEventWidgetStore.DEFAULT_ALPHA,
        pendingIntent: PendingIntent
    ): RemoteViews {
        val views = baseViews(context)
        val name = normalizedName(event.name)
        val groupColor = group?.colorArgb ?: DEFAULT_GROUP_COLOR
        val density = context.resources.displayMetrics.density

        // Generate translucent timeline card background
        val bgBitmap = createTimelineCardBitmap(
            groupColor = groupColor,
            bgAlpha = bgAlpha,
            density = density
        )
        views.setImageViewBitmap(R.id.widget_bg_image, bgBitmap)

        // Event name stays ALWAYS centered at 16sp bold in widget_name_single
        views.setViewVisibility(R.id.widget_name_single, View.VISIBLE)
        views.setTextViewText(R.id.widget_name_single, name)

        if (runningStartTime != null) {
            val elapsedTimeMs = (System.currentTimeMillis() - runningStartTime).coerceAtLeast(0L)
            val baseTime = SystemClock.elapsedRealtime() - elapsedTimeMs
            views.setChronometer(R.id.widget_chronometer, baseTime, null, true)
            views.setViewVisibility(R.id.widget_chronometer, View.VISIBLE)

            views.setContentDescription(
                R.id.widget_root,
                "${event.name}，进行中，点击结束"
            )
        } else {
            views.setChronometer(R.id.widget_chronometer, 0, null, false)
            views.setViewVisibility(R.id.widget_chronometer, View.GONE)

            views.setContentDescription(
                R.id.widget_root,
                "${event.name}，点击开始"
            )
        }

        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
        return views
    }

    fun unconfigured(
        context: Context,
        pendingIntent: PendingIntent,
        text: String = "选择事件",
        contentDescription: String = "选择小组件事件",
        bgAlpha: Float = QuickEventWidgetStore.DEFAULT_ALPHA
    ): RemoteViews =
        placeholder(
            context = context,
            text = text,
            contentDescription = contentDescription,
            bgAlpha = bgAlpha,
            pendingIntent = pendingIntent
        )

    fun unavailable(
        context: Context,
        pendingIntent: PendingIntent,
        bgAlpha: Float = QuickEventWidgetStore.DEFAULT_ALPHA
    ): RemoteViews =
        placeholder(
            context = context,
            text = "不可用",
            contentDescription = "事件不可用，点击重新选择",
            bgAlpha = bgAlpha,
            pendingIntent = pendingIntent
        )

    fun edit(
        context: Context,
        pendingIntent: PendingIntent,
        bgAlpha: Float = QuickEventWidgetStore.DEFAULT_ALPHA
    ): RemoteViews =
        placeholder(
            context = context,
            text = "编辑",
            contentDescription = "编辑快速计时小组件",
            pendingIntent = pendingIntent
        )

    private fun placeholder(
        context: Context,
        text: String,
        contentDescription: String,
        bgAlpha: Float = QuickEventWidgetStore.DEFAULT_ALPHA,
        pendingIntent: PendingIntent
    ): RemoteViews {
        val views = baseViews(context)
        val density = context.resources.displayMetrics.density
        val placeholderColor = 0xFF808080.toInt()

        val bgBitmap = createTimelineCardBitmap(
            groupColor = placeholderColor,
            bgAlpha = bgAlpha,
            density = density
        )
        views.setImageViewBitmap(R.id.widget_bg_image, bgBitmap)

        views.setViewVisibility(R.id.widget_name_single, View.VISIBLE)
        views.setTextViewText(R.id.widget_name_single, text)
        views.setChronometer(R.id.widget_chronometer, 0, null, false)
        views.setViewVisibility(R.id.widget_chronometer, View.GONE)
        views.setContentDescription(R.id.widget_root, contentDescription)
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
        return views
    }

    private fun baseViews(context: Context): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_quick_event)
        views.setTextColor(R.id.widget_name_single, Color.WHITE)
        views.setTextColor(R.id.widget_chronometer, Color.WHITE)
        return views
    }

    private fun createTimelineCardBitmap(
        widthPx: Int = 300,
        heightPx: Int = 200,
        groupColor: Int,
        bgAlpha: Float = QuickEventWidgetStore.DEFAULT_ALPHA,
        cornerRadiusDp: Float = 16f,
        density: Float
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val r = Color.red(groupColor)
        val g = Color.green(groupColor)
        val b = Color.blue(groupColor)

        // Translucent background fill
        val fillAlphaInt = (bgAlpha * 255).toInt().coerceIn(10, 255)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(fillAlphaInt, r, g, b)
        }

        // Translucent border stroke (~2.5x fill alpha, capped at 255)
        val strokeAlphaInt = (bgAlpha * 2.5f * 255).toInt().coerceIn(60, 255)
        val strokeWidthPx = 1.5f * density
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = strokeWidthPx
            color = Color.argb(strokeAlphaInt, r, g, b)
        }

        val radiusPx = cornerRadiusDp * density
        val halfStroke = strokeWidthPx / 2f
        val rect = RectF(
            halfStroke,
            halfStroke,
            widthPx.toFloat() - halfStroke,
            heightPx.toFloat() - halfStroke
        )

        canvas.drawRoundRect(rect, radiusPx, radiusPx, fillPaint)
        canvas.drawRoundRect(rect, radiusPx, radiusPx, strokePaint)

        return bitmap
    }

    private fun normalizedName(rawName: String): String =
        rawName.replace(Regex("\\s+"), " ").trim().ifEmpty { "未命名" }
}