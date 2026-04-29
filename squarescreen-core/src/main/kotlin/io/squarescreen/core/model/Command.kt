package io.squarescreen.core.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * A command issued by the server to this device.
 *
 * @param id Unique identifier for this command — used when acknowledging.
 * @param type Command type string (e.g. "set_volume").
 * @param payload Type-specific parameters as a raw JSON object.
 */
@Serializable
data class Command(
    val id: String,
    val type: String,
    val payload: JsonObject = JsonObject(emptyMap())
)

/**
 * Typed interpretation of a [Command].
 *
 * Use [Command.toCommandType] to resolve the raw command into a typed variant.
 * Unknown command types are represented by [Unknown] — always handle this case
 * so future command types do not crash older SDK versions.
 */
sealed class CommandType {
    /** Set the device output volume to [volume] (0–100). */
    data class SetVolume(val volume: Int) : CommandType()

    /** A command type not recognised by this SDK version. */
    data class Unknown(val type: String, val rawPayload: JsonObject) : CommandType()
}

/**
 * Resolves this [Command] into a typed [CommandType].
 */
fun Command.toCommandType(): CommandType = when (type) {
    "set_volume" -> {
        val volume = payload["volume"]?.jsonPrimitive?.intOrNull ?: 0
        CommandType.SetVolume(volume)
    }
    else -> CommandType.Unknown(type, payload)
}
