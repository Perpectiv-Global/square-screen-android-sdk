package io.squarescreen.player.internal

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.StatFs

private const val TAG = "DeviceMetricsCollector"

internal class DeviceMetricsCollector(private val context: Context) {

    /**
     * Estimates this process's CPU usage by sampling /proc/self/stat twice with a
     * short delay and computing the delta in jiffies.
     *
     * /proc/stat (system-wide) is blocked on API 26+, but /proc/self/stat (process-level)
     * remains accessible. The result reflects the player process's CPU consumption rather
     * than device-wide usage — appropriate for a signage SDK heartbeat.
     *
     * Returns null if the file is unreadable or the delta is zero (no time has elapsed).
     */
    fun getCpuUsage(): Float? {
        return try {
            val sample1 = readProcessJiffies() ?: return null
            Thread.sleep(200)
            val sample2 = readProcessJiffies() ?: return null

            val processDelta = (sample2.first - sample1.first).toFloat()
            val totalDelta = (sample2.second - sample1.second).toFloat()

            if (totalDelta <= 0f) return null
            (processDelta / totalDelta * 100f).coerceIn(0f, 100f)
        } catch (e: Exception) {
            SquareScreenServiceLocator.log(TAG, "CPU usage unavailable: ${e.message}")
            null
        }
    }

    // Returns Pair(processJiffies, totalJiffies) from /proc/self/stat and /proc/stat uptime.
    // Falls back to wall-clock total if /proc/stat is unavailable.
    private fun readProcessJiffies(): Pair<Long, Long>? {
        return try {
            val parts = java.io.File("/proc/self/stat").readText().trim().split(" ")
            if (parts.size < 17) return null
            // Fields 13,14 = utime, stime; 15,16 = cutime, cstime (all in jiffies)
            val utime = parts[13].toLongOrNull() ?: return null
            val stime = parts[14].toLongOrNull() ?: return null
            val processJiffies = utime + stime
            val totalJiffies = System.nanoTime() / 10_000_000L // nanoseconds → centiseconds (jiffies at 100Hz)
            Pair(processJiffies, totalJiffies)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Reads memory usage via ActivityManager.
     * Returns used memory as a percentage of total.
     */
    fun getMemoryUsage(): Float? {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            am.getMemoryInfo(memInfo)
            if (memInfo.totalMem == 0L) return null
            val used = memInfo.totalMem - memInfo.availMem
            (used.toFloat() / memInfo.totalMem.toFloat() * 100f).coerceIn(0f, 100f)
        } catch (e: Exception) {
            SquareScreenServiceLocator.logError(TAG, "Failed to read memory usage", e)
            null
        }
    }

    /**
     * Reads disk usage via StatFs on the app's files directory.
     * Returns used storage as a percentage of total.
     */
    fun getDiskUsage(): Float? {
        return try {
            val stat = StatFs(context.filesDir.path)
            val total = stat.totalBytes
            if (total == 0L) return null
            val used = total - stat.freeBytes
            (used.toFloat() / total.toFloat() * 100f).coerceIn(0f, 100f)
        } catch (e: Exception) {
            SquareScreenServiceLocator.logError(TAG, "Failed to read disk usage", e)
            null
        }
    }

    /**
     * Reads device temperature from the BatteryManager sticky broadcast.
     * Returns Celsius as a Float, or null if unavailable.
     *
     * Note: Temperature reporting is inconsistent across manufacturers.
     * Some devices expose it here; others via thermal HAL files or not at all.
     * Never throws — always returns null on failure.
     */
    fun getTemperature(): Float? {
        return try {
            val intent = context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            val rawTemp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
            if (rawTemp == null || rawTemp == Int.MIN_VALUE) null
            else rawTemp / 10f
        } catch (e: Exception) {
            null // Explicitly silent — temperature is always best-effort
        }
    }
}
