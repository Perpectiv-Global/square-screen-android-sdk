package io.squarescreen.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.squarescreen.core.model.EmergencyAlert
import io.squarescreen.ui.internal.parseHexColor
import kotlinx.coroutines.flow.Flow

/**
 * Full-screen emergency alert overlay.
 *
 * Renders on top of all content when an emergency broadcast is active.
 * Automatically dismisses (fades out) when [emergencyAlert] emits null.
 *
 * Place this above [SquareScreenPlayerView] in your layout hierarchy so it
 * overlays the playlist content:
 *
 * ```kotlin
 * Box(modifier = Modifier.fillMaxSize()) {
 *     SquareScreenPlayerView(nowPlaying = squareScreen.nowPlaying)
 *     EmergencyOverlayView(emergencyAlert = squareScreen.emergencyAlert)
 * }
 * ```
 *
 * @param emergencyAlert Flow of the current emergency alert from [io.squarescreen.player.SquareScreen].
 * @param modifier Modifier applied to the overlay container.
 */
@Composable
fun EmergencyOverlayView(
    emergencyAlert: Flow<EmergencyAlert?>,
    modifier: Modifier = Modifier
) {
    val alert by emergencyAlert.collectAsState(initial = null)

    AnimatedVisibility(
        visible = alert != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize()
    ) {
        alert?.let { activeAlert ->
            EmergencyAlertContent(alert = activeAlert)
        }
    }
}

@Composable
private fun EmergencyAlertContent(alert: EmergencyAlert) {
    val backgroundColor = parseHexColor(alert.backgroundColor, fallback = Color.Red)
    val textColor = parseHexColor(alert.textColor, fallback = Color.White)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 48.dp)
        ) {
            Text(
                text = alert.title,
                color = textColor,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = alert.message,
                color = textColor,
                fontSize = 24.sp,
                textAlign = TextAlign.Center,
                lineHeight = 32.sp
            )
        }
    }
}
