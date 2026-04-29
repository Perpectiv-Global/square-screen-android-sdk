package io.squarescreen.cache.file

import java.io.File
import java.security.MessageDigest

/**
 * Disk-based cache for media files (images and video).
 *
 * Files are stored in [cacheDir] using a SHA-256 hash of the source URL as
 * the file name. This ensures the same remote file is never downloaded twice,
 * even if the URL appears in multiple playlists.
 *
 * The cache directory is the app's private external storage
 * (`context.getExternalFilesDir("squarescreen_media")`). No storage
 * permissions are required on API 29+.
 */
internal class MediaFileCache(private val cacheDir: File) {

    init {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }

    fun getFile(url: String): File? {
        val file = fileForUrl(url)
        return if (file.exists()) file else null
    }

    fun saveFile(url: String, source: File): File {
        val destination = fileForUrl(url)
        source.copyTo(destination, overwrite = true)
        return destination
    }

    fun clearAll() {
        cacheDir.listFiles()?.forEach { it.delete() }
    }

    private fun fileForUrl(url: String): File {
        val hash = sha256(url)
        return File(cacheDir, hash)
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
