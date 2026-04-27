package io.squarescreen.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class PlaybackStrategyDto(
    val loop: Boolean = true,
    val shuffle: Boolean = false,
    @SerialName("preloadCount") val preloadCount: Int = 1,
    @SerialName("showThumbnail") val showThumbnail: Boolean = false,
    @SerialName("defaultTransition") val defaultTransition: String = "none"
)
