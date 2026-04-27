package io.squarescreen.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HeartbeatPayload(
    @SerialName("cpu_usage") val cpuUsage: Float?,
    @SerialName("memory_usage") val memoryUsage: Float?,
    @SerialName("disk_usage") val diskUsage: Float?,
    /**
     * Device temperature in Celsius. Best-effort — may be null on devices that do not
     * expose thermal data via BatteryManager or thermal HAL. Never throws if unavailable.
     */
    @SerialName("temperature") val temperature: Float?,
    @SerialName("os_version") val osVersion: String,
    @SerialName("player_version") val playerVersion: String
)
