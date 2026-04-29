package io.squarescreen.player.internal

import io.squarescreen.core.config.SquareScreenConfig
import io.squarescreen.core.datasource.NetworkDataSource
import io.squarescreen.core.cache.CacheProvider
import io.squarescreen.core.logging.SquareScreenLogger
import io.squarescreen.core.model.DeviceStatus
import io.squarescreen.core.model.EmergencyAlert
import io.squarescreen.core.model.Playlist
import io.squarescreen.core.result.SquareScreenResult
import kotlinx.coroutines.flow.MutableStateFlow

internal object SquareScreenServiceLocator {

    @Volatile var config: SquareScreenConfig? = null
    @Volatile var networkDataSource: NetworkDataSource? = null
    @Volatile var cacheProvider: CacheProvider? = null

    val nowPlayingState = MutableStateFlow<SquareScreenResult<Playlist>?>(null)
    val emergencyAlertState = MutableStateFlow<EmergencyAlert?>(null)
    val deviceStatusState = MutableStateFlow(DeviceStatus.CONNECTING)

    fun log(tag: String, message: String) {
        config?.logger?.debug(tag, message)
    }

    fun logError(tag: String, message: String, throwable: Throwable? = null) {
        config?.logger?.error(tag, message, throwable)
    }

    fun clear() {
        config = null
        networkDataSource = null
        cacheProvider = null
        nowPlayingState.value = null
        emergencyAlertState.value = null
        deviceStatusState.value = DeviceStatus.CONNECTING
    }
}
