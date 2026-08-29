package com.dwplayer.core.downloader

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.dwplayer.R
import com.dwplayer.data.daos.DownloadTaskDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class DownloadService : Service() {

    companion object {
        private const val CHANNEL_ID = "dwplayer_downloads_channel"
        private const val NOTIFICATION_ID = 1001
    }

    @Inject lateinit var downloadManager: DwDownloadManager
    @Inject lateinit var downloadTaskDao: DownloadTaskDao

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("dwPlayer Downloader Active", "Managing downloads in background"))
        observeDownloadStatus()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val taskId = intent?.getStringExtra("TASK_ID")

        serviceScope.launch {
            when (action) {
                "PAUSE_TASK" -> {
                    taskId?.let { downloadManager.pauseDownload(it) }
                }
                "PAUSE_ALL" -> {
                    downloadManager.pauseAll()
                }
                "RESUME_ALL" -> {
                    val pausedTasks = downloadTaskDao.getAllTasks().first().filter { it.status == "PAUSED" || it.status == "FAILED" }
                    pausedTasks.forEach { downloadManager.startDownload(it.id) }
                }
                else -> {
                    if (taskId != null) {
                        downloadManager.startDownload(taskId)
                    } else {
                        // Resume pending
                        val pending = downloadTaskDao.getAllTasks().first().filter { it.status == "PENDING" }
                        pending.forEach { downloadManager.startDownload(it.id) }
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun observeDownloadStatus() {
        serviceScope.launch {
            downloadManager.downloadStatus.collect { map ->
                if (map.isNotEmpty()) {
                    val activeCount = map.size
                    val totalSpeed = map.values.joinToString(" | ") { it.speed }.take(50)
                    updateNotification("Downloading ($activeCount active)", totalSpeed)
                } else {
                    updateNotification("dwPlayer Downloader Ready", "Idle")
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "dwPlayer Download Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live download progress and speed for video downloads"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification(title: String, content: String) {
        val notification = buildNotification(title, content)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceJob.cancel()
        super.onDestroy()
    }
}
