package io.squarescreen.core.model

/**
 * A single item in a playlist.
 *
 * @param uuid Unique identifier for this media file.
 * @param name Human-readable name for this media asset.
 * @param type Whether this item is an image or video.
 * @param url CDN URL of the media file.
 * @param durationSeconds How long this item should be displayed, in seconds.
 * @param width Native width of the media in pixels.
 * @param height Native height of the media in pixels.
 * @param quality Quality descriptor (e.g. "hd", "sd").
 * @param transition Optional transition to apply when this item appears. If null,
 *   the [PlaybackStrategy.defaultTransition] is used.
 */
data class PlaylistItem(
    val uuid: String,
    val name: String,
    val type: MediaType,
    val url: String,
    val durationSeconds: Int,
    val width: Int? = null,
    val height: Int? = null,
    val quality: String? = null,
    val transition: TransitionType? = null
)
