package io.squarescreen.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class PlaylistItemDto(
    val id: String,
    val url: String,
    val duration: Int,
    val width: Int? = null,
    val height: Int? = null,
    val transition: String? = null,
    val title: String? = null,
    val thumbnail: String? = null,
    @SerialName("playlist_uuid") val playlistUuid: String? = null
)
