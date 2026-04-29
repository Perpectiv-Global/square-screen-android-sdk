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
     * Reads CPU usage from /proc/stat.
     * Computes (total - idle) / total as a percentage.
     * Returns null if /proc/stat is unavailable or unreadable.
     */
    fun getCpuUsage(): Float? {
        return try {
            val lines = java.io.File("/proc/stat").readLines()
            val cpuLine = lines.firstOrNull { it.startsWith("cpu ") } ?: return null
            val parts = cpuLine.trim().split("\\s+".toRegex()).drop(1).mapNotNull { it.toLongOrNull() }
            if (parts.size < 4) return null
            val idle = parts[3]
            val total = parts.sum()
            if (total == 0L) return null
            ((total - idle).toFloat() / total.toFloat() * 100f).coerceIn(0f, 100f)
        } catch (e: Exception) {
            SquareScreenServiceLocator.logError(TAG, "Failed to read CPU usage", e)
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
