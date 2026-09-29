package io.squarescreen.sample.ui

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.squarescreen.core.model.CommandType
import io.squarescreen.core.model.MediaType
import io.squarescreen.core.model.PlaylistItem
import io.squarescreen.core.model.toCommandType
import io.squarescreen.core.result.SquareScreenResult
import io.squarescreen.player.SquareScreen
import io.squarescreen.sample.data.PendingPlaybackStore
import io.squarescreen.ui.SquareScreenDisplay
import kotlinx.coroutines.launch

@Composable
fun PlayerScreen(squareScreen: SquareScreen) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val commands by squareScreen.commands.collectAsState(initial = emptyList())

    val pendingStore = remember { PendingPlaybackStore(context) }

    var currentItem by remember { mutableStateOf<PlaylistItem?>(null) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }

    LaunchedEffect(currentItem) {
        elapsedSeconds = 0
        val item = currentItem ?: return@LaunchedEffect
        while (elapsedSeconds < item.duration) {
            kotlinx.coroutines.delay(1_000L)
            elapsedSeconds++
        }
    }

    // Keep the screen on while the player is active.
    DisposableEffect(Unit) {
        val window = (view.context as Activity).window
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    // When network reconnects, flush any cached failed playback reports.
    DisposableEffect(Unit) {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                if (pendingStore.isEmpty) return
                scope.launch {
                    val pending = pendingStore.getAll()
                    val result = squareScreen.reportPlayback(pending)
                    if (result is SquareScreenResult.Success) {
                        pendingStore.clear()
                    }
                }
            }
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, callback)
        onDispose { connectivityManager.unregisterNetworkCallback(callback) }
    }

    // Handle incoming server commands.
    LaunchedEffect(commands) {
        commands.forEach { command ->
            scope.launch {
                when (val type = command.toCommandType()) {
                    is CommandType.SetVolume -> {
                        squareScreen.acknowledgeCommand(
                            commandId = command.id,
                            status = "completed",
                            result = mapOf("message" to "Volume set to ${type.volume}")
                        )
                    }
                    is CommandType.Unknown -> {
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
        SquareScreenDisplay(
            squareScreen = squareScreen,
            modifier = Modifier.fillMaxSize(),
            onReportFailed = { report -> pendingStore.add(report) },
            onItemStarted = { item -> currentItem = item },
            emptyContent = { NoContentPlaceholder() },
            errorContent = { ErrorPlaceholder() }
        )

    }
}

@Composable
private fun NoContentPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
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

@Composable
private fun ErrorPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
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

@Composable
private fun OfflineBadge() {
    Box(
        modifier = Modifier
            .background(color = Color(0xFFFF9500), shape = RoundedCornerShape(4.dp))
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

@Composable
private fun MediaTypeBadge(type: MediaType) {
    Box(
        modifier = Modifier
            .background(
                color = Color.Black.copy(alpha = 0.5f),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = if (type == MediaType.IMAGE) "IMAGE" else "VIDEO",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun PlaybackCounter(elapsed: Int, total: Int) {
    Box(
        modifier = Modifier
            .background(
                color = Color.Black.copy(alpha = 0.5f),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = "${elapsed}s / ${total}s",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}
