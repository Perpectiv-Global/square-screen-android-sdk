package io.squarescreen.core.model

/**
 * Metadata about the playlist being played.
 *
 * @param uuid Unique identifier for the playlist.
 * @param name Human-readable playlist name.
 */
data class PlaylistInfo(
    val uuid: String,
    val name: String
)
