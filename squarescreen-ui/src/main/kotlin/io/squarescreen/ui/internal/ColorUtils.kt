package io.squarescreen.ui.internal

import androidx.compose.ui.graphics.Color

/**
 * Parses a hex color string (e.g. "#FF0000" or "FF0000") to a Compose [Color].
 * Returns [fallback] if the string cannot be parsed.
 */
internal fun parseHexColor(hex: String, fallback: Color = Color.Black): Color {
    return try {
        Color(android.graphics.Color.parseColor(
            if (hex.startsWith("#")) hex else "#$hex"
        ))
    } catch (e: Exception) {
        fallback
    }
}
