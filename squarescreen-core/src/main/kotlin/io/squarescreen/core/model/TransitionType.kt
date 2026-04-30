package io.squarescreen.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Visual transition applied when advancing from one [PlaylistItem] to the next.
 *
 * The transition is specified per item by the server. If a given item has no
 * explicit transition, the UI falls back to [NONE].
 */
@Serializable
enum class TransitionType {
    /** Cross-fade between items. */
    @SerialName("fade") FADE,
    /** Slide the next item in from the right. */
    @SerialName("slide") SLIDE,
    /** Cut directly to the next item with no animation. */
    @SerialName("none") NONE
}
