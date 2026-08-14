package io.squarescreen.cache.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.squarescreen.cache.db.entity.PlaylistEntity
import io.squarescreen.cache.db.entity.PlaylistItemEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaylistDaoTest {

    private lateinit var db: SquareScreenDatabase
    private lateinit var dao: io.squarescreen.cache.db.dao.PlaylistDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SquareScreenDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.playlistDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun getPlaylist_returnsNull_whenEmpty() = runTest {
        assertNull(dao.getPlaylist())
    }

    @Test
    fun upsertAndGetPlaylist_returnsStoredEntity() = runTest {
        val entity = playlistEntity(cachedAt = 1000L)
        dao.upsertPlaylist(entity)
        val stored = dao.getPlaylist()
        assertNotNull(stored)
        assertEquals(1000L, stored!!.cachedAt)
    }

    @Test
    fun replacePlaylist_replacesItemsAtomically() = runTest {
        val entity = playlistEntity(cachedAt = 1000L)
        val items = listOf(
            itemEntity(id = 1, url = "https://cdn.example.com/a.jpg", sortOrder = 0),
            itemEntity(id = 2, url = "https://cdn.example.com/b.jpg", sortOrder = 1)
        )
        dao.replacePlaylist(entity, items)

        val storedItems = dao.getPlaylistItems()
        assertEquals(2, storedItems.size)
        assertEquals("https://cdn.example.com/a.jpg", storedItems[0].url)
    }

    @Test
    fun replacePlaylist_deletesOldItems() = runTest {
        val entity = playlistEntity(cachedAt = 1000L)
        dao.replacePlaylist(entity, listOf(itemEntity(id = 1, url = "old.jpg", sortOrder = 0)))

        val newEntity = playlistEntity(cachedAt = 2000L)
        dao.replacePlaylist(newEntity, listOf(itemEntity(id = 2, url = "new.jpg", sortOrder = 0)))

        val storedItems = dao.getPlaylistItems()
        assertEquals(1, storedItems.size)
        assertEquals("new.jpg", storedItems[0].url)
    }

    @Test
    fun getPlaylistItems_returnsInSortOrder() = runTest {
        val entity = playlistEntity(cachedAt = 1000L)
        val items = listOf(
            itemEntity(id = 3, url = "c.jpg", sortOrder = 2),
            itemEntity(id = 1, url = "a.jpg", sortOrder = 0),
            itemEntity(id = 2, url = "b.jpg", sortOrder = 1)
        )
        dao.replacePlaylist(entity, items)

        val stored = dao.getPlaylistItems()
        assertEquals("a.jpg", stored[0].url)
        assertEquals("b.jpg", stored[1].url)
        assertEquals("c.jpg", stored[2].url)
    }

    @Test
    fun clearAll_removesAllData() = runTest {
        val entity = playlistEntity(cachedAt = 1000L)
        dao.replacePlaylist(entity, listOf(itemEntity(id = 1, url = "a.jpg", sortOrder = 0)))
        dao.clearAll()

        assertNull(dao.getPlaylist())
        assertTrue(dao.getPlaylistItems().isEmpty())
    }

    // --- helpers ---

    private fun playlistEntity(cachedAt: Long) = PlaylistEntity(
        id = 1,
        cachedAt = cachedAt,
        strategyLoop = true,
        strategyShuffle = false,
        strategyPreloadCount = 3,
        scheduleUuid = null,
        scheduleName = null,
        schedulePriority = null,
        playlistUuid = null,
        playlistName = null,
        playlistServerId = null
    )

    private fun itemEntity(id: Int, url: String, sortOrder: Int) = PlaylistItemEntity(
        id = id.toString(),
        playlistId = 1,
        type = "image",
        url = url,
        duration = 10,
        transition = "fade",
        sortOrder = sortOrder
    )
}
