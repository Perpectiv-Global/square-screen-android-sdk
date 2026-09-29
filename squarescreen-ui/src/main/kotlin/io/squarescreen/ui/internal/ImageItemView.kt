package io.squarescreen.ui.internal

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
    // Strip query params (presigned signatures) so the cache key is stable across
    // playlist refreshes that return a new signed URL for the same underlying file.
    val stableCacheKey = remember(url) { url.substringBefore('?') }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(url)
                .memoryCacheKey(stableCacheKey)
                .diskCacheKey(stableCacheKey)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
            onSuccess = {
                Log.d("ImageItemView", "Loaded: $stableCacheKey")
            },
            onError = { state ->
                Log.w("ImageItemView", "Failed to load: $stableCacheKey — ${state.result.throwable.message}")
                // Evict corrupt/partial entries so the next attempt re-downloads from network
                context.imageLoader.diskCache?.remove(stableCacheKey)
                context.imageLoader.memoryCache?.remove(MemoryCache.Key(stableCacheKey))
            }
        )
    }
}
