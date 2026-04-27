package io.squarescreen.network.mapper

import io.squarescreen.core.annotation.ExperimentalSquareScreenApi
import io.squarescreen.core.model.EmergencyAlert
import io.squarescreen.core.model.MediaType
import io.squarescreen.core.model.PlaybackStrategy
import io.squarescreen.core.model.Playlist
import io.squarescreen.core.model.PlaylistItem
import io.squarescreen.core.model.TransitionType
import io.squarescreen.network.dto.EmergencyBroadcastDto
import io.squarescreen.network.dto.NowPlayingResponseDto
import io.squarescreen.network.dto.PlaybackStrategyDto
import io.squarescreen.network.dto.PlaylistItemDto

@OptIn(ExperimentalSquareScreenApi::class)
internal object NetworkMapper {

    fun mapNowPlaying(dto: NowPlayingResponseDto, cachedAt: Long): Playlist {
        return Playlist(
            items = dto.items.map { mapPlaylistItem(it) },
            strategy = dto.strategy?.let { mapStrategy(it) },
            cachedAt = cachedAt
        )
    }

    private fun mapPlaylistItem(dto: PlaylistItemDto): PlaylistItem {
        return PlaylistItem(
            id = dto.id,
            type = when (dto.type.lowercase()) {
                "image" -> MediaType.IMAGE
                "video" -> MediaType.VIDEO
                else -> MediaType.IMAGE
            },
            url = dto.url,
            duration = dto.duration,
            transition = dto.transition?.let { mapTransition(it) }
        )
    }

    private fun mapStrategy(dto: PlaybackStrategyDto): PlaybackStrategy {
        return PlaybackStrategy(
            loop = dto.loop,
            shuffle = dto.shuffle,
            preloadCount = dto.preloadCount,
            showThumbnail = dto.showThumbnail,
            defaultTransition = mapTransition(dto.defaultTransition) ?: TransitionType.NONE
        )
    }

    private fun mapTransition(value: String): TransitionType? {
        return when (value.lowercase()) {
            "fade" -> TransitionType.FADE
            "slide" -> TransitionType.SLIDE
            "none" -> TransitionType.NONE
            else -> null
        }
    }

    fun mapEmergencyBroadcast(dto: EmergencyBroadcastDto): EmergencyAlert {
        return EmergencyAlert(
            id = dto.id,
            title = dto.title,
            message = dto.message,
            backgroundColor = dto.backgroundColor,
            textColor = dto.textColor
        )
    }
}
