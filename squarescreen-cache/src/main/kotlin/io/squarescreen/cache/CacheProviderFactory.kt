package io.squarescreen.cache

import android.content.Context
import io.squarescreen.core.cache.CacheProvider
import io.squarescreen.core.config.SquareScreenConfig

/**
 * Internal factory — resolves the active [CacheProvider].
 * Uses the integrator-supplied provider from config if present,
 * otherwise falls back to [DefaultCacheProvider].
 */
internal object CacheProviderFactory {

    fun create(context: Context, config: SquareScreenConfig): CacheProvider {
        return config.cacheProvider ?: DefaultCacheProvider(
            context = context,
            ttlSeconds = config.cacheTtlSeconds
        )
    }
}
