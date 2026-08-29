package com.dwplayer.phone.core.server

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.dwplayer.phone.MainActivity
import com.dwplayer.phone.R
import com.dwplayer.phone.core.discovery.NsdServerAdvertiser
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PhoneServerService : Service() {

    @Inject lateinit var httpServer: PhoneHttpServer
    @Inject lateinit var nsdAdvertiser: NsdServerAdvertiser

    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        const val CHANNEL_ID = "dwshare_service_channel"
        const val NOTIFICATION_ID = 9001
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForegroundServer()
                stopSelf()
            }
            else -> {
                startForegroundServer()
            }
        }
        return START_STICKY
    }

    private fun startForegroundServer() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "dwShare::ServerWakeLock").apply {
            acquire(6 * 60 * 60 * 1000L) // 6 hours max
        }

        httpServer.start(port = 8085)
        nsdAdvertiser.startAdvertising(port = 8085)

        val ip = nsdAdvertiser.getLocalIpAddress()

        val openIntent = Intent(this, MainActivity::class.java)
        val pendingOpen = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, PhoneServerService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStop = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("dwShare Server Active")
            .setContentText("Broadcasting to TV on http://$ip:8085")
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentIntent(pendingOpen)
            .addAction(R.drawable.ic_launcher, "Stop Server", pendingStop)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun stopForegroundServer() {
        nsdAdvertiser.stopAdvertising()
        httpServer.stop()
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }

    override fun onDestroy() {
        stopForegroundServer()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "dwShare Server Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
