package io.squarescreen.core.model

import io.squarescreen.core.annotation.ExperimentalSquareScreenApi

/**
 * Server-defined playback strategy returned alongside the playlist.
 *
 * This type is experimental — additional fields may be added as the backend API evolves.
 *
 * @param loop Whether the playlist should loop continuously.
 * @param shuffle Whether items should be played in random order.
 * @param preloadCount Number of items to preload ahead of the current position.
 * @param showThumbnail Whether to show a thumbnail preview during transitions.
 * @param defaultTransition Default transition to apply when a PlaylistItem has none set.
 */
@ExperimentalSquareScreenApi
data class PlaybackStrategy(
    val loop: Boolean = true,
    val shuffle: Boolean = false,
    val preloadCount: Int = 1,
    val showThumbnail: Boolean = false,
    val defaultTransition: TransitionType = TransitionType.NONE
)
