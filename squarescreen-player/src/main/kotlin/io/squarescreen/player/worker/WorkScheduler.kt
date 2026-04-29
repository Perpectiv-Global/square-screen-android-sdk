package io.squarescreen.player.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

internal const val HEARTBEAT_WORK_TAG = "squarescreen_heartbeat"
internal const val EMERGENCY_POLL_WORK_TAG = "squarescreen_emergency_poll"

internal class WorkScheduler(private val context: Context) {

    private val workManager = WorkManager.getInstance(context)

    fun scheduleHeartbeat(intervalSeconds: Long) {
        val request = PeriodicWorkRequestBuilder<HeartbeatWorker>(
            repeatInterval = intervalSeconds,
            repeatIntervalTimeUnit = TimeUnit.SECONDS
        )
            .addTag(HEARTBEAT_WORK_TAG)
            .build()

        workManager.enqueueUniquePeriodicWork(
            HEARTBEAT_WORK_TAG,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun scheduleEmergencyPoll(intervalSeconds: Long) {
        val request = PeriodicWorkRequestBuilder<EmergencyPollWorker>(
            repeatInterval = intervalSeconds,
            repeatIntervalTimeUnit = TimeUnit.SECONDS
        )
            .addTag(EMERGENCY_POLL_WORK_TAG)
            .build()

        workManager.enqueueUniquePeriodicWork(
            EMERGENCY_POLL_WORK_TAG,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancelAll() {
        workManager.cancelAllWorkByTag(HEARTBEAT_WORK_TAG)
        workManager.cancelAllWorkByTag(EMERGENCY_POLL_WORK_TAG)
    }
}
