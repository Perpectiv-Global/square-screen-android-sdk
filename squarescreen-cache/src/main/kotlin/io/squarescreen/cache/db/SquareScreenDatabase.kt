package io.squarescreen.cache.db

import androidx.room.Database
import androidx.room.RoomDatabase
import io.squarescreen.cache.db.dao.PlaylistDao
import io.squarescreen.cache.db.entity.PlaylistEntity
import io.squarescreen.cache.db.entity.PlaylistItemEntity

@Database(
    entities = [PlaylistEntity::class, PlaylistItemEntity::class],
    version = 4,
    exportSchema = false
)
internal abstract class SquareScreenDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
}
