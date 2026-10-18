package com.bigbrother.mobile.data

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.bigbrother.mobile.R
import com.bigbrother.mobile.ui.MainActivity

object IttNotificationHelper {

    const val CHANNEL_ID = "itt_ongoing_channel"
    const val NOTIFICATION_ID = 1001
    const val INACTIVITY_NOTIFICATION_ID = 1002

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "ITT 状态栏控制与提醒"
            val descriptionText = "实时打卡显示与闲置超时补录提醒"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun buildOngoingNotification(
        context: Context,
        runningEvents: List<RecordEntity>
    ): Notification {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (runningEvents.isNotEmpty()) {
            val count = runningEvents.size
            if (count == 1) {
                val single = runningEvents.first()
                builder.setContentTitle("正在进行：${single.eventNameSnapshot}")
                    .setContentText("已实时打卡计时，点击返回应用")
                    .setUsesChronometer(true)
                    .setWhen(single.startTime)
            } else {
                val eventNamesStr = runningEvents.joinToString("、") { it.eventNameSnapshot }
                val earliestStartTime = runningEvents.minOf { it.startTime }
                val bulletList = runningEvents.joinToString("\n") { "• ${it.eventNameSnapshot}" }

                builder.setContentTitle("正在进行 ($count 项)")
                    .setContentText("包含：$eventNamesStr")
                    .setStyle(
                        NotificationCompat.BigTextStyle()
                            .setBigContentTitle("正在进行 ($count 项)")
                            .bigText("当前进行中的打卡项目：\n$bulletList")
                    )
                    .setUsesChronometer(true)
                    .setWhen(earliestStartTime)
            }
        } else {
            builder.setContentTitle("Individual Time Trial")
                .setContentText("当前空闲状态，点击开启打卡或快捷补录")
        }

        return builder.build()
    }

    @SuppressLint("MissingPermission")
    fun updateOngoingNotification(
        context: Context,
        runningEvents: List<RecordEntity>
    ) {
        val notification = buildOngoingNotification(context, runningEvents)
        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
    }

    @SuppressLint("MissingPermission")
    fun sendInactivityReminder(
        context: Context,
        title: String = "已经空闲 3 分钟啦",
        text: String = "记得及时记录事件哦，点击返回首页"
    ) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        runCatching {
            NotificationManagerCompat.from(context).notify(INACTIVITY_NOTIFICATION_ID, builder.build())
        }
    }

    fun cancelOngoingNotification(context: Context) {
        runCatching {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        }
    }
}
