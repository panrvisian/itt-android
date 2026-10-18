package com.bigbrother.mobile.data

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.bigbrother.mobile.BigBrotherApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class IttForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var reminderJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        IttNotificationHelper.createNotificationChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = IttNotificationHelper.buildOngoingNotification(this, null, null)
        runCatching {
            startForeground(IttNotificationHelper.NOTIFICATION_ID, notification)
        }

        observeAppStateAndScheduleReminders()
        return START_STICKY
    }

    private fun observeAppStateAndScheduleReminders() {
        val app = application as? BigBrotherApp ?: return
        val repository = app.container.repository

        serviceScope.launch {
            repository.records.collectLatest { records ->
                val running = records.firstOrNull { it.endTime == null }
                val settings = repository.settings.first()

                if (settings.ongoingNotificationEnabled) {
                    IttNotificationHelper.updateOngoingNotification(
                        context = this@IttForegroundService,
                        runningEventName = running?.eventNameSnapshot,
                        startTime = running?.startTime
                    )
                }

                if (running != null || !settings.inactivityReminderEnabled) {
                    reminderJob?.cancel()
                    reminderJob = null
                } else {
                    if (reminderJob == null || reminderJob?.isActive != true) {
                        startProgressiveInactivityJob()
                    }
                }
            }
        }
    }

    private fun startProgressiveInactivityJob() {
        reminderJob?.cancel()
        reminderJob = serviceScope.launch {
            // Stage 1: 3 minutes (180s)
            delay(3L * 60_000L)
            IttNotificationHelper.sendInactivityReminder(
                context = this@IttForegroundService,
                title = "已经空闲 3 分钟啦",
                text = "记得及时记录事件哦，点击返回首页打卡"
            )

            // Stage 2: Next 7 minutes (total 10 minutes)
            delay(7L * 60_000L)
            IttNotificationHelper.sendInactivityReminder(
                context = this@IttForegroundService,
                title = "已经空闲 10 分钟啦",
                text = "刚才忙了什么呢？点击返回首页打卡"
            )

            // Stage 3: Next 20 minutes (total 30 minutes)
            delay(20L * 60_000L)
            IttNotificationHelper.sendInactivityReminder(
                context = this@IttForegroundService,
                title = "已经空闲 30 分钟啦",
                text = "点击返回首页开启打卡或补录"
            )

            // Stage 4: Every 60 minutes thereafter
            while (true) {
                delay(60L * 60_000L)
                IttNotificationHelper.sendInactivityReminder(
                    context = this@IttForegroundService,
                    title = "已经空闲 1 小时啦",
                    text = "点击返回首页开启打卡或补录"
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        reminderJob?.cancel()
    }

    companion object {
        fun startService(context: Context) {
            val intent = Intent(context, IttForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                runCatching { context.startForegroundService(intent) }
            } else {
                runCatching { context.startService(intent) }
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, IttForegroundService::class.java)
            runCatching { context.stopService(intent) }
        }
    }
}
