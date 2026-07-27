package io.squarescreen.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Proof-of-play report for a single playlist item.
 *
 * Reports are sent in a batch via [io.squarescreen.player.SquareScreen.reportPlayback].
 *
 * @param id ID of the playlist item that was played.
 * @param startedAt ISO 8601 timestamp when the item started displaying.
 * @param endedAt ISO 8601 timestamp when the item finished displaying.
 * @param durationSeconds Actual number of seconds the item was displayed.
 * @param completed Whether the item played to its full duration without interruption.
 */
@Serializable
data class PlaybackReport(
    @SerialName("item_id") val id: String,
    @SerialName("started_at") val startedAt: String,
    @SerialName("ended_at") val endedAt: String,
    @SerialName("duration_seconds") val durationSeconds: Int,
    val completed: Boolean
)
