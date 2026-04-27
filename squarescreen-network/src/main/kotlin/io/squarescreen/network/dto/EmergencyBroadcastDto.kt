package io.squarescreen.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class EmergencyBroadcastDto(
    val id: String,
    val title: String,
    val message: String,
    @SerialName("background_color") val backgroundColor: String,
    @SerialName("text_color") val textColor: String
)
