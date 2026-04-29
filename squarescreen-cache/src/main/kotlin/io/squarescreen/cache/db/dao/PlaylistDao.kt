package io.squarescreen.cache.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import io.squarescreen.cache.db.entity.PlaylistEntity
import io.squarescreen.cache.db.entity.PlaylistItemEntity

@Dao
internal interface PlaylistDao {

    @Query("SELECT * FROM playlist WHERE id = 1 LIMIT 1")
    suspend fun getPlaylist(): PlaylistEntity?

    @Query("SELECT * FROM playlist_item WHERE playlistId = 1 ORDER BY sortOrder ASC")
    suspend fun getPlaylistItems(): List<PlaylistItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlaylist(playlist: PlaylistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItems(items: List<PlaylistItemEntity>)

    @Query("DELETE FROM playlist_item WHERE playlistId = 1")
    suspend fun deleteAllItems()

    @Query("DELETE FROM playlist")
    suspend fun deletePlaylist()

    @Transaction
    suspend fun replacePlaylist(playlist: PlaylistEntity, items: List<PlaylistItemEntity>) {
        deleteAllItems()
        upsertPlaylist(playlist)
        upsertItems(items)
    }

    @Transaction
    suspend fun clearAll() {
        deleteAllItems()
        deletePlaylist()
    }
}
