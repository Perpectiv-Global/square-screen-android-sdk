package io.squarescreen.network.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
internal data class CommandDto(
    val id: String,
    val type: String,
    val payload: JsonObject = JsonObject(emptyMap())
)

@Serializable
internal data class CommandsResponseDto(
    val commands: List<CommandDto> = emptyList()
)

@Serializable
internal data class AckRequestDto(
    val status: String,
    val result: Map<String, String> = emptyMap()
)

@Serializable
internal data class AckResponseDto(
    val accepted: Boolean
)
