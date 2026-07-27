package io.squarescreen.ui.internal

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.imageLoader
import coil.memory.MemoryCache
import coil.request.ImageRequest

@Composable
internal fun ImageItemView(
    url: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(url)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            onError = { state ->
                Log.w("ImageItemView", "Failed to load image: $url — ${state.result.throwable.message}")
                // Evict corrupt/partial cache entries so the next loop re-downloads from network
                context.imageLoader.diskCache?.remove(url)
                context.imageLoader.memoryCache?.remove(MemoryCache.Key(url))
            }
        )
    }
}
