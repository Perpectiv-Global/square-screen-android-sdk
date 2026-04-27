package io.squarescreen.core.cache

import io.squarescreen.core.model.Playlist
import java.io.File

/**
 * Abstraction over local storage. The default implementation lives in `squarescreen-cache`
 * (Room + disk). Integrators who already have a media download manager can supply their own
 * implementation via [io.squarescreen.core.config.SquareScreenConfig.cacheProvider].
 */
interface CacheProvider {

    /** Returns the most recently cached playlist, or null if nothing is cached. */
    suspend fun getPlaylist(): Playlist?

    /** Persists a playlist to the local cache. */
    suspend fun savePlaylist(playlist: Playlist)

    /**
     * Returns the locally cached media file for the given [url], or null if not cached.
     * File names are derived from a hash of the URL to avoid collisions.
     */
    suspend fun getMediaFile(url: String): File?

    /**
     * Stores [file] in the local media cache, keyed by [url].
     */
    suspend fun saveMediaFile(url: String, file: File)

    /** Removes all cached data (playlist metadata and media files). */
    suspend fun clearAll()
}
