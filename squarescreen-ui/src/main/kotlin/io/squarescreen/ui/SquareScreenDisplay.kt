package io.squarescreen.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.squarescreen.player.SquareScreen

/**
 * Convenience Composable that combines [SquareScreenPlayerView] and [EmergencyOverlayView]
 * into a single drop-in component.
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
 * For integrators who need to customise the empty state or error state, use
 * [SquareScreenPlayerView] and [EmergencyOverlayView] directly instead.
 *
 * @param squareScreen The initialized [SquareScreen] instance.
 * @param modifier Modifier applied to the root container.
 * @param emptyContent Composable shown when the playlist has no items scheduled.
 * @param errorContent Composable shown on a persistent network error with no cache.
 */
@Composable
fun SquareScreenDisplay(
    squareScreen: SquareScreen,
    modifier: Modifier = Modifier,
    emptyContent: @Composable () -> Unit = {},
    errorContent: @Composable () -> Unit = {}
) {
    Box(modifier = modifier.fillMaxSize()) {
        SquareScreenPlayerView(
            nowPlaying = squareScreen.nowPlaying,
            modifier = Modifier.fillMaxSize(),
            emptyContent = emptyContent,
            errorContent = errorContent
        )
        EmergencyOverlayView(
            emergencyAlert = squareScreen.emergencyAlert,
            modifier = Modifier.fillMaxSize()
        )
    }
}
