# Consumer ProGuard rules for squarescreen-player

# ─── Public API ──────────────────────────────────────────────────────────────
# The SquareScreen singleton is the primary integration point — must be fully kept.
-keep class io.squarescreen.player.SquareScreen { *; }

# ─── WorkManager workers ─────────────────────────────────────────────────────
# WorkManager instantiates workers by class name stored in the JobScheduler DB.
# If these are renamed or removed, background jobs will silently fail after app update.
-keep class io.squarescreen.player.worker.HeartbeatWorker { *; }
-keep class io.squarescreen.player.worker.EmergencyPollWorker { *; }
-keep class io.squarescreen.player.worker.CommandPollWorker { *; }

# ─── Foreground service ──────────────────────────────────────────────────────
# Service class name is registered in the manifest — must not be renamed.
-keep class io.squarescreen.player.service.SquareScreenPlayerService { *; }

# ─── WorkManager library ─────────────────────────────────────────────────────
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**

# ─── Internal (not public, but needed at runtime) ────────────────────────────
-keep class io.squarescreen.player.internal.SquareScreenServiceLocator { *; }
