package io.squarescreen.cache.db

import io.squarescreen.core.annotation.ExperimentalSquareScreenApi
import io.squarescreen.core.model.MediaType
import io.squarescreen.core.model.PlaybackStrategy
import io.squarescreen.core.model.Playlist
import io.squarescreen.core.model.PlaylistItem
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
                preloadCount = entity.strategyPreloadCount ?: 1,
                showThumbnail = entity.strategyShowThumbnail ?: false,
                defaultTransition = mapTransition(entity.strategyDefaultTransition) ?: TransitionType.NONE
            )
        } else null

        return Playlist(
            items = items.map { toPlaylistItem(it) },
            strategy = strategy,
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
            strategyShowThumbnail = strategy?.showThumbnail,
            strategyDefaultTransition = strategy?.defaultTransition?.name?.lowercase()
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
                transition = item.transition?.name?.lowercase(),
                sortOrder = index
            )
        }
    }

    private fun toPlaylistItem(entity: PlaylistItemEntity): PlaylistItem {
        return PlaylistItem(
            id = entity.id,
            type = when (entity.type.lowercase()) {
                "image" -> MediaType.IMAGE
                "video" -> MediaType.VIDEO
                else -> MediaType.IMAGE
            },
            url = entity.url,
            duration = entity.duration,
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
