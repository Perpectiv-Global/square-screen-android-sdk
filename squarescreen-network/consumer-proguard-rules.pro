# Consumer ProGuard rules for squarescreen-network

# ─── Retrofit ────────────────────────────────────────────────────────────────
# Retrofit uses reflection to build service interfaces and parse annotations.
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*

-keep class retrofit2.** { *; }
-keepclassmembernames interface * {
    @retrofit2.http.* <methods>;
}

# ─── OkHttp ──────────────────────────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ─── kotlinx.serialization ───────────────────────────────────────────────────
# The serialization plugin generates companion objects that R8 may otherwise remove.
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
    @kotlinx.serialization.Serializable <fields>;
}
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers @kotlinx.serialization.Serializable class * {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class kotlinx.serialization.** { *; }
-dontwarn kotlinx.serialization.**

# ─── Internal DTOs ───────────────────────────────────────────────────────────
# DTOs are serialized/deserialized by kotlinx.serialization; field names must
# survive minification to match @SerialName annotations.
-keep class io.squarescreen.network.dto.** { *; }

# ─── Interceptors ────────────────────────────────────────────────────────────
-keep class io.squarescreen.network.interceptor.** { *; }
