package io.squarescreen.player

import android.content.Context
import android.content.Intent
import io.squarescreen.cache.CacheProviderFactory
import io.squarescreen.core.config.SquareScreenConfig
import io.squarescreen.core.exception.SquareScreenNotInitializedException
import io.squarescreen.core.model.DeviceStatus
import io.squarescreen.core.model.EmergencyAlert
import io.squarescreen.core.model.Playlist
import io.squarescreen.core.result.SquareScreenError
import io.squarescreen.core.result.SquareScreenResult
import io.squarescreen.network.NetworkClientFactory
import io.squarescreen.player.internal.PlayerRepository
import io.squarescreen.player.internal.SquareScreenServiceLocator
import io.squarescreen.player.service.SquareScreenPlayerService
import io.squarescreen.player.worker.WorkScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

private const val TAG = "SquareScreen"

/**
 * Main entry point for the SquareScreen SDK.
 *
 * Initialize once in `Application.onCreate()`:
 * ```kotlin
 * SquareScreen.init(context = applicationContext, config = SquareScreenConfig(...))
 * val squareScreen = SquareScreen.getInstance()
 * ```
 */
class SquareScreen private constructor(
    private val appContext: Context,
    private val config: SquareScreenConfig,
    private val repository: PlayerRepository,
    private val workScheduler: WorkScheduler
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Currently active playlist. Emits on every schedule change or manual refresh. */
    val nowPlaying: Flow<SquareScreenResult<Playlist>> =
        SquareScreenServiceLocator.nowPlayingState.filterNotNull()

    /**
     * Active emergency alert, or null when none is broadcast.
     * Emits null immediately on collection (no active alert at startup).
     */
    val emergencyAlert: Flow<EmergencyAlert?> =
        SquareScreenServiceLocator.emergencyAlertState

    /** Device connectivity and sync state. */
    val deviceStatus: Flow<DeviceStatus> =
        SquareScreenServiceLocator.deviceStatusState

    // -------------------------------------------------------------------------
    // Manual controls
    // -------------------------------------------------------------------------

    /**
     * Forces a playlist refresh, bypassing the cache TTL.
     * The [nowPlaying] flow will emit the new result automatically.
     */
    suspend fun refresh(): SquareScreenResult<Playlist> {
        SquareScreenServiceLocator.log(TAG, "Manual refresh requested")
        val result = repository.fetchPlaylist()
        SquareScreenServiceLocator.nowPlayingState.value = result
        return result
    }

    /**
     * Checks emergency status immediately, outside the normal polling cycle.
     * Updates [emergencyAlert] if the status has changed.
     */
    suspend fun checkEmergency(): EmergencyAlert? {
        val result = repository.fetchEmergencyAlert()
        if (result is SquareScreenResult.Success) {
            SquareScreenServiceLocator.emergencyAlertState.value = result.data
            return result.data
        }
        return SquareScreenServiceLocator.emergencyAlertState.value
    }

    /**
     * Stops the foreground service and cancels all WorkManager tasks.
     * After calling this, [getInstance] will still return this instance
     * but background work will not run.
     */
    fun shutdown() {
        SquareScreenServiceLocator.log(TAG, "Shutdown requested")
        workScheduler.cancelAll()
        appContext.stopService(Intent(appContext, SquareScreenPlayerService::class.java))
    }

    // -------------------------------------------------------------------------
    // Callback wrappers (Java interop / non-coroutine usage)
    // -------------------------------------------------------------------------

    /**
     * Subscribes to [nowPlaying] using callbacks instead of Flow collection.
     * Returns a [Job] that can be cancelled to stop receiving updates.
     *
     * @param scope The coroutine scope in which callbacks are delivered.
     */
    fun nowPlayingCallback(
        scope: CoroutineScope,
        onSuccess: (Playlist) -> Unit,
        onError: (SquareScreenError) -> Unit
    ): Job = nowPlaying.onEach { result ->
        when (result) {
            is SquareScreenResult.Success -> onSuccess(result.data)
            is SquareScreenResult.Error -> onError(result.error)
        }
    }.launchIn(scope)

    /**
     * Subscribes to [emergencyAlert] using callbacks instead of Flow collection.
     * Returns a [Job] that can be cancelled to stop receiving updates.
     *
     * @param scope The coroutine scope in which callbacks are delivered.
     */
    fun emergencyAlertCallback(
        scope: CoroutineScope,
        onAlert: (EmergencyAlert) -> Unit,
        onCleared: () -> Unit
    ): Job = emergencyAlert.onEach { alert ->
        if (alert != null) onAlert(alert) else onCleared()
    }.launchIn(scope)

    // -------------------------------------------------------------------------
    // Internal startup
    // -------------------------------------------------------------------------

    internal fun start() {
        // Trigger initial playlist fetch
        scope.launch {
            SquareScreenServiceLocator.log(TAG, "Starting initial playlist fetch")
            val result = repository.fetchPlaylist()
            SquareScreenServiceLocator.nowPlayingState.value = result
        }

        // Start WorkManager tasks
        workScheduler.scheduleHeartbeat(config.heartbeatIntervalSeconds)
        workScheduler.scheduleEmergencyPoll(config.emergencyPollIntervalSeconds)

        // Start foreground service
        val serviceIntent = Intent(appContext, SquareScreenPlayerService::class.java)
        appContext.startForegroundService(serviceIntent)

        SquareScreenServiceLocator.log(TAG, "SquareScreen SDK started (v${io.squarescreen.player.internal.SDK_VERSION})")
    }

    // -------------------------------------------------------------------------
    // Companion — singleton management
    // -------------------------------------------------------------------------

    companion object {
        @Volatile private var instance: SquareScreen? = null

        /**
         * Initializes the SDK. Must be called once in `Application.onCreate()`.
         *
         * Calling this more than once is a no-op after the first successful call.
         * A warning is logged if called again.
         */
        fun init(context: Context, config: SquareScreenConfig) {
            if (instance != null) {
                config.logger?.warn(TAG, "SquareScreen.init() called more than once — ignoring")
                return
            }
            synchronized(this) {
                if (instance != null) return

                val appContext = context.applicationContext
                val networkDataSource = NetworkClientFactory.create(config)
                val cacheProvider = CacheProviderFactory.create(appContext, config)

                SquareScreenServiceLocator.config = config
                SquareScreenServiceLocator.networkDataSource = networkDataSource
                SquareScreenServiceLocator.cacheProvider = cacheProvider

                val repository = PlayerRepository(networkDataSource, cacheProvider)
                val workScheduler = WorkScheduler(appContext)

                val sdk = SquareScreen(appContext, config, repository, workScheduler)
                instance = sdk
                sdk.start()
            }
        }

        /**
         * Returns the initialized [SquareScreen] instance.
         *
         * @throws SquareScreenNotInitializedException if [init] has not been called.
         */
        fun getInstance(): SquareScreen {
            return instance
                ?: throw SquareScreenNotInitializedException()
        }

        /** For testing only — resets singleton state. */
        internal fun reset() {
            instance = null
            SquareScreenServiceLocator.clear()
        }
    }
}
