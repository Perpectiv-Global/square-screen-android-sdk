package io.squarescreen.core.model

/**
 * An active emergency broadcast targeting this device.
 *
 * @param id Server-assigned integer ID.
 * @param uuid Universally unique identifier for this broadcast.
 * @param companyId ID of the company that issued the broadcast.
 * @param title Short title displayed prominently on the overlay.
 * @param message Body text of the emergency message.
 * @param backgroundColor Hex color string for the overlay background (e.g. "#FF3B30").
 * @param textColor Hex color string for the overlay text (e.g. "#FFFFFF").
 * @param targetScope Scope of this broadcast ("all", "workspace", "group", or "device").
 * @param isActive Whether this broadcast is currently active.
 * @param startedAt ISO 8601 timestamp when the broadcast started.
 * @param endedAt ISO 8601 timestamp when the broadcast ended, or null if still active.
 */
data class EmergencyAlert(
    val id: Int,
    val uuid: String,
    val companyId: Int,
    val title: String,
    val message: String,
    /** Hex color string, e.g. "#FF3B30". */
    val backgroundColor: String,
    /** Hex color string, e.g. "#FFFFFF". */
    val textColor: String,
    val targetScope: String,
    val isActive: Boolean,
    val startedAt: String,
    val endedAt: String? = null
)
