# SquareScreen Android SDK — Architecture & Specification

**Version:** 0.1.0-draft  
**Date:** April 2026  
**Owner:** Perspective Global  
**Status:** Pre-development spec — hand this document to Claude Code as the authoritative brief

---

## 1. Purpose & Context

SquareScreen is an enterprise digital signage and content management platform. This SDK turns an Android device into a SquareScreen display player. It is consumed by:

- Third-party developers building custom player applications on the SquareScreen platform
- Perspective Global's own internal team building first-party apps

The SDK is **not** a management/control SDK. It is a **device-side player SDK**. Its job is to authenticate a device, fetch what should be playing, keep the server informed of device health, respond to emergency broadcasts, and cache content for offline continuity.

---

## 2. Non-Negotiable Principles

These apply to every line of code in the SDK:

1. **Never impose a DI framework.** No Hilt, no Dagger, no Koin in the public API or as a required dependency. Use an internal factory/object graph.
2. **Never spam Logcat in production.** All logging is opt-in via a pluggable interface.
3. **Never break integrators silently.** Binary compatibility is enforced via the Kotlin Binary Compatibility Validator on every release.
4. **Never force UI decisions on integrators who don't want them.** The UI module is optional. Core is headless.
5. **Ship `consumer-proguard-rules.pro`.** Integrators should never have to write R8/ProGuard rules for this SDK.

---

## 3. Target Platform

| Property | Value |
|---|---|
| Min SDK | API 29 (Android 10) |
| Target SDK | Latest stable |
| Language | Kotlin (100%) |
| Build system | Gradle with Kotlin DSL (`.kts`) |
| UI toolkit (ui module only) | Jetpack Compose |

**Note on temperature metric:** Reading device temperature on Android is inconsistent across manufacturers. Some expose it via `BatteryManager`, others via thermal HAL files, and some not at all. Treat `temperature` in the heartbeat payload as best-effort. Document this explicitly. Never throw if it cannot be read — send `null`.

---

## 4. Module Structure

```
squarescreen-android-sdk/
├── squarescreen-core/          ← Public API contracts, models, sealed types, interfaces
├── squarescreen-network/       ← Retrofit + OkHttp, auth interceptor, API client
├── squarescreen-cache/         ← CacheProvider abstraction + default Room/disk implementation
├── squarescreen-player/        ← Playlist engine, scheduling, heartbeat, emergency polling
├── squarescreen-ui/            ← Compose playlist renderer + emergency overlay (OPTIONAL)
└── sample-app/                 ← Full reference integration
```

### 4.1 Module Dependency Graph

```
squarescreen-ui
    └── squarescreen-player
            ├── squarescreen-network
            ├── squarescreen-cache
            └── squarescreen-core

squarescreen-network
    └── squarescreen-core

squarescreen-cache
    └── squarescreen-core

sample-app
    └── squarescreen-ui (or squarescreen-player directly for headless usage)
```

**Rule:** Integrators who only want data (no UI) depend on `squarescreen-player`. Integrators who want the full rendered player depend on `squarescreen-ui`. Nobody ever needs to depend on `squarescreen-network` or `squarescreen-cache` directly — those are implementation details.

---

## 5. Module Responsibilities

### 5.1 `squarescreen-core`

- All public data models (`PlaylistItem`, `EmergencyAlert`, `DeviceStatus`, `HeartbeatPayload`)
- All public sealed result and error types (`SquareScreenResult`, `SquareScreenError`)
- Public interfaces that other modules implement (`NetworkDataSource`, `CacheProvider`, `SquareScreenLogger`)
- `SquareScreenConfig` — the configuration object developers pass at init
- `@ExperimentalSquareScreenApi` annotation definition
- No Android framework dependencies beyond `Context` where strictly necessary. Prefer pure Kotlin.

### 5.2 `squarescreen-network`

- Retrofit + OkHttp setup, internal only
- `DeviceAuthInterceptor` — injects `X-Device-Id` and `X-Device-Token` headers on every request automatically. Developers never touch auth headers.
- Implements `NetworkDataSource` from core
- JSON parsing via kotlinx.serialization or Gson (pick one — see Section 8)
- Handles HTTP error mapping to `SquareScreenError` sealed types
- Not exposed in the public API surface

### 5.3 `squarescreen-cache`

- Defines `CacheProvider` interface in core; ships the default implementation here
- **Metadata cache:** Room database — stores playlist metadata, schedule rules, last-known state
- **Media cache:** Disk/file cache — downloaded media files stored in the app's private external directory (`context.getExternalFilesDir()`). Never touches public storage.
- Cache is read-first: if network fails, serve from cache. Log the fallback.
- Cache invalidation: TTL-based per item, with force-refresh available
- Not exposed in the public API surface

### 5.4 `squarescreen-player`

- The main integration point for headless usage
- Owns the `SquareScreen` singleton — entry point for all developers
- Playlist engine: processes `PlaylistItem` list, manages sequencing and timing
- Scheduler: respects `live`/`expire` date-times from the playlist response
- **Heartbeat:** WorkManager `PeriodicWorkRequest`, fires every 60 seconds, collects device metrics, posts to `/api/v1/screen/heartbeat`. Survives process death.
- **Emergency polling:** polls `/api/v1/screen/emergency` every 30 seconds. When active, emits to the `emergencyAlert` Flow. When cleared, emits `null`. Design the transport so it can be swapped to WebSocket/FCM later without changing the public API.
- **Foreground Service:** manages active playback continuity. The SDK starts/stops this service internally. Integrators provide the notification details (title, icon) via config.
- Exposes all state as Kotlin `Flow`s

### 5.5 `squarescreen-ui`

- **Jetpack Compose only.** No XML layouts.
- `SquareScreenPlayerView` — a Composable that accepts the playlist flow and renders each item. Handles transitions.
- `EmergencyOverlayView` — a full-screen Composable that renders the emergency alert (background_color, text_color, message). Overlays everything. Dismisses automatically when the emergency flow emits null.
- Media rendering: Glide (via `accompanist` or `coil` — see Section 8 decision) for images, Media3/ExoPlayer for video
- This module is **optional**. Integrators who want their own rendering use only `squarescreen-player` and consume the flows themselves.

### 5.6 `sample-app`

- A standalone Android application (not a library module)
- Demonstrates the full integration flow:
  1. SDK initialization with config
  2. Playlist fetch and display
  3. Heartbeat running in background
  4. Emergency alert override rendering
- Uses `squarescreen-ui` (not headless) so it validates the full stack
- Should be buildable and runnable against a real or mock backend
- Written to be read as documentation — clear comments, no clever shortcuts

---

## 6. Authentication

The SquareScreen API uses **device-level auth**, not user Bearer tokens.

Every request must include:
```
X-Device-Id: <device UUID>
X-Device-Token: <device token>
```

These are set once at `SquareScreen.init()` via `SquareScreenConfig` and injected automatically by `DeviceAuthInterceptor` in `squarescreen-network`. Developers never set headers manually.

**Security note for documentation:** The `deviceToken` lives on the device. Integrators must not hardcode it in source code. Recommend storing it in `EncryptedSharedPreferences` and fetching it from a secure backend at first launch.

---

## 7. Public API Surface

### 7.1 Initialization

```kotlin
// Required: call once in Application.onCreate()
SquareScreen.init(
    context = applicationContext,
    config = SquareScreenConfig(
        baseUrl = "https://api.squarescreen.io",
        deviceId = "device-uuid",
        deviceToken = "device-token",
        heartbeatIntervalSeconds = 60,          // default 60, min 30
        emergencyPollIntervalSeconds = 30,       // default 30, min 15
        foregroundNotification = ForegroundNotificationConfig(
            title = "SquareScreen Player",
            iconResId = R.drawable.ic_player
        ),
        logger = null                            // null = silent (default)
    )
)

// Get the instance after init
val squareScreen = SquareScreen.getInstance()
```

**Init rules:**
- Calling `init()` more than once is a no-op after the first successful call. Log a warning if called again.
- `getInstance()` before `init()` throws `SquareScreenNotInitializedException` (a clear, named exception — not a generic IllegalStateException).

### 7.2 Core Flows

```kotlin
val squareScreen = SquareScreen.getInstance()

// Currently active playlist — updates when schedule changes
squareScreen.nowPlaying: Flow<SquareScreenResult<Playlist>>

// Emergency alert state — null when no active alert, EmergencyAlert when active
squareScreen.emergencyAlert: Flow<EmergencyAlert?>

// Device connectivity and sync state
squareScreen.deviceStatus: Flow<DeviceStatus>
```

### 7.3 Manual Controls

```kotlin
// Force a playlist refresh (ignores cache TTL)
suspend fun squareScreen.refresh(): SquareScreenResult<Playlist>

// Check emergency status immediately (outside the polling cycle)
suspend fun squareScreen.checkEmergency(): EmergencyAlert?

// Shutdown — stops foreground service, cancels WorkManager tasks
fun squareScreen.shutdown()
```

### 7.4 Callback Compatibility Wrapper

For Java interop or developers who prefer callbacks over coroutines:

```kotlin
squareScreen.nowPlayingCallback(
    onSuccess = { playlist -> },
    onError = { error -> }
)

squareScreen.emergencyAlertCallback(
    onAlert = { alert -> },
    onCleared = { }
)
```

Callbacks are implemented as thin wrappers over the Flow API using `lifecycleScope` or a provided `CoroutineScope`. They do not replace the Flow API — they wrap it.

---

## 8. Data Models

### 8.1 Core Models (in `squarescreen-core`)

```kotlin
data class Playlist(
    val items: List<PlaylistItem>,
    val strategy: PlaybackStrategy?,
    val cachedAt: Long                     // epoch ms — for cache age display
)

data class PlaylistItem(
    val id: Int,
    val type: MediaType,                   // IMAGE or VIDEO
    val url: String,
    val duration: Int,                     // seconds
    val transition: TransitionType?
)

enum class MediaType { IMAGE, VIDEO }

enum class TransitionType { FADE, SLIDE, NONE }

data class EmergencyAlert(
    val id: String,
    val title: String,
    val message: String,
    val backgroundColor: String,           // hex e.g. "#FF0000"
    val textColor: String                  // hex e.g. "#FFFFFF"
)

enum class DeviceStatus { CONNECTING, ONLINE, OFFLINE, SYNCING }

data class HeartbeatPayload(
    val cpuUsage: Float?,
    val memoryUsage: Float?,
    val diskUsage: Float?,
    val temperature: Float?,               // best-effort, may be null
    val osVersion: String,
    val playerVersion: String              // SDK version string
)
```

### 8.2 PlaybackStrategy

```kotlin
// Returned from /screen/now-playing alongside items
// Exact shape TBD as more APIs arrive — mark as experimental
@ExperimentalSquareScreenApi
data class PlaybackStrategy(
    val raw: Map<String, Any>              // placeholder until backend defines this fully
)
```

---

## 9. Error Handling Contract

All SDK errors flow through two sealed types:

```kotlin
sealed class SquareScreenResult<out T> {
    data class Success<T>(val data: T) : SquareScreenResult<T>()
    data class Error(val error: SquareScreenError) : SquareScreenResult<Nothing>()
}

sealed class SquareScreenError {
    data class NetworkError(
        val code: Int,
        val message: String
    ) : SquareScreenError()

    data class AuthError(
        val message: String                // device token invalid/expired
    ) : SquareScreenError()

    data class CacheError(
        val message: String                // cache read/write failure
    ) : SquareScreenError()

    data class ParseError(
        val message: String                // response could not be parsed
    ) : SquareScreenError()

    object EmergencyOverrideActive : SquareScreenError()

    data class Unknown(
        val throwable: Throwable
    ) : SquareScreenError()
}
```

**Rules:**
- The SDK **never throws** unchecked exceptions into integrator code except `SquareScreenNotInitializedException` (pre-init guard only)
- `Flow`s never terminate on error — they emit `SquareScreenResult.Error` and continue
- `suspend` functions return `SquareScreenResult` — never throw
- Cache fallback is transparent to the result type: a successful cache hit is still `SquareScreenResult.Success`

---

## 10. Logging

```kotlin
interface SquareScreenLogger {
    fun debug(tag: String, message: String)
    fun info(tag: String, message: String)
    fun warn(tag: String, message: String)
    fun error(tag: String, message: String, throwable: Throwable? = null)
}
```

- Default: `null` logger = completely silent in all build types
- Integrators provide an implementation at init time
- A built-in `SquareScreenDebugLogger` is shipped that logs to Android `Log.*` — for use in development only, never in production
- The SDK never directly calls `android.util.Log` anywhere. All logging goes through the internal logger reference.

```kotlin
// Example: using the built-in debug logger during development
SquareScreen.init(
    context = applicationContext,
    config = SquareScreenConfig(
        ...
        logger = if (BuildConfig.DEBUG) SquareScreenDebugLogger() else null
    )
)
```

---

## 11. Caching Architecture

### 11.1 CacheProvider Interface (in `squarescreen-core`)

```kotlin
interface CacheProvider {
    suspend fun getPlaylist(): Playlist?
    suspend fun savePlaylist(playlist: Playlist)
    suspend fun getMediaFile(url: String): File?
    suspend fun saveMediaFile(url: String, file: File)
    suspend fun clearAll()
}
```

### 11.2 Default Implementation (in `squarescreen-cache`)

- **Metadata:** Room database with a `PlaylistEntity` and `PlaylistItemEntity`
- **Media files:** Stored in `context.getExternalFilesDir("squarescreen_media")`. File names are a hash of the URL for deduplication.
- **TTL:** Configurable via `SquareScreenConfig.cacheTtlSeconds` (default: 3600)
- **Pre-fetching:** When a playlist is received, the player module triggers background download of all media URLs before they're needed. Uses WorkManager `OneTimeWorkRequest` chained per item.
- **Fallback behaviour:** On network failure, the player falls back to cached playlist automatically. `DeviceStatus` emits `OFFLINE` to signal this to the integrator.

### 11.3 Custom Cache

Integrators who want their own caching (e.g. already have a media download manager) can implement `CacheProvider` and pass it via config:

```kotlin
SquareScreenConfig(
    ...
    cacheProvider = MyCustomCacheProvider()
)
```

---

## 12. Background Work

### 12.1 Heartbeat (WorkManager)

- `PeriodicWorkRequest` — interval defined by `heartbeatIntervalSeconds` in config (default 60s)
- Collects: cpu_usage, memory_usage, disk_usage, temperature (best-effort), os_version, player_version
- Posts to `POST /api/v1/screen/heartbeat`
- WorkManager tag: `"squarescreen_heartbeat"` — used for cancellation on `shutdown()`
- Requires `INTERNET` permission only — no special system permissions needed

**CPU/Memory collection approach:** Use `android.os.Debug` for memory and `/proc/stat` parsing for CPU. Both available at API 29. Document that values are approximate.

### 12.2 Emergency Polling (WorkManager)

- `PeriodicWorkRequest` — interval defined by `emergencyPollIntervalSeconds` in config (default 30s)
- GETs `/api/v1/screen/emergency`
- When `active: true` → emits `EmergencyAlert` to the `emergencyAlert` Flow
- When `active: false` → emits `null`
- **Architecture note:** The emergency transport is abstracted behind an internal interface. The polling WorkManager is the first implementation. This must be swappable for WebSocket or FCM push without changing the `emergencyAlert` Flow signature. Flag this to the backend team — 30s polling means up to 30s delay on a fire alarm broadcast.
- WorkManager tag: `"squarescreen_emergency_poll"`

### 12.3 Foreground Service (Active Playback)

- Started when `SquareScreen.init()` is called and a playlist is active
- Keeps the player process alive during continuous display operation
- Integrators provide notification config (title, icon resource ID) — the SDK creates and manages the notification
- `FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK` declared in the SDK's manifest (merged automatically)
- Stopped cleanly on `squareScreen.shutdown()`

---

## 13. Known APIs (as of April 2026)

More APIs will be added. All new endpoints slot into the existing module structure. Update `NetworkDataSource` and its implementation in `squarescreen-network` for each new endpoint.

### `GET /api/v1/screen/now-playing`

Returns the active playlist for the authenticated device.

**Headers:** `X-Device-Id`, `X-Device-Token`  
**Query params:** `type` (image/video), `category`, `quality` (default 1080p), `limit` (default 20)  
**Response:**
```json
{
  "items": [
    {
      "id": 1,
      "type": "image",
      "url": "https://cdn.example.com/media/banner.jpg",
      "duration": 10,
      "transition": "fade"
    }
  ],
  "strategy": {}
}
```
Returns `{ "items": [], "strategy": {} }` when nothing is scheduled. Treat this as valid — show a default/fallback state, not an error.

---

### `POST /api/v1/screen/heartbeat`

Reports device health. All fields optional. Updates device `last_heartbeat_at` on the server.

**Headers:** `X-Device-Id`, `X-Device-Token`  
**Body:**
```json
{
  "cpu_usage": 42.5,
  "memory_usage": 61.0,
  "disk_usage": 28.3,
  "temperature": 52.0,
  "os_version": "Android 13",
  "player_version": "1.2.0"
}
```
**Response:** `{ "success": true }`

---

### `GET /api/v1/screen/emergency`

Check for active emergency broadcasts targeting this device (by company, workspace, group, or device).

**Headers:** `X-Device-Id`, `X-Device-Token`  
**Response (active):**
```json
{
  "active": true,
  "broadcast": {
    "id": "uuid",
    "title": "FIRE ALARM",
    "message": "Evacuate immediately.",
    "background_color": "#FF0000",
    "text_color": "#FFFFFF"
  }
}
```
**Response (none):**
```json
{
  "active": false,
  "broadcast": null
}
```

---

## 14. `@ExperimentalSquareScreenApi` Annotation

Mark any API surface that is subject to change without a major version bump:

```kotlin
@RequiresOptIn(
    message = "This API is experimental and may change without notice in future versions.",
    level = RequiresOptIn.Level.WARNING
)
@Retention(AnnotationRetention.BINARY)
annotation class ExperimentalSquareScreenApi
```

Integrators opt in at call site with `@OptIn(ExperimentalSquareScreenApi::class)` or at file/class level. This is how you safely ship APIs for endpoints that are still being finalized, like `PlaybackStrategy`.

---

## 15. Versioning

- **Scheme:** Semantic Versioning — `MAJOR.MINOR.PATCH`
- `MAJOR` — breaking public API change
- `MINOR` — new functionality, backwards compatible
- `PATCH` — bug fixes, backwards compatible
- **Initial version:** `0.1.0` — `0.x.x` signals pre-stable; breaking changes may occur on minor bumps (document this clearly)
- **Stable release:** `1.0.0` — only after all known MVP endpoints are implemented and the sample app validates the full flow
- **Binary Compatibility Validator:** The JetBrains `kotlinx-binary-compatibility-validator` Gradle plugin runs on every build. Any unintentional public API change fails the build. This is non-negotiable.
- **Changelog:** Maintained in `CHANGELOG.md` at repo root using Keep a Changelog format (`Added`, `Changed`, `Deprecated`, `Removed`, `Fixed`, `Security`)
- **Snapshot builds:** `-SNAPSHOT` suffix on unreleased development builds

---

## 16. Distribution & Publishing

The SDK must be publishable to all three targets from a single Gradle configuration.

### 16.1 Registries

| Registry | Use case | Notes |
|---|---|---|
| Maven Central | Public open-source distribution | Requires Sonatype OSSRH account, domain-verified group ID, GPG signing |
| JitPack | Easy public fallback | Automatic from GitHub tags, no manual publish step |
| Private registry | Perspective Global internal / enterprise clients | GitHub Packages or Nexus/Artifactory |

### 16.2 Group ID

Use a group ID tied to a domain Perspective Global owns, e.g. `io.squarescreen` or `com.perspectiveglobal`. This is required for Maven Central. Secure this early — domain verification takes time.

### 16.3 Signing

- GPG/PGP key required for Maven Central
- Key stored in GitHub Actions secrets, never in the repository
- All release artifacts must be signed before upload

### 16.4 Publish Plugin

Use `com.vanniktech.maven.publish` (the `gradle-maven-publish-plugin`) — it supports Maven Central, GitHub Packages, and arbitrary registries from a single config block. Set it up once and configure per-target via environment variables in CI.

### 16.5 CI/CD Release Flow

```
git tag v0.1.0
→ GitHub Actions triggers on tag push
→ Runs tests + binary compat check
→ Signs artifacts with GPG key from secrets
→ Publishes to Maven Central staging
→ Publishes to GitHub Packages (private registry)
→ JitPack picks up tag automatically
→ Creates GitHub Release with CHANGELOG entry
```

---

## 17. ProGuard / R8

The SDK must ship `consumer-proguard-rules.pro` in each module. This file is automatically merged into the integrator's app build — they write nothing.

At minimum, keep rules for:
- All public model classes in `squarescreen-core` (to prevent field name obfuscation breaking JSON parsing)
- Retrofit and OkHttp (standard rules)
- Room (standard rules)
- Any reflection-based serialization

Do **not** keep internal implementation classes. Only what is strictly needed to prevent runtime crashes.

---

## 18. Testing Requirements

### 18.1 Unit Tests (per module)

- `squarescreen-core`: Model serialization/deserialization, sealed type exhaustiveness
- `squarescreen-network`: MockWebServer tests for all three endpoints — success paths, error paths, malformed responses, auth header injection
- `squarescreen-cache`: Room in-memory database tests, file cache read/write/eviction
- `squarescreen-player`: Playlist sequencing logic, scheduler date/time rules, heartbeat payload construction, emergency state transitions

### 18.2 Integration Tests

- End-to-end flow against MockWebServer: init → fetch playlist → receive emergency → clear emergency → verify playlist resumes
- Heartbeat payload verification: all fields present, temperature gracefully null when unavailable

### 18.3 Sample App (Manual Smoke Test)

Before every release, run the sample app against a real or staging backend and verify:
- [ ] Device registers and fetches playlist
- [ ] Content displays and sequences correctly
- [ ] Heartbeat appears in the SquareScreen dashboard
- [ ] Emergency broadcast renders as full-screen overlay
- [ ] Emergency clear restores playlist
- [ ] Kill the app, relaunch — WorkManager resumes heartbeat

---

## 19. Developer Experience Requirements

These are requirements, not suggestions:

- **README** at repo root: quick start in under 20 lines of code
- **KDoc** on every public API surface — class, function, and parameter level
- **Migration guide** for every breaking change (major version)
- **Sample app** is readable as documentation — comments explain why, not just what
- **`SquareScreenNotInitializedException`** has a clear message that tells the developer exactly what to call and where
- The SDK must compile cleanly with zero warnings in the sample app's build output

---

## 20. Legal & Compliance Checklist

- [ ] License decided and placed in `LICENSE` file at repo root
- [ ] License header added to every source file
- [ ] Third-party dependency licenses acknowledged (Retrofit, OkHttp, Room, ExoPlayer/Media3, Glide/Coil — all Apache 2.0, compatible with most licenses)
- [ ] Maven Central requires a license declaration in the POM — ensure it is correct
- [ ] SDK token security guidance included in developer documentation
- [ ] Responsible disclosure / bug report path documented in `SECURITY.md`

---

## 21. Open Decisions (resolve before or during development)

| Decision | Options | Notes |
|---|---|---|
| JSON serialization library | `kotlinx.serialization` vs Gson | kotlinx.serialization preferred (pure Kotlin, no reflection), but check if Retrofit adapter is stable enough |
| Image loading in `squarescreen-ui` | Coil vs Glide | Coil is Kotlin-first and Compose-native; Glide is battle-tested. For a signage context (large images, aggressive caching), Glide may be safer. |
| SDK open source vs closed source | Public GitHub vs private | Affects JitPack viability and license choice. Decide before first public release. |
| `player_version` source | Hardcoded `BuildConfig.VERSION_NAME` | SDK must expose its own version string — ensure this is set correctly in the library's `build.gradle.kts` |
| Foreground service notification | SDK-owned vs integrator-managed | Current spec: integrator provides title + icon, SDK creates the notification. Revisit if integrators need full notification control. |

---

## 22. What to Build First (Recommended Order for Claude Code)

1. **Project scaffolding** — all module directories, `build.gradle.kts` per module, root Gradle config, Binary Compatibility Validator setup
2. **`squarescreen-core`** — all models, sealed types, interfaces, annotation. No Android dependencies yet.
3. **`squarescreen-network`** — Retrofit client, auth interceptor, `NetworkDataSource` implementation for the three known endpoints, MockWebServer tests
4. **`squarescreen-cache`** — Room DB schema, disk cache, default `CacheProvider` implementation
5. **`squarescreen-player`** — `SquareScreen` singleton, init/shutdown, all three Flows, WorkManager heartbeat, emergency polling, foreground service stub
6. **`squarescreen-ui`** — Compose player view, emergency overlay, Media3 video, Coil/Glide images
7. **`sample-app`** — full flow against mock or real backend
8. **Publishing config** — `gradle-maven-publish-plugin` setup for all three registries
9. **ProGuard rules** — per module, after implementation is stable
10. **Documentation pass** — KDoc, README, CHANGELOG

---

*End of specification. All decisions in this document are final unless explicitly revisited. New API endpoints slot into the existing structure — update `NetworkDataSource`, add the corresponding model to `squarescreen-core`, and expose via a new Flow or suspend function in `squarescreen-player` as appropriate.*
