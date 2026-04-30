# Consumer ProGuard rules for squarescreen-ui

# ─── Public Compose entry points ─────────────────────────────────────────────
# Composable functions are called by name from integrators' code and must not
# be removed or renamed by R8.
-keep class io.squarescreen.ui.SquareScreenPlayerViewKt { *; }
-keep class io.squarescreen.ui.SquareScreenDisplayKt { *; }
-keep class io.squarescreen.ui.EmergencyOverlayViewKt { *; }

# ─── Coil ────────────────────────────────────────────────────────────────────
-dontwarn coil.**
-keep class coil.** { *; }

# ─── Media3 / ExoPlayer ──────────────────────────────────────────────────────
-dontwarn androidx.media3.**
-keep class androidx.media3.** { *; }
-keep interface androidx.media3.** { *; }

# ExoPlayer renderers and decoders are loaded by class name
-keepclassmembers class * extends androidx.media3.exoplayer.Renderer { *; }
-keepclassmembers class * extends androidx.media3.decoder.Decoder { *; }

# ─── Jetpack Compose ─────────────────────────────────────────────────────────
# R8 handles Compose well since 1.5, but keep stable API entry points
-keep @androidx.compose.runtime.Composable class * { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}
