package io.squarescreen.core.model

import io.squarescreen.core.annotation.ExperimentalSquareScreenApi

data class Playlist(
    val items: List<PlaylistItem>,
    @OptIn(ExperimentalSquareScreenApi::class)
    val strategy: PlaybackStrategy?,
    /** The active schedule driving this playlist, if any. */
    val schedule: ScheduleInfo?,
    /** Metadata about the playlist itself. */
    val playlist: PlaylistInfo?,
    /** Epoch milliseconds at which this playlist was cached locally. */
    val cachedAt: Long
)
