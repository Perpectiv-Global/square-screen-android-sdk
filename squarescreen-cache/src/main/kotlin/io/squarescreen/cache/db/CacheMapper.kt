package io.squarescreen.cache.db

import io.squarescreen.core.annotation.ExperimentalSquareScreenApi
import io.squarescreen.core.model.MediaType
import io.squarescreen.core.model.PlaybackStrategy
import io.squarescreen.core.model.Playlist
import io.squarescreen.core.model.PlaylistInfo
import io.squarescreen.core.model.PlaylistItem
import io.squarescreen.core.model.ScheduleInfo
import io.squarescreen.core.model.TransitionType
import io.squarescreen.cache.db.entity.PlaylistEntity
import io.squarescreen.cache.db.entity.PlaylistItemEntity

@OptIn(ExperimentalSquareScreenApi::class)
internal object CacheMapper {

    fun toPlaylist(entity: PlaylistEntity, items: List<PlaylistItemEntity>): Playlist {
        val strategy = if (entity.strategyLoop != null) {
            PlaybackStrategy(
                loop = entity.strategyLoop,
                shuffle = entity.strategyShuffle ?: false,
                preloadCount = entity.strategyPreloadCount ?: 1
            )
        } else null

        val schedule = if (entity.scheduleUuid != null && entity.scheduleName != null) {
            ScheduleInfo(
                uuid = entity.scheduleUuid,
                name = entity.scheduleName,
                priority = entity.schedulePriority ?: 0
            )
        } else null

        val playlistInfo = if (entity.playlistUuid != null && entity.playlistName != null) {
            PlaylistInfo(uuid = entity.playlistUuid, name = entity.playlistName)
        } else null

        return Playlist(
            items = items.map { toPlaylistItem(it) },
            strategy = strategy,
            schedule = schedule,
            playlist = playlistInfo,
            cachedAt = entity.cachedAt
        )
    }

    fun toEntity(playlist: Playlist): PlaylistEntity {
        val strategy = playlist.strategy
        return PlaylistEntity(
            id = 1,
            cachedAt = playlist.cachedAt,
            strategyLoop = strategy?.loop,
            strategyShuffle = strategy?.shuffle,
            strategyPreloadCount = strategy?.preloadCount,
            scheduleUuid = playlist.schedule?.uuid,
            scheduleName = playlist.schedule?.name,
            schedulePriority = playlist.schedule?.priority,
            playlistUuid = playlist.playlist?.uuid,
            playlistName = playlist.playlist?.name
        )
    }

    fun toItemEntities(playlist: Playlist): List<PlaylistItemEntity> {
        return playlist.items.mapIndexed { index, item ->
            PlaylistItemEntity(
                id = item.id,
                playlistId = 1,
                type = item.type.name.lowercase(),
                url = item.url,
                duration = item.duration,
                width = item.width,
                height = item.height,
                transition = item.transition?.name?.lowercase(),
                title = item.title,
                thumbnail = item.thumbnail,
                sortOrder = index
            )
        }
    }

    private fun toPlaylistItem(entity: PlaylistItemEntity): PlaylistItem {
        return PlaylistItem(
            id = entity.id,
            // Type was inferred at network time and stored as a string.
            // Re-map back to the enum on read.
            type = when (entity.type.lowercase()) {
                "video" -> MediaType.VIDEO
                else -> MediaType.IMAGE
            },
            url = entity.url,
            duration = entity.duration,
            width = entity.width,
            height = entity.height,
            transition = mapTransition(entity.transition),
            title = entity.title,
            thumbnail = entity.thumbnail
        )
    }

    private fun mapTransition(value: String?): TransitionType? {
        return when (value?.lowercase()) {
            "fade" -> TransitionType.FADE
            "slide" -> TransitionType.SLIDE
            "none" -> TransitionType.NONE
            else -> null
        }
    }
}
