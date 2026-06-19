package io.squarescreen.player.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import io.squarescreen.core.model.HeartbeatPayload
import io.squarescreen.player.internal.DeviceMetricsCollector
import io.squarescreen.player.internal.SDK_VERSION
import io.squarescreen.player.internal.SquareScreenServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal const val NOTIFICATION_CHANNEL_ID = "squarescreen_player"
internal const val NOTIFICATION_ID = 0x5351 // 'SQ' in hex

private const val TAG = "SquareScreenPlayerService"

/**
 * Foreground service that keeps the player process alive and drives the heartbeat loop.
 *
 * The heartbeat runs as a coroutine loop inside the service rather than WorkManager so
 * it respects the configured interval without WorkManager's 15-minute minimum clamp.
 *
 * Started automatically by [io.squarescreen.player.SquareScreen.init].
 * Stopped cleanly by [io.squarescreen.player.SquareScreen.shutdown].
 */
class SquareScreenPlayerService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var heartbeatJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        startHeartbeatLoop()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        heartbeatJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    private fun startHeartbeatLoop() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            val config = SquareScreenServiceLocator.config ?: return@launch
            val intervalMs = config.heartbeatIntervalSeconds * 1_000L
            val metrics = DeviceMetricsCollector(applicationContext)

            while (true) {
                sendHeartbeat(metrics)
                delay(intervalMs)
            }
        }
    }

    private suspend fun sendHeartbeat(metrics: DeviceMetricsCollector) {
        val network = SquareScreenServiceLocator.networkDataSource ?: return
        val config = SquareScreenServiceLocator.config ?: return

        val payload = HeartbeatPayload(
            cpuUsage = metrics.getCpuUsage(),
            memoryUsage = metrics.getMemoryUsage(),
            diskUsage = metrics.getDiskUsage(),
            temperature = metrics.getTemperature(),
            osVersion = "Android ${android.os.Build.VERSION.RELEASE}",
            playerVersion = SDK_VERSION
        )

        SquareScreenServiceLocator.log(TAG, "Posting heartbeat")
        network.sendHeartbeat(payload)
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
