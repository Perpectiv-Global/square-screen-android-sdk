package io.squarescreen.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.squarescreen.core.annotation.ExperimentalSquareScreenApi
import io.squarescreen.core.model.Playlist
import io.squarescreen.core.model.PlaylistItem
import io.squarescreen.core.model.TransitionType
import io.squarescreen.core.result.SquareScreenResult
import io.squarescreen.ui.internal.MediaItemView
import io.squarescreen.ui.internal.transitionSpecFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

/**
 * Renders the active playlist, handling item sequencing, duration timing,
 * transitions, and looping.
 *
 * @param nowPlaying Flow of the current playlist result from [io.squarescreen.player.SquareScreen].
 * @param modifier Modifier applied to the root container.
 * @param onItemCompleted Called when an item finishes its full display duration.
 *   Receives the completed item, the epoch-ms timestamp when it started, and when it ended.
 *   Use this for proof-of-play reporting. [io.squarescreen.ui.SquareScreenDisplay] wires
 *   this automatically — only set it when using [SquareScreenPlayerView] directly.
 * @param emptyContent Composable shown when the playlist has no items.
 * @param errorContent Composable shown on a persistent error with no cache fallback.
 */
@OptIn(ExperimentalSquareScreenApi::class)
@Composable
fun SquareScreenPlayerView(
    nowPlaying: Flow<SquareScreenResult<Playlist>>,
    modifier: Modifier = Modifier,
    onItemCompleted: ((item: PlaylistItem, startedAt: Long, endedAt: Long) -> Unit)? = null,
    emptyContent: @Composable () -> Unit = { DefaultEmptyContent() },
    errorContent: @Composable () -> Unit = {}
) {
    val result by nowPlaying.collectAsState(initial = null)

    when (val r = result) {
        null -> {
            Box(modifier = modifier.fillMaxSize().background(Color.Black))
        }
        is SquareScreenResult.Error -> {
            Box(modifier = modifier.fillMaxSize()) { errorContent() }
        }
        is SquareScreenResult.Success -> {
            val playlist = r.data
            if (playlist.items.isEmpty()) {
                Box(modifier = modifier.fillMaxSize()) { emptyContent() }
            } else {
                PlaylistRenderer(
                    playlist = playlist,
                    onItemCompleted = onItemCompleted,
                    modifier = modifier
                )
            }
        }
    }
}

@OptIn(ExperimentalSquareScreenApi::class)
@Composable
private fun PlaylistRenderer(
    playlist: Playlist,
    onItemCompleted: ((item: PlaylistItem, startedAt: Long, endedAt: Long) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val items = remember(playlist) {
        if (playlist.strategy?.shuffle == true) playlist.items.shuffled()
        else playlist.items
    }

    var currentIndex by remember(playlist) { mutableIntStateOf(0) }
    val currentItem = items[currentIndex]

    val effectiveTransition = currentItem.transition ?: TransitionType.NONE

    LaunchedEffect(currentIndex, playlist) {
        val startedAt = System.currentTimeMillis()
        delay(currentItem.duration * 1000L)
        val endedAt = System.currentTimeMillis()

        // Report the completed item before advancing
        onItemCompleted?.invoke(currentItem, startedAt, endedAt)

        val nextIndex = currentIndex + 1
        val loop = playlist.strategy?.loop != false
        if (nextIndex < items.size) {
            currentIndex = nextIndex
        } else if (loop) {
            currentIndex = 0
        }
    }

    AnimatedContent(
        targetState = currentItem,
        transitionSpec = { transitionSpecFor(effectiveTransition) },
        label = "playlist_item_transition",
        modifier = modifier.fillMaxSize()
    ) { item ->
        MediaItemView(item = item, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun DefaultEmptyContent() {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "No content scheduled", color = Color.White)
    }
}
