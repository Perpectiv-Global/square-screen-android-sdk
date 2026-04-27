package io.squarescreen.network.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class NowPlayingResponseDto(
    val items: List<PlaylistItemDto> = emptyList(),
    val strategy: PlaybackStrategyDto? = null
)
