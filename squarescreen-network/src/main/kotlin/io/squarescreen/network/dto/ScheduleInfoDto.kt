package io.squarescreen.network.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class ScheduleInfoDto(
    val uuid: String,
    val name: String,
    val priority: Int
)
