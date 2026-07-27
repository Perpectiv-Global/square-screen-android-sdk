package io.squarescreen.network.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class PlaybackReportResponseDto(
    val recorded: Int,
    val failed: Int,
    val results: List<PlaybackResultItemDto> = emptyList()
)

@Serializable
internal data class PlaybackResultItemDto(
    val index: Int,
    val recorded: Boolean
)
