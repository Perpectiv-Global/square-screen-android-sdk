package io.squarescreen.cache.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "playlist_item",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("playlistId")]
)
internal data class PlaylistItemEntity(
    @PrimaryKey val id: Int,
    val playlistId: Int = 1,
    val type: String,
    val url: String,
    val duration: Int,
    val transition: String?,
    /** Preserves server-defined display order. */
    val sortOrder: Int
)
