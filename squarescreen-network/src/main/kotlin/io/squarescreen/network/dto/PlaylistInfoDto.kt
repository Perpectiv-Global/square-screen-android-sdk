package io.squarescreen.network.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class PlaylistInfoDto(
    val id: String? = null,
    val uuid: String,
    val name: String
)
