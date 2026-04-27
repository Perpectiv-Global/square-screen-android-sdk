package io.squarescreen.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlaylistItem(
    val id: Int,
    val type: MediaType,
    val url: String,
    /** Duration in seconds this item should be displayed. */
    val duration: Int,
    val transition: TransitionType? = null
)
