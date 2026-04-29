package io.squarescreen.sample.ui

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.squarescreen.core.model.CommandType
import io.squarescreen.core.model.DeviceStatus
import io.squarescreen.core.model.toCommandType
import io.squarescreen.player.SquareScreen
import io.squarescreen.ui.SquareScreenDisplay
import kotlinx.coroutines.launch

/**
 * The main player screen. This is intentionally kept simple — a real kiosk app
 * might add PIN-protected settings access, but the core display logic lives here.
 *
 * What this screen does:
 * - Renders the active playlist via [SquareScreenDisplay], which also shows the
 *   emergency overlay and handles proof-of-play reporting automatically.
 * - Shows an offline indicator when the device loses network connectivity.
 * - Observes the commands flow and handles each command type, then acknowledges it.
 */
@Composable
fun PlayerScreen(squareScreen: SquareScreen) {
    val scope = rememberCoroutineScope()
    val deviceStatus by squareScreen.deviceStatus.collectAsState(initial = DeviceStatus.CONNECTING)
    val commands by squareScreen.commands.collectAsState(initial = emptyList())

    // Handle incoming server commands.
    //
    // The commands flow emits whenever the command-poll worker retrieves new commands.
    // Each command must be acknowledged after handling so the server knows it was executed.
    // Unknown command types are acknowledged with "failed" so the server can log them —
    // silently dropping commands would make remote management impossible to debug.
    LaunchedEffect(commands) {
        commands.forEach { command ->
            scope.launch {
                when (val type = command.toCommandType()) {
                    is CommandType.SetVolume -> {
                        // In a production player, apply the volume change via AudioManager:
                        // val am = context.getSystemService(AudioManager::class.java)
                        // am.setStreamVolume(AudioManager.STREAM_MUSIC, type.volume, 0)
                        squareScreen.acknowledgeCommand(
                            commandId = command.id,
                            status = "completed",
                            result = mapOf("message" to "Volume set to ${type.volume}")
                        )
                    }
                    is CommandType.Unknown -> {
                        // Always acknowledge unknown commands so the server doesn't
                        // keep re-sending them. Log the type so it can be investigated.
                        squareScreen.acknowledgeCommand(
                            commandId = command.id,
                            status = "failed",
                            result = mapOf("message" to "Unsupported command type: ${type.type}")
                        )
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // SquareScreenDisplay is the single Composable that renders everything:
        //   - The playlist (images and videos in sequence)
        //   - The emergency overlay (full-screen, auto-dismisses when cleared)
        //   - Proof-of-play reporting (fires automatically after each item)
        //
        // The emptyContent lambda is shown when the server returns an empty playlist —
        // e.g. outside scheduled hours or when no content is assigned to this device.
        SquareScreenDisplay(
            squareScreen = squareScreen,
            modifier = Modifier.fillMaxSize(),
            emptyContent = { NoContentPlaceholder() },
            errorContent = { ErrorPlaceholder() }
        )

        // Offline indicator — shown when the SDK is serving cached content because
        // the device has lost network connectivity. Positioned in the top-left corner
        // so it doesn't interfere with the content but is visible to operations staff.
        AnimatedVisibility(
            visible = deviceStatus == DeviceStatus.OFFLINE,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(12.dp)
        ) {
            OfflineBadge()
        }
    }
}

/**
 * Shown when no content is scheduled for this device.
 *
 * In production you might display a branded holding screen or the company logo.
 * Keep it simple — it should be obvious to operations staff that the device is
 * healthy but has nothing to play right now.
 */
@Composable
private fun NoContentPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "No content scheduled",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Light
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Assign a playlist in the SquareScreen dashboard",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 14.sp
            )
        }
    }
}

/**
 * Shown when there is a persistent network error and no cached content is available.
 *
 * This state should be rare — the SDK aggressively caches content for offline continuity.
 * If this screen appears, the device has never successfully fetched a playlist.
 */
@Composable
private fun ErrorPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Unable to load content",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Light
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Check network connectivity and device credentials",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 14.sp
            )
        }
    }
}

/**
 * Small badge shown in the corner when the device is offline and serving cached content.
 */
@Composable
private fun OfflineBadge() {
    Box(
        modifier = Modifier
            .background(
                color = Color(0xFFFF9500),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = "OFFLINE",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}
