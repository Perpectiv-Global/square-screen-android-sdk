package io.squarescreen.cache.file

import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class MediaFileCacheTest {

    private lateinit var cacheDir: File
    private lateinit var cache: MediaFileCache

    @Before
    fun setUp() {
        cacheDir = createTempDir("squarescreen_test_media")
        cache = MediaFileCache(cacheDir)
    }

    @After
    fun tearDown() {
        cacheDir.deleteRecursively()
    }

    @Test
    fun getFile_returnsNull_whenNotCached() {
        assertNull(cache.getFile("https://cdn.example.com/banner.jpg"))
    }

    @Test
    fun saveAndGetFile_returnsCachedFile() {
        val source = File.createTempFile("test", ".jpg")
        source.writeBytes(byteArrayOf(1, 2, 3, 4))

        cache.saveFile("https://cdn.example.com/banner.jpg", source)
        val retrieved = cache.getFile("https://cdn.example.com/banner.jpg")

        assertNotNull(retrieved)
        assertArrayEquals(byteArrayOf(1, 2, 3, 4), retrieved!!.readBytes())
        source.delete()
    }

    @Test
    fun sameUrlProducesSameFile() {
        val url = "https://cdn.example.com/image.jpg"
        val source1 = File.createTempFile("tmp1", ".jpg").also { it.writeText("first") }
        val source2 = File.createTempFile("tmp2", ".jpg").also { it.writeText("second") }

        cache.saveFile(url, source1)
        cache.saveFile(url, source2)

        // Second write should overwrite — only one file should exist for this URL
        assertEquals("second", cache.getFile(url)!!.readText())
        source1.delete()
        source2.delete()
    }

    @Test
    fun differentUrlsProduceDifferentFiles() {
        val source1 = File.createTempFile("tmp1", ".jpg").also { it.writeText("image1") }
        val source2 = File.createTempFile("tmp2", ".jpg").also { it.writeText("image2") }

        cache.saveFile("https://cdn.example.com/a.jpg", source1)
        cache.saveFile("https://cdn.example.com/b.jpg", source2)

        assertEquals("image1", cache.getFile("https://cdn.example.com/a.jpg")!!.readText())
        assertEquals("image2", cache.getFile("https://cdn.example.com/b.jpg")!!.readText())
        source1.delete()
        source2.delete()
    }

    @Test
    fun clearAll_removesAllCachedFiles() {
        val source = File.createTempFile("tmp", ".jpg").also { it.writeText("data") }
        val url = "https://cdn.example.com/file.jpg"
        cache.saveFile(url, source)

        cache.clearAll()

        assertNull(cache.getFile(url))
        assertTrue(cacheDir.listFiles()?.isEmpty() ?: true)
        source.delete()
    }
}
