# SquareScreen Android SDK — Claude Code Guide

## Project Overview

This is the **SquareScreen Android SDK** — a device-side player SDK that turns an Android device into a SquareScreen digital signage display. It authenticates devices, fetches playlists, maintains heartbeats, handles emergency broadcasts, and caches content for offline continuity.

**Version:** 0.1.0 (pre-stable)
**Owner:** Perspective Global
**Language:** Kotlin 100%
**Build System:** Gradle with Kotlin DSL (`.kts`)

---

## Non-Negotiable Rules

1. **No DI frameworks** in the public API — no Hilt, Dagger, or Koin. Use internal factory/object graph.
2. **No direct `android.util.Log` calls** — all logging goes through the internal `SquareScreenLogger` reference.
3. **Binary compatibility is enforced** via `kotlinx-binary-compatibility-validator` on every build. Never break it silently.
4. **UI module is optional** — `squarescreen-core` and `squarescreen-player` are headless.
5. **Ship `consumer-proguard-rules.pro`** in every module — integrators write zero ProGuard rules.
6. **SDK never throws** unchecked exceptions into integrator code (except `SquareScreenNotInitializedException` pre-init).
7. **Flows never terminate on error** — emit `SquareScreenResult.Error` and continue.

---

## Module Structure

```
squarescreen-android-sdk/
├── squarescreen-core/       ← Models, sealed types, interfaces, config, annotation
├── squarescreen-network/    ← Retrofit + OkHttp, auth interceptor, API client (internal)
├── squarescreen-cache/      ← Room metadata cache + disk media cache (internal)
├── squarescreen-player/     ← SquareScreen singleton, Flows, WorkManager, foreground service
├── squarescreen-ui/         ← Compose player view + emergency overlay (OPTIONAL)
└── sample-app/              ← Full reference integration app
```

### Module Dependency Graph

```
squarescreen-ui
    └── squarescreen-player
            ├── squarescreen-network
            ├── squarescreen-cache
            └── squarescreen-core

squarescreen-network → squarescreen-core
squarescreen-cache   → squarescreen-core
sample-app           → squarescreen-ui (or squarescreen-player for headless)
```

**Rule:** Integrators never depend on `squarescreen-network` or `squarescreen-cache` directly — they are implementation details.

---

## Platform Targets

| Property | Value |
|---|---|
| Min SDK | API 29 (Android 10) |
| Target SDK | Latest stable |
| Language | Kotlin 100% |
| UI toolkit | Jetpack Compose (ui module only) |

---

## Key Architecture Decisions

| Decision | Choice | Reason |
|---|---|---|
| JSON serialization | `kotlinx.serialization` (preferred) | Pure Kotlin, no reflection |
| Image loading | Coil (Compose-native) or Glide (battle-tested) | TBD — Glide may be safer for large signage images |
| Emergency transport | WorkManager polling (swappable to WebSocket/FCM) | Abstract behind internal interface |
| Background work | WorkManager | Survives process death |

---

## Public API Summary

### Initialization (call once in `Application.onCreate()`)

```kotlin
SquareScreen.init(
    context = applicationContext,
    config = SquareScreenConfig(
        baseUrl = "https://api.squarescreen.io",
        deviceId = "device-uuid",
        deviceToken = "device-token",
        heartbeatIntervalSeconds = 60,         // default 60, min 30
        emergencyPollIntervalSeconds = 30,     // default 30, min 15
        foregroundNotification = ForegroundNotificationConfig(
            title = "SquareScreen Player",
            iconResId = R.drawable.ic_player
        ),
        logger = null                          // null = silent (default)
    )
)
val squareScreen = SquareScreen.getInstance()
```

- `init()` called more than once → no-op (log warning)
- `getInstance()` before `init()` → throws `SquareScreenNotInitializedException`

### Core Flows

```kotlin
squareScreen.nowPlaying: Flow<SquareScreenResult<Playlist>>
squareScreen.emergencyAlert: Flow<EmergencyAlert?>      // null = no active alert
squareScreen.deviceStatus: Flow<DeviceStatus>
```

### Manual Controls

```kotlin
suspend fun squareScreen.refresh(): SquareScreenResult<Playlist>
suspend fun squareScreen.checkEmergency(): EmergencyAlert?
fun squareScreen.shutdown()
```

---

## Known API Endpoints

### `GET /api/v1/screen/now-playing`
- Headers: `X-Device-Id`, `X-Device-Token`
- Query: `type`, `category`, `quality` (default 1080p), `limit` (default 20)
- Empty `items: []` is **valid** — show fallback state, not an error

### `POST /api/v1/screen/heartbeat`
- Headers: `X-Device-Id`, `X-Device-Token`
- Body: `cpu_usage`, `memory_usage`, `disk_usage`, `temperature` (nullable), `os_version`, `player_version`
- `temperature` is best-effort — always send `null` if unavailable, never throw

### `GET /api/v1/screen/emergency`
- Headers: `X-Device-Id`, `X-Device-Token`
- Response: `{ "active": true/false, "broadcast": {...} | null }`

**Auth:** All requests use `DeviceAuthInterceptor` — headers injected automatically. Developers never touch auth headers.

---

## Error Types

```kotlin
sealed class SquareScreenResult<out T>
    Success<T>(val data: T)
    Error(val error: SquareScreenError)

sealed class SquareScreenError
    NetworkError(code: Int, message: String)
    AuthError(message: String)
    CacheError(message: String)
    ParseError(message: String)
    EmergencyOverrideActive
    Unknown(throwable: Throwable)
```

---

## Background Work

| Task | Type | Interval | WorkManager Tag |
|---|---|---|---|
| Heartbeat | `PeriodicWorkRequest` | `heartbeatIntervalSeconds` (default 60s) | `squarescreen_heartbeat` |
| Emergency poll | `PeriodicWorkRequest` | `emergencyPollIntervalSeconds` (default 30s) | `squarescreen_emergency_poll` |
| Media prefetch | `OneTimeWorkRequest` chained per item | On playlist receive | — |

Foreground service: `FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK`, started on init, stopped on `shutdown()`.

---

## Caching

- **Metadata:** Room database (`PlaylistEntity`, `PlaylistItemEntity`)
- **Media:** `context.getExternalFilesDir("squarescreen_media")` — file names are URL hashes
- **TTL:** `SquareScreenConfig.cacheTtlSeconds` (default 3600)
- **Fallback:** On network failure, serve from cache → emit `DeviceStatus.OFFLINE`
- **Custom cache:** Implement `CacheProvider` and pass via `SquareScreenConfig.cacheProvider`

---

## Build & Release

- **Versioning:** SemVer — currently `0.1.0` (pre-stable, breaking on minor bumps)
- **Binary compat:** `kotlinx-binary-compatibility-validator` — fails build on unintentional API changes
- **Publish plugin:** `com.vanniktech.maven.publish`
- **Targets:** Maven Central, JitPack, GitHub Packages (private)
- **Signing:** GPG key in GitHub Actions secrets — never in repo
- **Changelog:** `CHANGELOG.md` in Keep a Changelog format

---

## Recommended Build Order

1. Project scaffolding — module dirs, `build.gradle.kts`, root Gradle, binary compat validator
2. `squarescreen-core` — models, sealed types, interfaces, annotation (no Android deps)
3. `squarescreen-network` — Retrofit, auth interceptor, `NetworkDataSource` impl, MockWebServer tests
4. `squarescreen-cache` — Room schema, disk cache, default `CacheProvider`
5. `squarescreen-player` — `SquareScreen` singleton, Flows, WorkManager, foreground service
6. `squarescreen-ui` — Compose player view, emergency overlay, Media3, Coil/Glide
7. `sample-app` — full flow against mock or real backend
8. Publishing config — all three registries
9. ProGuard rules — per module
10. Docs pass — KDoc, README, CHANGELOG

---

## Testing

- **Unit:** MockWebServer for network, Room in-memory for cache, coroutine test rules for Flows
- **Integration:** end-to-end with MockWebServer — init → playlist → emergency → clear → resume
- **Smoke (manual before release):** Run sample app against staging, verify all checklist items in spec section 18.3

---

## Experimental API

Use `@ExperimentalSquareScreenApi` for APIs subject to change (e.g. `PlaybackStrategy`). Integrators opt in with `@OptIn(ExperimentalSquareScreenApi::class)`.

---

## Security Notes

- `deviceToken` lives on the device — document that integrators must **not** hardcode it. Recommend `EncryptedSharedPreferences`.
- `SquareScreenDebugLogger` is for development only — never ship in production builds.
- Use `if (BuildConfig.DEBUG) SquareScreenDebugLogger() else null` pattern.
