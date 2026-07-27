package io.squarescreen.network.dto

import io.squarescreen.core.model.PlaybackReport
import kotlinx.serialization.Serializable

@Serializable
internal data class PlaybackReportRequestDto(
    val playbacks: List<PlaybackReport>
)
