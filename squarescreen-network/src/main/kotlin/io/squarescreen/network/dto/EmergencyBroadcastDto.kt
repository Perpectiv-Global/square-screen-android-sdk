package io.squarescreen.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class EmergencyBroadcastDto(
    val id: Int,
    val uuid: String,
    @SerialName("company_id") val companyId: Int,
    val title: String,
    val message: String,
    @SerialName("background_color") val backgroundColor: String,
    @SerialName("text_color") val textColor: String,
    @SerialName("target_scope") val targetScope: String,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("started_at") val startedAt: String,
    @SerialName("ended_at") val endedAt: String? = null
)
