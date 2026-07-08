package io.squarescreen.cache.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores the single active playlist for the device.
 * A device only ever has one active playlist — we always upsert id=1.
 */
@Entity(tableName = "playlist")
internal data class PlaylistEntity(
    @PrimaryKey val id: Int = 1,
    val cachedAt: Long,
    // Strategy fields (flattened)
    val strategyLoop: Boolean?,
    val strategyShuffle: Boolean?,
    val strategyPreloadCount: Int?,
    // Schedule metadata
    val scheduleUuid: String?,
    val scheduleName: String?,
    val schedulePriority: Int?,
    // Playlist metadata
    val playlistUuid: String?,
    val playlistName: String?,
    val playlistServerId: String?
)
