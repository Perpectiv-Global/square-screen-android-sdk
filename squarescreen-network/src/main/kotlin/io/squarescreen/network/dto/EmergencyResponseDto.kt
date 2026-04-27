package io.squarescreen.network.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class EmergencyResponseDto(
    val active: Boolean,
    val broadcast: EmergencyBroadcastDto? = null
)
