package io.squarescreen.core.model

import io.squarescreen.core.annotation.ExperimentalSquareScreenApi
import kotlinx.serialization.Serializable

/**
 * Server-defined playback strategy. The exact shape is TBD as the backend API evolves.
 * This type is experimental and its structure may change without a major version bump.
 */
@ExperimentalSquareScreenApi
@Serializable
data class PlaybackStrategy(
    val raw: Map<String, String> = emptyMap()
)
