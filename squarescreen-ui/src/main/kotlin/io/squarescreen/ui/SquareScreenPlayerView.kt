package io.squarescreen.ui

import android.util.Log
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.squarescreen.core.annotation.ExperimentalSquareScreenApi
import io.squarescreen.core.model.MediaType
import io.squarescreen.core.model.Playlist
import io.squarescreen.core.model.PlaylistItem
import io.squarescreen.core.model.TransitionType
import io.squarescreen.core.result.SquareScreenResult
import io.squarescreen.ui.internal.MediaItemView
import io.squarescreen.ui.internal.transitionSpecFor
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Renders the active playlist, handling item sequencing, duration timing,
 * transitions, and looping.
 *
 * @param nowPlaying Flow of the current playlist result from [io.squarescreen.player.SquareScreen].
 * @param modifier Modifier applied to the root container.
 * @param onItemStarted Called when a new item begins playing. Receives the item that just started.
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
    onItemStarted: ((item: PlaylistItem) -> Unit)? = null,
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
                    onItemStarted = onItemStarted,
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
    onItemStarted: ((item: PlaylistItem) -> Unit)?,
    onItemCompleted: ((item: PlaylistItem, startedAt: Long, endedAt: Long) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val items = remember(playlist) {
        if (playlist.strategy?.shuffle == true) playlist.items.shuffled()
        else playlist.items
    }

    val loop = playlist.strategy?.loop != false

    // Key tick off item IDs only, not the full playlist object. This way, 60-second
    // playlist refreshes that rotate presigned URLs (same IDs, new query params) do not
    // reset the tick and cut the currently-playing item short.
    val itemIds = items.map { it.id }
    var tick by remember(itemIds) { mutableIntStateOf(0) }
    val currentIndex = tick % items.size
    val currentItem = items[currentIndex]

    // Only replace the item reference when the ID changes — not when presigned URLs rotate.
    // Without this, a URL rotation produces a new PlaylistItem object with the same ID,
    // AnimatedContent sees a changed targetState, tears down VideoItemView, and the
    // new ExoPlayer restarts from frame 0 mid-stream.
    val stableCurrentItem = remember(currentItem.id) { currentItem }

    val effectiveTransition = stableCurrentItem.transition ?: TransitionType.NONE

    // One channel per item slot — VideoItemView signals into it when ExoPlayer fires STATE_ENDED.
    val videoEndedChannel = remember(tick, itemIds) { Channel<Unit>(Channel.CONFLATED) }

    val onItemStartedState = rememberUpdatedState(onItemStarted)
    LaunchedEffect(tick, itemIds) {
        Log.d("PlaylistRenderer", "Item started: index=$currentIndex tick=$tick id=${currentItem.id} duration=${currentItem.duration}s")
        onItemStartedState.value?.invoke(currentItem)
        val startedAt = System.currentTimeMillis()

        if (currentItem.type == MediaType.VIDEO) {
            // Wait for ExoPlayer to signal completion. Fall back after duration + 30s in case
            // the player never fires STATE_ENDED (e.g. unrecoverable network failure).
            withTimeoutOrNull((currentItem.duration + 30) * 1000L) {
                videoEndedChannel.receive()
            }
        } else {
            delay(currentItem.duration * 1000L)
        }

        val endedAt = System.currentTimeMillis()
        Log.d("PlaylistRenderer", "Item completed: index=$currentIndex id=${currentItem.id} — firing onItemCompleted")
        onItemCompleted?.invoke(currentItem, startedAt, endedAt)

        val isLastItem = currentIndex == items.size - 1
        if (!isLastItem || loop) {
            tick++
        }
    }

    AnimatedContent(
        targetState = stableCurrentItem,
        transitionSpec = { transitionSpecFor(effectiveTransition) },
        label = "playlist_item_transition",
        modifier = modifier.fillMaxSize()
    ) { item ->
        MediaItemView(
            item = item,
            onEnded = { videoEndedChannel.trySend(Unit) },
            modifier = Modifier.fillMaxSize()
        )
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
