# SquareScreen Android SDK

Device-side player SDK for DOOH (Digital Out-of-Home) digital signage on Android.

The SDK handles everything below the display layer: device pairing, playlist fetching, media caching, heartbeat reporting, emergency broadcast overrides, and remote device commands — so your app only needs to render what it receives.

---

## Modules

| Artifact | What it does |
|---|---|
| `squarescreen-player` | Core engine — pairing, flows, background workers, foreground service |
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
    implementation("io.squarescreen:squarescreen-player:0.1.2")

    // Jetpack Compose UI components (optional)
    implementation("io.squarescreen:squarescreen-ui:0.1.2")
}
```

Minimum SDK: **API 29 (Android 10)**

---

## Device pairing

Before calling `SquareScreen.init()`, a device must be paired with a SquareScreen workspace. There are two pairing paths — choose whichever fits your setup.

### Path 1 — Register (admin confirms by OS identifier)

1. An admin pre-registers the device in the SquareScreen dashboard using its Android ID.
2. The SDK calls the register endpoint with that ID and polls for admin approval.
3. On approval, the SDK receives a `deviceId` and `deviceToken` — store them securely and pass them to `SquareScreen.init()`.

```kotlin
val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
val pairing = SquareScreenPairing.create(
    context = applicationContext,
    osIdentifier = androidId,
    logger = if (BuildConfig.DEBUG) SquareScreenDebugLogger() else null
)

pairing.pairingStatus.collect { status ->
    when (status) {
        is PairingStatus.Approved -> {
            // Store securely — token is issued exactly once
            credentialStore.save(status.deviceId, status.deviceToken)
            initializeSdk(status.deviceId, status.deviceToken)
        }
        PairingStatus.Pending        -> showAwaitingApprovalUI()
        PairingStatus.DeviceNotFound -> showDeviceNotFoundUI(androidId)
        PairingStatus.AlreadyPaired  -> initializeFromStoredCredentials()
        PairingStatus.Expired        -> showExpiredUI()
        PairingStatus.InvalidToken   -> showInvalidTokenUI()
        is PairingStatus.Error       -> showGenericError(status.throwable)
    }
}
```

### Path 2 — Activate (developer-supplied credentials)

Use this when an admin has already created a device in the SquareScreen dashboard and given you its device ID. You supply that ID alongside your own device token — the token can be anything stable (IMEI, installation UUID, etc.), it is your choice.

1. Admin creates the device in the SquareScreen dashboard — this generates the `deviceId`.
2. The SDK calls the activate endpoint with that `deviceId` and your chosen `deviceToken`, then polls for admin approval exactly like Path 1.
3. On approval, call `SquareScreen.init()` with the same `deviceId` and `deviceToken`.

```kotlin
val pairing = SquareScreenPairing.createWithActivation(
    context = applicationContext,
    deviceId = "AB12CD34",           // created in the SquareScreen admin dashboard
    deviceToken = telephonyManager.imei ?: myInstallationUuid,
    logger = if (BuildConfig.DEBUG) SquareScreenDebugLogger() else null
)

pairing.pairingStatus.collect { status ->
    when (status) {
        is PairingStatus.Approved -> {
            credentialStore.save(status.deviceId, status.deviceToken)
            initializeSdk(status.deviceId, status.deviceToken)
        }
        PairingStatus.Pending        -> showAwaitingApprovalUI()
        PairingStatus.DeviceNotFound -> showDeviceNotFoundUI()
        PairingStatus.AlreadyPaired  -> initializeFromStoredCredentials()
        PairingStatus.Expired        -> showExpiredUI()
        PairingStatus.InvalidToken   -> showInvalidTokenUI()
        is PairingStatus.Error       -> showGenericError(status.throwable)
    }
}
```

### How it works

- On first launch (no stored token): calls `register`, persists the returned pairing token, then begins polling `pair-status`.
- On subsequent launches (stored token): skips `register` and goes straight to polling.
- Polls with exponential backoff: 5 s × 3 attempts → 10 s × 3 → 30 s indefinitely.
- The pairing token is automatically cleared on `Approved`, `Expired`, and `InvalidToken`.
- Call `pairing.cancel()` when the pairing screen is destroyed.

### Manual status check

```kotlin
// One-shot check outside the automatic polling cycle
val status = pairing.checkPairStatus()
```

> **Tip:** `DeviceNotFound` means the admin hasn't pre-registered this device yet.
> Show the Android ID on screen so the admin can copy it into the dashboard.

---

## Quick start

Once the device is paired and credentials are stored, initialize the SDK once in `Application.onCreate()`:

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        SquareScreen.init(
            context = this,
            config = SquareScreenConfig(
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

> **Security:** never hardcode `deviceId` or `deviceToken`. Store them in
> `EncryptedSharedPreferences` after the pairing flow completes.

### Drop in the Compose player

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

> The API base URL is managed internally by the SDK — debug builds target staging,
> release builds target production. Integrators do not set it.

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
        DeviceStatus.ONLINE     -> hideOfflineBadge()
        DeviceStatus.OFFLINE    -> showOfflineBadge()
        DeviceStatus.SYNCING    -> showSpinner()
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

### Heartbeat

The SDK sends device health metrics (CPU usage, memory, disk, temperature) to the server at the configured `heartbeatIntervalSeconds` interval. This runs as a coroutine loop inside the foreground service — not WorkManager — so the configured interval is respected exactly without the 15-minute WorkManager floor.

CPU usage is measured at the process level via `/proc/self/stat` (system-wide `/proc/stat` is restricted on API 26+). Temperature reporting is best-effort and varies by manufacturer; the SDK always sends `null` rather than throw if unavailable.

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

## Caching

The SDK caches both playlist metadata and media files so content continues to play when the device is offline.

### How it works

| Layer | Storage | What's cached |
|---|---|---|
| Metadata | Room database (`squarescreen.db`) | Playlist and playlist item records |
| Media files | `external files dir / squarescreen_media` | Images and video, keyed by SHA-256 hash of URL |

- **TTL:** A cached playlist is considered fresh for `cacheTtlSeconds` (default 3600 s / 1 hour). After expiry the SDK fetches a fresh copy on the next request.
- **Offline fallback:** If the network fetch fails and a cached playlist exists (even expired), the SDK serves it and emits `DeviceStatus.OFFLINE`.
- **Deduplication:** Media files are stored under a SHA-256 hash of the source URL, so the same file is never downloaded twice even if it appears in multiple playlists.
- **Storage permissions:** The media cache directory lives in the app's private external storage (`context.getExternalFilesDir()`). No `READ_EXTERNAL_STORAGE` or `WRITE_EXTERNAL_STORAGE` permission is required on API 29+.

### Custom cache

Implement `CacheProvider` to plug in your own storage backend:

```kotlin
class MyCustomCacheProvider : CacheProvider {
    override suspend fun getPlaylist(): Playlist? { ... }
    override suspend fun savePlaylist(playlist: Playlist) { ... }
    override suspend fun getMediaFile(url: String): File? { ... }
    override suspend fun saveMediaFile(url: String, file: File) { ... }
    override suspend fun clearAll() { ... }
}

SquareScreenConfig(
    ...
    cacheProvider = MyCustomCacheProvider()
)
```

---

## Background work

| Task | Mechanism | Interval | Notes |
|---|---|---|---|
| Heartbeat | Foreground service coroutine loop | `heartbeatIntervalSeconds` (default 60 s) | Exact interval — not subject to WorkManager's 15-min floor |
| Emergency poll | WorkManager `PeriodicWorkRequest` | `emergencyPollIntervalSeconds` (default 30 s) | Survives process death |
| Command poll | WorkManager `PeriodicWorkRequest` | 30 s | Survives process death |

---

## Using the player without Compose UI

The `squarescreen-player` module has no UI dependency. Use it headlessly — collect `nowPlaying` and render however you like:

```kotlin
// Add only the player, skip the UI module
implementation("io.squarescreen:squarescreen-player:0.1.2")
```

```kotlin
squareScreen.nowPlaying.collect { result ->
    val playlist = result.getOrNull() ?: return@collect
    myCustomRenderer.show(playlist.items)
}
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
