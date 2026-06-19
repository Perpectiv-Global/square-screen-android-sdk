package io.squarescreen.player.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

internal const val HEARTBEAT_WORK_TAG = "squarescreen_heartbeat"
internal const val EMERGENCY_POLL_WORK_TAG = "squarescreen_emergency_poll"
internal const val COMMAND_POLL_WORK_TAG = "squarescreen_command_poll"

/** Default command poll interval in seconds. */
private const val DEFAULT_COMMAND_POLL_INTERVAL_SECONDS = 30L

internal class WorkScheduler(private val context: Context) {

    private val workManager = WorkManager.getInstance(context)

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

    fun scheduleCommandPoll(intervalSeconds: Long = DEFAULT_COMMAND_POLL_INTERVAL_SECONDS) {
        val request = PeriodicWorkRequestBuilder<CommandPollWorker>(
            repeatInterval = intervalSeconds,
            repeatIntervalTimeUnit = TimeUnit.SECONDS
        )
            .addTag(COMMAND_POLL_WORK_TAG)
            .build()

        workManager.enqueueUniquePeriodicWork(
            COMMAND_POLL_WORK_TAG,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancelAll() {
        workManager.cancelAllWorkByTag(EMERGENCY_POLL_WORK_TAG)
        workManager.cancelAllWorkByTag(COMMAND_POLL_WORK_TAG)
    }
}
