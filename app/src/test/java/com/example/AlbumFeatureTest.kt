package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.AlbumItem
import com.example.data.model.MediaItem
import com.example.data.model.ReminderItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AlbumFeatureTest {

    private lateinit var database: AppDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun createAlbumAndVerifyPersistence() = runBlocking {
        val album = AlbumItem(
            title = "Vacation Memories",
            description = "Trip to Hawaii",
            colorHex = 0xFF6750A4,
            reminderTimestamp = System.currentTimeMillis() + 86400000L
        )
        val albumId = database.albumDao().insert(album)
        assertTrue(albumId > 0)

        val loaded = database.albumDao().getAlbumByIdDirect(albumId)
        assertNotNull(loaded)
        assertEquals("Vacation Memories", loaded?.title)
        assertEquals("Trip to Hawaii", loaded?.description)
        assertNotNull(loaded?.reminderTimestamp)
    }

    @Test
    fun addMediaItemsToAlbumAndQuery() = runBlocking {
        val albumId = database.albumDao().insert(
            AlbumItem(title = "Summer Album", description = "Test")
        )

        val photo = MediaItem(
            albumId = albumId,
            uriString = "/data/user/0/com.example/files/albums/$albumId/photo_1.jpg",
            isVideo = false
        )
        val video = MediaItem(
            albumId = albumId,
            uriString = "/data/user/0/com.example/files/albums/$albumId/video_1.mp4",
            isVideo = true
        )

        database.mediaDao().insertMedia(photo)
        database.mediaDao().insertMedia(video)

        val mediaList = database.mediaDao().getMediaForAlbum(albumId).first()
        assertEquals(2, mediaList.size)
        assertEquals(1, mediaList.count { !it.isVideo })
        assertEquals(1, mediaList.count { it.isVideo })

        val totalMedia = database.mediaDao().getMediaCountForAlbum(albumId).first()
        assertEquals(2, totalMedia)
    }

    @Test
    fun createReminderLinkedToAlbum() = runBlocking {
        val albumId = database.albumDao().insert(
            AlbumItem(title = "Birthday Celebration")
        )

        val reminder = ReminderItem(
            title = "Review Birthday Celebration album",
            description = "Check newly added photos & videos",
            dueTimestamp = System.currentTimeMillis() + 3600000L,
            albumId = albumId,
            albumTitle = "Birthday Celebration"
        )

        val reminderId = database.reminderDao().insert(reminder)
        assertTrue(reminderId > 0)

        val albumReminder = database.reminderDao().getReminderByAlbumId(albumId)
        assertNotNull(albumReminder)
        assertEquals(albumId, albumReminder?.albumId)
        assertEquals("Birthday Celebration", albumReminder?.albumTitle)

        // Delete reminder for album
        database.reminderDao().deleteByAlbumId(albumId)
        val deleted = database.reminderDao().getReminderByAlbumId(albumId)
        assertNull(deleted)
    }

    @Test
    fun deleteAlbumRemovesAllAssociatedMedia() = runBlocking {
        val albumId = database.albumDao().insert(
            AlbumItem(title = "Temp Album")
        )

        database.mediaDao().insertMedia(
            MediaItem(albumId = albumId, uriString = "dummy_path", isVideo = false)
        )

        val album = database.albumDao().getAlbumByIdDirect(albumId)
        assertNotNull(album)

        database.mediaDao().deleteMediaForAlbum(albumId)
        database.albumDao().delete(album!!)

        val remainingMedia = database.mediaDao().getMediaForAlbum(albumId).first()
        assertTrue(remainingMedia.isEmpty())

        val remainingAlbum = database.albumDao().getAlbumByIdDirect(albumId)
        assertNull(remainingAlbum)
    }
}
