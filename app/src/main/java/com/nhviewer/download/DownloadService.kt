package com.nhviewer.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.nhviewer.ui.download.DownloadsActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DownloadService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NotificationManager::class.java)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val initial = buildNotification("Starting downloads\u2026", 0, 0, indeterminate = true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, initial, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, initial)
        }

        scope.launch {
            DownloadCenter.tasks.collectLatest { tasks ->
                val active = tasks.filter {
                    it.status == DownloadStatus.RUNNING || it.status == DownloadStatus.QUEUED
                }
                if (active.isEmpty()) {
                    ServiceCompat.stopForeground(this@DownloadService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return@collectLatest
                }
                val totalDone = active.sumOf { it.completed }
                val totalPages = active.sumOf { it.total }
                val title = if (active.size == 1) active[0].title else "${active.size} downloads in progress"
                notificationManager.notify(NOTIFICATION_ID, buildNotification(title, totalDone, totalPages))
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Gallery Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows download progress for galleries"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(
        title: String,
        completed: Int,
        total: Int,
        indeterminate: Boolean = false
    ): android.app.Notification {
        val tapIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, DownloadsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val progress = if (total > 0) completed * 100 / total else 0
        val contentText = if (total > 0) "$completed / $total pages" else "Preparing\u2026"
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(contentText)
            .setProgress(100, progress, indeterminate)
            .setContentIntent(tapIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "nhviewer_downloads"
        const val NOTIFICATION_ID = 1001
    }
}
