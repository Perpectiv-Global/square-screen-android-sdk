# SquareScreen Android SDK

Device-side player SDK for DOOH (Digital Out-of-Home) digital signage on Android.

The SDK handles everything below the display layer: playlist fetching, media caching, heartbeat reporting, emergency broadcast overrides, and remote device commands — so your app only needs to render what it receives.

---

## Modules

| Artifact | What it does |
|---|---|
| `squarescreen-player` | Core engine — flows, background workers, foreground service |
| `squarescreen-ui` | Jetpack Compose player and emergency overlay *(optional)* |
| `squarescreen-core` | Shared models and contracts *(transitive)* |
| `squarescreen-network` | Retrofit network layer *(transitive)* |
| `squarescreen-cache` | Room metadata cache + disk media cache *(transitive)* |

---

## Installation

Add to your module's `build.gradle.kts`:

```kotlin
dependencies {
    // Core player (required)
    implementation("io.squarescreen:squarescreen-player:0.1.0")

    // Jetpack Compose UI components (optional)
    implementation("io.squarescreen:squarescreen-ui:0.1.0")
}
```

Minimum SDK: **API 29 (Android 10)**

---

## Quick start

### 1. Initialize in `Application.onCreate()`

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        SquareScreen.init(
            context = this,
            config = SquareScreenConfig(
                baseUrl = "https://api.squarescreen.io",
                deviceId = credentialStore.deviceId,
                deviceToken = credentialStore.deviceToken,
                foregroundNotification = ForegroundNotificationConfig(
                    title = "Display active",
                    iconResId = R.drawable.ic_player
                ),
                logger = if (BuildConfig.DEBUG) SquareScreenDebugLogger() else null
            )
        )
    }
}
```

> **Security:** never hardcode `deviceToken`. Store it in `EncryptedSharedPreferences`
> and retrieve it after the device pairing flow.

### 2. Drop in the Compose player

```kotlin
@Composable
fun PlayerScreen() {
    val squareScreen = SquareScreen.getInstance()

    SquareScreenDisplay(
        squareScreen = squareScreen,
        modifier = Modifier.fillMaxSize()
    )
}
```

`SquareScreenDisplay` renders the active playlist, overlays emergency alerts, and automatically reports proof-of-play after each item.

---

## Configuration

```kotlin
SquareScreenConfig(
    baseUrl = "https://api.squarescreen.io",   // required
    deviceId = "...",                           // required — X-Device-Id header
    deviceToken = "...",                        // required — X-Device-Token header
    heartbeatIntervalSeconds = 60L,            // min 30, default 60
    emergencyPollIntervalSeconds = 30L,        // min 15, default 30
    cacheTtlSeconds = 3600L,                   // default 1 hour
    foregroundNotification = ForegroundNotificationConfig(
        title = "Display active",
        iconResId = R.drawable.ic_player
    ),
    logger = SquareScreenDebugLogger()         // null = silent (recommended for production)
)
```

---

## Core concepts

### Flows

The SDK exposes four Kotlin `Flow`s on the `SquareScreen` instance:

```kotlin
val squareScreen = SquareScreen.getInstance()

// Active playlist — emits on every schedule change or manual refresh
squareScreen.nowPlaying.collect { result ->
    when (result) {
        is SquareScreenResult.Success -> render(result.data)
        is SquareScreenResult.Error   -> showError(result.error)
    }
}

// Emergency alert — emits null when no broadcast is active
squareScreen.emergencyAlert.collect { alert ->
    if (alert != null) showEmergencyOverlay(alert) else hideOverlay()
}

// Device connectivity and sync state
squareScreen.deviceStatus.collect { status ->
    when (status) {
        DeviceStatus.ONLINE    -> hideOfflineBadge()
        DeviceStatus.OFFLINE   -> showOfflineBadge()
        DeviceStatus.SYNCING   -> showSpinner()
        DeviceStatus.CONNECTING -> Unit
    }
}

// Remote commands issued by the management console
squareScreen.commands.collect { commands ->
    commands.forEach { command ->
        when (command.toCommandType()) {
            is CommandType.SetVolume -> setVolume((command.toCommandType() as CommandType.SetVolume).volume)
            is CommandType.Unknown   -> { /* ignore unknown commands */ }
        }
        squareScreen.acknowledgeCommand(command.id, status = "completed")
    }
}
```

### Java / callback interop

For code that can't use `collect`, all flows have callback wrappers:

```kotlin
val job = squareScreen.nowPlayingCallback(
    scope = lifecycleScope,
    onSuccess = { playlist -> render(playlist) },
    onError = { error -> showError(error) }
)

// Cancel when done
job.cancel()
```

### Emergency alerts

Emergency broadcasts automatically override the playlist UI when using `SquareScreenDisplay`. The overlay renders the alert's `title`, `message`, `backgroundColor`, and `textColor` as provided by the server.

If you are **not** using `squarescreen-ui`, subscribe to `squareScreen.emergencyAlert` and implement your own overlay.

### Proof-of-play

`SquareScreenDisplay` reports playback automatically. If you use `SquareScreenPlayerView` directly, wire the `onItemCompleted` callback:

```kotlin
SquareScreenPlayerView(
    nowPlaying = squareScreen.nowPlaying,
    onItemCompleted = { item, startedAt, endedAt ->
        scope.launch {
            squareScreen.reportPlayback(
                PlaybackReport(
                    mediaUuid = item.id,
                    playlistUuid = currentPlaylist?.playlist?.uuid,
                    scheduleUuid = currentPlaylist?.schedule?.uuid,
                    startedAt = formatIso8601(startedAt),
                    endedAt = formatIso8601(endedAt),
                    durationSeconds = item.duration,
                    completed = true
                )
            )
        }
    }
)
```

---

## Using the player without Compose UI

The `squarescreen-player` module has no UI dependency. Use it headlessly — collect `nowPlaying` and render however you like:

```kotlin
// Add only the player, skip the UI module
implementation("io.squarescreen:squarescreen-player:0.1.0")
```

```kotlin
squareScreen.nowPlaying.collect { result ->
    val playlist = result.getOrNull() ?: return@collect
    myCustomRenderer.show(playlist.items)
}
```

---

## Custom cache or network

Provide your own implementation of `CacheProvider` or `NetworkDataSource` in `SquareScreenConfig`:

```kotlin
SquareScreenConfig(
    ...
    cacheProvider = MyCustomCacheProvider()
)
```

---

## Manual controls

```kotlin
// Force a playlist refresh (bypasses TTL)
val result = squareScreen.refresh()

// Force an emergency status check
val alert = squareScreen.checkEmergency()

// Stop the foreground service and cancel all background workers
squareScreen.shutdown()
```

---

## Logging

The SDK never calls `android.util.Log` directly. All internal logs route through `SquareScreenLogger`.

```kotlin
// Development — writes to Logcat
logger = SquareScreenDebugLogger()

// Production — silent (recommended)
logger = null

// Custom — e.g. forward to Crashlytics
logger = object : SquareScreenLogger {
    override fun debug(tag: String, message: String) {}
    override fun info(tag: String, message: String) {}
    override fun warn(tag: String, message: String) {}
    override fun error(tag: String, message: String, throwable: Throwable?) {
        FirebaseCrashlytics.getInstance().recordException(throwable ?: Exception(message))
    }
}
```

---

## Requirements

- Android **API 29+**
- Kotlin **2.0+**
- Jetpack Compose (only if using `squarescreen-ui`)

---

## License

```
Copyright 2024 SquareScreen

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
