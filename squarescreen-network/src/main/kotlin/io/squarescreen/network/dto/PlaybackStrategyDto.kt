package io.squarescreen.network.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class PlaybackStrategyDto(
    val loop: Boolean = true,
    val shuffle: Boolean = false,
    val preloadCount: Int = 1
)
