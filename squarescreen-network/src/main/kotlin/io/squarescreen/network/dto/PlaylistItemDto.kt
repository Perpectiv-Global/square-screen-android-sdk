package io.squarescreen.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class PlaylistItemDto(
    val uuid: String,
    val name: String,
    val type: String,
    val url: String,
    @SerialName("duration_seconds") val durationSeconds: Int,
    val width: Int? = null,
    val height: Int? = null,
    val quality: String? = null,
    val transition: String? = null
)
