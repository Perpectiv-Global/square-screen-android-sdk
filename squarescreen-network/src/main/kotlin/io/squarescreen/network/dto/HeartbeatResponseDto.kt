package io.squarescreen.network.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class HeartbeatResponseDto(
    val success: Boolean
)
