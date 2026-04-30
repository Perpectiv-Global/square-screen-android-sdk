package io.squarescreen.core.model

import io.squarescreen.core.annotation.ExperimentalSquareScreenApi

/**
 * Server-defined playback strategy returned alongside the playlist.
 *
 * This type is experimental — additional fields may be added as the backend API evolves.
 *
 * @param loop Whether the playlist should loop continuously after the last item.
 * @param shuffle Whether items should be played in random order.
 * @param preloadCount Number of upcoming items to preload ahead of the current position.
 */
@ExperimentalSquareScreenApi
data class PlaybackStrategy(
    val loop: Boolean = true,
    val shuffle: Boolean = false,
    val preloadCount: Int = 1
)
