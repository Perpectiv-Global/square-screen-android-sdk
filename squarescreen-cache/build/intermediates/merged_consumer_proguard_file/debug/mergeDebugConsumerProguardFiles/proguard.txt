# Consumer ProGuard rules for squarescreen-cache

# ─── Room ────────────────────────────────────────────────────────────────────
# Room generates implementations at compile time via KSP, but the entity and
# DAO classes are still accessed reflectively at runtime.
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers @androidx.room.Entity class * { *; }

# Room uses field names to map columns — prevent them from being renamed.
-keep class io.squarescreen.cache.db.entity.** { *; }
-keep class io.squarescreen.cache.db.dao.** { *; }

# ─── SQLite / Cursor ─────────────────────────────────────────────────────────
-dontwarn android.database.sqlite.**

# ─── Public factory ──────────────────────────────────────────────────────────
-keep class io.squarescreen.cache.CacheProviderFactory { *; }
-keep class io.squarescreen.cache.DefaultCacheProvider { *; }
