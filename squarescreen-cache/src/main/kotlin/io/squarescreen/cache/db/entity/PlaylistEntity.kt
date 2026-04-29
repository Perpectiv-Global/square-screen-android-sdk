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
    // Strategy fields (flattened — avoids nested object complexity in Room)
    val strategyLoop: Boolean?,
    val strategyShuffle: Boolean?,
    val strategyPreloadCount: Int?,
    val strategyShowThumbnail: Boolean?,
    val strategyDefaultTransition: String?
)
