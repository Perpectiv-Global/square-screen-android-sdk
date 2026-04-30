# ProGuard / R8 rules for the SquareScreen sample app.
# Rules from library modules are automatically merged via their
# consumer-proguard-rules.pro files — only app-specific rules go here.

# ─── Sample app classes ──────────────────────────────────────────────────────
-keep class io.squarescreen.sample.** { *; }

# ─── EncryptedSharedPreferences / Tink ───────────────────────────────────────
# androidx.security.crypto uses Tink internally; suppress noisy warnings.
-dontwarn com.google.crypto.tink.**
-keep class com.google.crypto.tink.** { *; }

# ─── General Android ─────────────────────────────────────────────────────────
# Keep custom Application subclass (registered in AndroidManifest)
-keep class io.squarescreen.sample.SampleApplication { *; }

# ─── Debugging ───────────────────────────────────────────────────────────────
# Preserve source file names and line numbers in crash stack traces.
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
