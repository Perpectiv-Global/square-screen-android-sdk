package io.squarescreen.player.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import io.squarescreen.player.internal.SquareScreenServiceLocator

internal const val NOTIFICATION_CHANNEL_ID = "squarescreen_player"
internal const val NOTIFICATION_ID = 0x5351 // 'SQ' in hex

/**
 * Foreground service that keeps the player process alive during continuous display operation.
 *
 * Started automatically by [io.squarescreen.player.SquareScreen.init] when a playlist is active.
 * Stopped cleanly by [io.squarescreen.player.SquareScreen.shutdown].
 *
 * Integrators do not interact with this service directly — they provide the notification
 * title and icon via [io.squarescreen.core.config.ForegroundNotificationConfig].
 */
class SquareScreenPlayerService : Service() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "SquareScreen Player",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps the SquareScreen display player running"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val config = SquareScreenServiceLocator.config
        val title = config?.foregroundNotification?.title ?: "SquareScreen Player"
        val iconResId = config?.foregroundNotification?.iconResId
            ?: android.R.drawable.ic_media_play

        return Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setSmallIcon(iconResId)
            .setOngoing(true)
            .build()
    }
}
