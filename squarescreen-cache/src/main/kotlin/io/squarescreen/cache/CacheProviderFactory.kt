package io.squarescreen.cache

import android.content.Context
import io.squarescreen.core.cache.CacheProvider
import io.squarescreen.core.config.SquareScreenConfig

/**
 * Resolves the active [CacheProvider].
 * Uses the integrator-supplied provider from config if present,
 * otherwise falls back to [DefaultCacheProvider].
 * Not intended for direct use by integrators — accessed by squarescreen-player only.
 */
object CacheProviderFactory {

    fun create(context: Context, config: SquareScreenConfig): CacheProvider {
        return config.cacheProvider ?: DefaultCacheProvider(
            context = context,
            ttlSeconds = config.cacheTtlSeconds
        )
    }
}
