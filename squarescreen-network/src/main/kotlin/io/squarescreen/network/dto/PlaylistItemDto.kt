package io.squarescreen.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class PlaylistItemDto(
    val id: Int,
    val type: String,
    val url: String,
    val duration: Int,
    val transition: String? = null
)
