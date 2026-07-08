package io.squarescreen.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import io.squarescreen.core.model.PlaybackReport
import io.squarescreen.core.result.SquareScreenResult
import io.squarescreen.player.SquareScreen
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Convenience Composable that combines [SquareScreenPlayerView] and [EmergencyOverlayView]
 * into a single drop-in component.
 *
 * Automatically reports proof-of-play via [SquareScreen.reportPlayback] after each item
 * finishes displaying.
 *
 * Usage:
 * ```kotlin
 * val squareScreen = SquareScreen.getInstance()
 *
 * SquareScreenDisplay(
 *     squareScreen = squareScreen,
 *     modifier = Modifier.fillMaxSize()
 * )
 * ```
 *
 * @param squareScreen The initialized [SquareScreen] instance.
 * @param modifier Modifier applied to the root container.
 * @param emptyContent Composable shown when no content is scheduled.
 * @param errorContent Composable shown on a persistent network error with no cache.
 */
@Composable
fun SquareScreenDisplay(
    squareScreen: SquareScreen,
    modifier: Modifier = Modifier,
    emptyContent: @Composable () -> Unit = {},
    errorContent: @Composable () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val nowPlayingResult by squareScreen.nowPlaying.collectAsState(initial = null)
    val currentPlaylist = (nowPlayingResult as? SquareScreenResult.Success)?.data

    Box(modifier = modifier.fillMaxSize()) {
        SquareScreenPlayerView(
            nowPlaying = squareScreen.nowPlaying,
            modifier = Modifier.fillMaxSize(),
            onItemCompleted = { item, startedAt, endedAt ->
                scope.launch {
                    squareScreen.reportPlayback(
                        PlaybackReport(
                            mediaUuid = item.id,
                            playlistUuid = currentPlaylist?.playlist?.uuid,
                            playlistId = currentPlaylist?.playlist?.id,
                            scheduleUuid = currentPlaylist?.schedule?.uuid,
                            startedAt = formatIso8601(startedAt),
                            endedAt = formatIso8601(endedAt),
                            durationSeconds = item.duration,
                            completed = true
                        )
                    )
                }
            },
            emptyContent = emptyContent,
            errorContent = errorContent
        )
        EmergencyOverlayView(
            emergencyAlert = squareScreen.emergencyAlert,
            modifier = Modifier.fillMaxSize()
        )
    }
}

private val isoFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
    timeZone = TimeZone.getTimeZone("UTC")
}

private fun formatIso8601(epochMs: Long): String = isoFormatter.format(Date(epochMs))
