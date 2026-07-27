package io.squarescreen.core.datasource

import io.squarescreen.core.model.Command
import io.squarescreen.core.model.EmergencyAlert
import io.squarescreen.core.model.HeartbeatPayload
import io.squarescreen.core.model.PlaybackReport
import io.squarescreen.core.model.Playlist
import io.squarescreen.core.result.SquareScreenResult

/**
 * Contract for the remote data layer. Implemented by `squarescreen-network`;
 * not exposed as a public dependency to integrators.
 */
interface NetworkDataSource {

    /**
     * Fetches the active playlist for the authenticated device.
     *
     * @param type Optional media type filter ("image" or "video").
     * @param category Optional category filter.
     * @param quality Desired media quality (default "1080p").
     * @param limit Maximum number of items to return (default 20).
     */
    suspend fun fetchNowPlaying(
        type: String? = null,
        category: String? = null,
        quality: String? = "1080p",
        limit: Int? = 20
    ): SquareScreenResult<Playlist>

    /** Posts a device health heartbeat to the server. */
    suspend fun sendHeartbeat(payload: HeartbeatPayload): SquareScreenResult<Unit>

    /**
     * Checks whether there is an active emergency broadcast for this device.
     * Returns null inside the result when no alert is active.
     */
    suspend fun fetchEmergencyAlert(): SquareScreenResult<EmergencyAlert?>

    /**
     * Reports a batch of completed playback events (proof-of-play) to the server.
     * Sent as {"playbacks": [...]}.
     */
    suspend fun reportPlayback(reports: List<PlaybackReport>): SquareScreenResult<Unit>

    /**
     * Polls for pending server-issued commands targeting this device.
     * Returns an empty list when no commands are pending.
     */
    suspend fun fetchCommands(): SquareScreenResult<List<Command>>

    /**
     * Acknowledges that a command has been executed.
     *
     * @param commandId The [Command.id] being acknowledged.
     * @param status Outcome status (e.g. "completed", "failed").
     * @param result Optional key-value result payload describing the outcome.
     */
    suspend fun acknowledgeCommand(
        commandId: String,
        status: String,
        result: Map<String, String> = emptyMap()
    ): SquareScreenResult<Unit>
}
