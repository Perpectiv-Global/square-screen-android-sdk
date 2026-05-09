# Consumer ProGuard rules for squarescreen-core
# These rules are automatically applied to any app that depends on this module.

# ─── Public models ───────────────────────────────────────────────────────────
# Keep all public data classes — their fields may be inspected by integrators
# and are used by kotlinx.serialization at runtime.
-keep class io.squarescreen.core.model.** { *; }
-keep class io.squarescreen.core.result.** { *; }
-keep class io.squarescreen.core.config.** { *; }
-keep class io.squarescreen.core.annotation.** { *; }
-keep class io.squarescreen.core.exception.** { *; }
-keep class io.squarescreen.core.logging.** { *; }

# Keep interfaces so integrators can implement them
-keep interface io.squarescreen.core.cache.CacheProvider { *; }
-keep interface io.squarescreen.core.datasource.NetworkDataSource { *; }

# ─── Kotlin sealed classes & enums ───────────────────────────────────────────
# Sealed class subclasses must be retained for exhaustive `when` expressions
-keep class io.squarescreen.core.result.SquareScreenResult$* { *; }
-keep class io.squarescreen.core.result.SquareScreenError$* { *; }
-keep class io.squarescreen.core.model.CommandType$* { *; }
-keepclassmembers enum io.squarescreen.core.model.** { *; }
-keepclassmembers enum io.squarescreen.core.result.** { *; }

# ─── Kotlin metadata & reflection ────────────────────────────────────────────
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses, EnclosingMethod

# ─── Kotlin coroutines ───────────────────────────────────────────────────────
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
