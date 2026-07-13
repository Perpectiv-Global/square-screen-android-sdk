package io.squarescreen.core.model

/**
 * A single item in a playlist.
 *
 * @param id Unique identifier for this media file (matches the server's `id` field).
 * @param type Whether this item is an image or video. Inferred from the URL file extension
 *   by the network layer — the API does not return an explicit type field.
 * @param url CDN URL of the media file.
 * @param duration How long this item should be displayed, in seconds.
 * @param width Native width of the media in pixels.
 * @param height Native height of the media in pixels.
 * @param transition Optional transition to apply when this item appears.
 * @param title Optional human-readable title. Present on some item types (e.g. video).
 * @param thumbnail Optional thumbnail URL. Used for preloading previews.
 */
data class PlaylistItem(
    val id: String,
    val type: MediaType,
    val url: String,
    val duration: Int,
    val width: Int? = null,
    val height: Int? = null,
    val transition: TransitionType? = null,
    val title: String? = null,
    val thumbnail: String? = null,
    val playlistUuid: String? = null
)
