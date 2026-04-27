package io.squarescreen.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EmergencyAlert(
    val id: String,
    val title: String,
    val message: String,
    /** Hex color string, e.g. "#FF0000". */
    @SerialName("background_color") val backgroundColor: String,
    /** Hex color string, e.g. "#FFFFFF". */
    @SerialName("text_color") val textColor: String
)
