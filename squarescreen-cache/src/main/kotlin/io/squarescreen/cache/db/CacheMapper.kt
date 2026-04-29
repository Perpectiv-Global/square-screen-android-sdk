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
                shuffle = entity.strategyShuffle ?: false
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
                uuid = item.uuid,
                playlistId = 1,
                name = item.name,
                type = item.type.name.lowercase(),
                url = item.url,
                durationSeconds = item.durationSeconds,
                width = item.width,
                height = item.height,
                quality = item.quality,
                transition = item.transition?.name?.lowercase(),
                sortOrder = index
            )
        }
    }

    private fun toPlaylistItem(entity: PlaylistItemEntity): PlaylistItem {
        return PlaylistItem(
            uuid = entity.uuid,
            name = entity.name,
            type = when (entity.type.lowercase()) {
                "image" -> MediaType.IMAGE
                "video" -> MediaType.VIDEO
                else -> MediaType.IMAGE
            },
            url = entity.url,
            durationSeconds = entity.durationSeconds,
            width = entity.width,
            height = entity.height,
            quality = entity.quality,
            transition = mapTransition(entity.transition)
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
