package io.squarescreen.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class PlaybackReportResponseDto(
    val recorded: Boolean
)
