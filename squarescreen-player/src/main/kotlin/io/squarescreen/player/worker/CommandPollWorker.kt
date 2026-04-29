package io.squarescreen.player.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.squarescreen.core.result.SquareScreenResult
import io.squarescreen.player.internal.SquareScreenServiceLocator

private const val TAG = "CommandPollWorker"

/**
 * Polls [GET /api/v1/screen/commands] and updates [SquareScreenServiceLocator.commandsState].
 *
 * When commands are received, the [io.squarescreen.player.SquareScreen.commands] Flow emits
 * them. Integrators (or [io.squarescreen.ui.SquareScreenDisplay]) observe this flow and call
 * [io.squarescreen.player.SquareScreen.acknowledgeCommand] after handling each one.
 */
internal class CommandPollWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val network = SquareScreenServiceLocator.networkDataSource
        if (network == null) {
            SquareScreenServiceLocator.log(TAG, "SDK not initialized — skipping command poll")
            return Result.success()
        }

        val result = network.fetchCommands()
        when (result) {
            is SquareScreenResult.Success -> {
                val commands = result.data
                if (commands.isNotEmpty()) {
                    SquareScreenServiceLocator.log(TAG, "Received ${commands.size} command(s)")
                    SquareScreenServiceLocator.commandsState.value = commands
                } else {
                    SquareScreenServiceLocator.log(TAG, "No pending commands")
                }
            }
            is SquareScreenResult.Error -> {
                SquareScreenServiceLocator.logError(TAG, "Command poll failed: ${result.error}")
            }
        }
        return Result.success()
    }
}
