package io.squarescreen.sample.data

import android.content.Context
import io.squarescreen.core.model.PlaybackReport

/**
 * Persists failed playback reports in SharedPreferences so they can be retried
 * when network connectivity is restored.
 *
 * Each report is encoded as a pipe-delimited string:
 * item_id|started_at|ended_at|duration_seconds|completed
 */
class PendingPlaybackStore(context: Context) {

    private val prefs = context.getSharedPreferences("pending_playbacks", Context.MODE_PRIVATE)

    fun add(report: PlaybackReport) {
        val encoded = encode(report)
        val current = prefs.getStringSet(KEY, emptySet())!!.toMutableSet()
        current.add(encoded)
        prefs.edit().putStringSet(KEY, current).apply()
    }

    fun getAll(): List<PlaybackReport> =
        prefs.getStringSet(KEY, emptySet())!!.mapNotNull { decode(it) }

    fun clear() {
        prefs.edit().remove(KEY).apply()
    }

    val isEmpty: Boolean
        get() = prefs.getStringSet(KEY, emptySet())!!.isEmpty()

    private fun encode(report: PlaybackReport): String =
        "${report.id}|${report.startedAt}|${report.endedAt}|${report.durationSeconds}|${report.completed}"

    private fun decode(encoded: String): PlaybackReport? {
        val parts = encoded.split("|")
        if (parts.size != 5) return null
        return try {
            PlaybackReport(
                id = parts[0],
                startedAt = parts[1],
                endedAt = parts[2],
                durationSeconds = parts[3].toInt(),
                completed = parts[4].toBoolean()
            )
        } catch (e: Exception) {
            null
        }
    }

    private companion object {
        const val KEY = "reports"
    }
}
