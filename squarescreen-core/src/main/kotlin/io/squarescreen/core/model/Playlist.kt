package io.squarescreen.core.model

import io.squarescreen.core.annotation.ExperimentalSquareScreenApi

/**
 * A resolved playlist ready for playback on this device.
 *
 * Emitted by [io.squarescreen.player.SquareScreen.nowPlaying] whenever the schedule
 * changes or a manual [io.squarescreen.player.SquareScreen.refresh] is triggered.
 *
 * @param items Ordered list of media items to display.
 * @param strategy Server-defined playback behaviour (loop, shuffle, preload). Null
 *   if the server returned no strategy for the current schedule.
 * @param schedule Metadata about the schedule driving this playlist, if any.
 * @param playlist Metadata about the playlist itself, if any.
 * @param cachedAt Epoch milliseconds at which this playlist was last fetched and cached.
 */
data class Playlist(
    val items: List<PlaylistItem>,
    @OptIn(ExperimentalSquareScreenApi::class)
    val strategy: PlaybackStrategy?,
    val schedule: ScheduleInfo?,
    val playlist: PlaylistInfo?,
    val cachedAt: Long
)
