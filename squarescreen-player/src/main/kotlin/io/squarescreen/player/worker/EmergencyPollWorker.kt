package io.squarescreen.player.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.squarescreen.core.result.SquareScreenResult
import io.squarescreen.player.internal.SquareScreenServiceLocator

private const val TAG = "EmergencyPollWorker"

/**
 * Polls the emergency endpoint and updates [SquareScreenServiceLocator.emergencyAlertState].
 *
 * Architecture note: this worker is the first implementation of the emergency transport.
 * The underlying [io.squarescreen.core.datasource.NetworkDataSource.fetchEmergencyAlert]
 * contract is transport-agnostic — switching to WebSocket or FCM push only requires a new
 * implementation of that interface, not changes to the public [io.squarescreen.player.SquareScreen]
 * API or this flow.
 */
internal class EmergencyPollWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val network = SquareScreenServiceLocator.networkDataSource
        if (network == null) {
            SquareScreenServiceLocator.log(TAG, "SDK not initialized — skipping emergency poll")
            return Result.success()
        }

        val result = network.fetchEmergencyAlert()
        when (result) {
            is SquareScreenResult.Success -> {
                val alert = result.data
                SquareScreenServiceLocator.emergencyAlertState.value = alert
                if (alert != null) {
                    SquareScreenServiceLocator.log(TAG, "Emergency alert active: ${alert.title}")
                } else {
                    SquareScreenServiceLocator.log(TAG, "No active emergency alert")
                }
            }
            is SquareScreenResult.Error -> {
                SquareScreenServiceLocator.logError(TAG, "Emergency poll failed: ${result.error}")
                // Do not clear existing alert on poll failure — keep last known state
            }
        }
        return Result.success()
    }
}
