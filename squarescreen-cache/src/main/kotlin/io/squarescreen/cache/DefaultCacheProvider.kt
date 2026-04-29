package io.squarescreen.cache

import android.content.Context
import androidx.room.Room
import io.squarescreen.cache.db.CacheMapper
import io.squarescreen.cache.db.SquareScreenDatabase
import io.squarescreen.cache.file.MediaFileCache
import io.squarescreen.core.cache.CacheProvider
import io.squarescreen.core.model.Playlist
import java.io.File

/**
 * Default [CacheProvider] implementation backed by Room (metadata) and disk (media files).
 *
 * Integrators who want to supply their own caching strategy can implement [CacheProvider]
 * and pass it via [io.squarescreen.core.config.SquareScreenConfig.cacheProvider].
 *
 * @param context Application context used to locate the database and external files dir.
 * @param ttlSeconds How long a cached playlist is considered fresh. Defaults to 3600s (1 hour).
 */
class DefaultCacheProvider(
    context: Context,
    private val ttlSeconds: Long = 3600L
) : CacheProvider {

    private val db: SquareScreenDatabase = Room.databaseBuilder(
        context.applicationContext,
        SquareScreenDatabase::class.java,
        "squarescreen.db"
    )
        .fallbackToDestructiveMigration()
        .build()

    private val mediaCache = MediaFileCache(
        cacheDir = context.getExternalFilesDir("squarescreen_media")
            ?: File(context.filesDir, "squarescreen_media")
    )

    private val dao = db.playlistDao()

    override suspend fun getPlaylist(): Playlist? {
        val entity = dao.getPlaylist() ?: return null
        val ageSeconds = (System.currentTimeMillis() - entity.cachedAt) / 1000
        if (ageSeconds > ttlSeconds) return null
        val items = dao.getPlaylistItems()
        return CacheMapper.toPlaylist(entity, items)
    }

    override suspend fun savePlaylist(playlist: Playlist) {
        val entity = CacheMapper.toEntity(playlist)
        val items = CacheMapper.toItemEntities(playlist)
        dao.replacePlaylist(entity, items)
    }

    override suspend fun getMediaFile(url: String): File? {
        return mediaCache.getFile(url)
    }

    override suspend fun saveMediaFile(url: String, file: File) {
        mediaCache.saveFile(url, file)
    }

    override suspend fun clearAll() {
        dao.clearAll()
        mediaCache.clearAll()
    }
}
