package io.squarescreen.core.config

/**
 * Configuration for the persistent foreground service notification that keeps
 * the player alive during active playback.
 *
 * @param title Text shown as the notification title.
 * @param iconResId Drawable resource ID for the notification small icon.
 */
data class ForegroundNotificationConfig(
    val title: String,
    val iconResId: Int
)
