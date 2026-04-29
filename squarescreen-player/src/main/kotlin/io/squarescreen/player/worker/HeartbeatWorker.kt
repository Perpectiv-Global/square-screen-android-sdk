package io.squarescreen.player.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.squarescreen.core.model.HeartbeatPayload
import io.squarescreen.player.internal.DeviceMetricsCollector
import io.squarescreen.player.internal.SDK_VERSION
import io.squarescreen.player.internal.SquareScreenServiceLocator

private const val TAG = "HeartbeatWorker"

internal class HeartbeatWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val network = SquareScreenServiceLocator.networkDataSource
        if (network == null) {
            SquareScreenServiceLocator.log(TAG, "SDK not initialized — skipping heartbeat")
            return Result.success()
        }

        val metrics = DeviceMetricsCollector(applicationContext)
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
        return Result.success()
    }
}
