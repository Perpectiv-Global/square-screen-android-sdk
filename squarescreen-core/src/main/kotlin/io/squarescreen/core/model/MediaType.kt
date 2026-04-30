package io.squarescreen.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Media type of a [PlaylistItem].
 *
 * The SquareScreen API does not return an explicit type field — the SDK infers
 * the type from the URL file extension in the network layer.
 */
@Serializable
enum class MediaType {
    /** A still image (JPEG, PNG, WebP, GIF, etc.). */
    @SerialName("image") IMAGE,
    /** A video file (MP4, MOV, WebM, etc.). */
    @SerialName("video") VIDEO
}
