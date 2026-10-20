package io.squarescreen.network.mapper

import io.squarescreen.core.annotation.ExperimentalSquareScreenApi
import io.squarescreen.core.model.Command
import io.squarescreen.core.model.EmergencyAlert
import io.squarescreen.core.model.MediaType
import io.squarescreen.core.model.PlaybackStrategy
import io.squarescreen.core.model.Playlist
import io.squarescreen.core.model.PlaylistInfo
import io.squarescreen.core.model.PlaylistItem
import io.squarescreen.core.model.ScheduleInfo
import io.squarescreen.core.model.TransitionType
import io.squarescreen.network.dto.CommandDto
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
            schedule = dto.schedule?.let { ScheduleInfo(it.uuid, it.name, it.priority) },
            playlist = dto.playlist?.let { PlaylistInfo(it.id, it.uuid, it.name) },
            cachedAt = cachedAt
        )
    }

    private fun mapPlaylistItem(dto: PlaylistItemDto): PlaylistItem {
        return PlaylistItem(
            id = dto.id,
            // The API no longer returns an explicit type field. Infer from the URL
            // file extension — this is reliable for CDN-hosted media files.
            type = inferMediaType(dto.url),
            url = dto.url,
            duration = dto.duration,
            width = dto.width,
            height = dto.height,
            transition = dto.transition?.let { mapTransition(it) },
            title = dto.title,
            thumbnail = dto.thumbnail,
            playlistUuid = dto.playlistUuid
        )
    }

    /**
     * Infers [MediaType] from the file extension of [url].
     * Query parameters and fragments are stripped from the path before extracting the
     * extension. Doing this first is critical: presigned URLs (CloudFront, S3) embed dots
     * inside query values (e.g. Signature=abc.def.xyz), so calling substringAfterLast('.')
     * on the raw URL picks up the wrong dot and the extension check silently falls through
     * to IMAGE.
     * Defaults to [MediaType.IMAGE] for unrecognised or missing extensions.
     */
    private fun inferMediaType(url: String): MediaType {
        val path = url.substringBefore('?').substringBefore('#')
        val extension = path.substringAfterLast('.', missingDelimiterValue = "").lowercase()
        return when (extension) {
            "mp4", "mov", "avi", "mkv", "webm", "m4v", "ts" -> MediaType.VIDEO
            else -> MediaType.IMAGE
        }
    }

    private fun mapStrategy(dto: PlaybackStrategyDto): PlaybackStrategy {
        return PlaybackStrategy(
            loop = dto.loop,
            shuffle = dto.shuffle,
            preloadCount = dto.preloadCount
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
            uuid = dto.uuid,
            companyId = dto.companyId,
            title = dto.title,
            message = dto.message,
            backgroundColor = dto.backgroundColor,
            textColor = dto.textColor,
            targetScope = dto.targetScope,
            isActive = dto.isActive,
            startedAt = dto.startedAt,
            endedAt = dto.endedAt
        )
    }

    fun mapCommand(dto: CommandDto): Command {
        return Command(
            id = dto.id,
            type = dto.type,
            payload = dto.payload
        )
    }
}
