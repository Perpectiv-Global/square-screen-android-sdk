package io.squarescreen.core.model

/**
 * Metadata about the schedule driving the current playlist.
 *
 * @param uuid Unique identifier for the schedule.
 * @param name Human-readable schedule name.
 * @param priority Scheduling priority (higher value = higher priority).
 */
data class ScheduleInfo(
    val uuid: String,
    val name: String,
    val priority: Int
)
