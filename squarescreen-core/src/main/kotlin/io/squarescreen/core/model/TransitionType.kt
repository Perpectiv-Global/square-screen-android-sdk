package io.squarescreen.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class TransitionType {
    @SerialName("fade") FADE,
    @SerialName("slide") SLIDE,
    @SerialName("none") NONE
}
