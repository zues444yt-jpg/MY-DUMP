package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.local.AlbumDao
import com.example.data.local.AppDatabase
import com.example.data.local.MediaDao
import com.example.data.local.ReminderDao
import com.example.data.model.AlbumItem
import com.example.data.model.AlbumWithDetails
import com.example.data.model.MediaItem
import com.example.data.model.Priority
import com.example.data.model.ReminderCategory
import com.example.data.model.ReminderItem
import com.example.data.model.RepeatMode
import com.example.receiver.ReminderAlarmScheduler
import com.example.util.MediaStorageManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class AlbumRepository(
    private val albumDao: AlbumDao,
    private val mediaDao: MediaDao,
    private val reminderDao: ReminderDao,
    private val alarmScheduler: ReminderAlarmScheduler
) {
    val allAlbumsWithDetails: Flow<List<AlbumWithDetails>> =
        combine(albumDao.getAllAlbums(), mediaDao.getAllMedia()) { albums, allMedia ->
            albums.map { album ->
                val mediaForThisAlbum = allMedia.filter { it.albumId == album.id }
                val imageCount = mediaForThisAlbum.count { !it.isVideo }
                val videoCount = mediaForThisAlbum.count { it.isVideo }
                val latestMedia = mediaForThisAlbum.maxByOrNull { it.addedAt }

                AlbumWithDetails(
                    album = album,
                    mediaCount = mediaForThisAlbum.size,
                    imageCount = imageCount,
                    videoCount = videoCount,
                    latestMediaUri = latestMedia?.uriString ?: album.coverUri
                )
            }
        }

    fun getAlbumById(albumId: Long): Flow<AlbumItem?> = albumDao.getAlbumById(albumId)

    fun getMediaForAlbum(albumId: Long): Flow<List<MediaItem>> = mediaDao.getMediaForAlbum(albumId)

    suspend fun createAlbum(
        title: String,
        description: String = "",
        colorHex: Long = 0xFF6750A4,
        reminderTimestamp: Long? = null
    ): Long {
        val newAlbum = AlbumItem(
            title = title.trim(),
            description = description.trim(),
            colorHex = colorHex,
            reminderTimestamp = reminderTimestamp
        )
        val albumId = albumDao.insert(newAlbum)

        if (reminderTimestamp != null && reminderTimestamp > System.currentTimeMillis()) {
            val reminder = ReminderItem(
                title = "Album: ${title.trim()}",
                description = if (description.isNotBlank()) description.trim() else "Reminder for album ${title.trim()}",
                dueTimestamp = reminderTimestamp,
                category = ReminderCategory.ALBUMS.displayName,
                priority = Priority.HIGH,
                repeatMode = RepeatMode.NONE,
                albumId = albumId,
                albumTitle = title.trim()
            )
            val reminderId = reminderDao.insert(reminder)
            val scheduledReminder = reminder.copy(id = reminderId)
            alarmScheduler.schedule(scheduledReminder)

            // Update album with reminderId
            albumDao.updateReminder(albumId, reminderTimestamp, reminderId)
        }

        return albumId
    }

    suspend fun updateAlbum(album: AlbumItem) {
        albumDao.update(album)
    }

    suspend fun setAlbumReminder(
        albumId: Long,
        timestamp: Long,
        note: String = ""
    ) {
        val album = albumDao.getAlbumByIdDirect(albumId) ?: return

        // Check if there is already an existing reminder for this album
        val existingReminder = reminderDao.getReminderByAlbumId(albumId)
        val reminderToSave = (existingReminder ?: ReminderItem(
            title = "Album: ${album.title}",
            category = ReminderCategory.ALBUMS.displayName,
            priority = Priority.HIGH,
            albumId = albumId,
            albumTitle = album.title,
            dueTimestamp = timestamp
        )).copy(
            title = "Album: ${album.title}",
            description = if (note.isNotBlank()) note else "Reminder for album ${album.title}",
            dueTimestamp = timestamp,
            isCompleted = false,
            category = ReminderCategory.ALBUMS.displayName,
            albumId = albumId,
            albumTitle = album.title
        )

        val reminderId = if (existingReminder != null) {
            reminderDao.update(reminderToSave)
            existingReminder.id
        } else {
            reminderDao.insert(reminderToSave)
        }

        val scheduledReminder = reminderToSave.copy(id = reminderId)
        alarmScheduler.schedule(scheduledReminder)
        albumDao.updateReminder(albumId, timestamp, reminderId)
    }

    suspend fun removeAlbumReminder(albumId: Long) {
        val existingReminder = reminderDao.getReminderByAlbumId(albumId)
        if (existingReminder != null) {
            alarmScheduler.cancel(existingReminder.id)
            reminderDao.deleteById(existingReminder.id)
        }
        albumDao.updateReminder(albumId, null, null)
    }

    suspend fun deleteAlbum(context: Context, album: AlbumItem) {
        // Cancel reminder
        album.reminderId?.let { alarmScheduler.cancel(it) }
        reminderDao.deleteByAlbumId(album.id)

        // Delete disk storage
        MediaStorageManager.deleteAlbumDirectory(context, album.id)

        // Delete media items
        mediaDao.deleteMediaForAlbum(album.id)

        // Delete album
        albumDao.delete(album)
    }

    suspend fun addMediaToAlbum(
        context: Context,
        albumId: Long,
        uris: List<Uri>
    ): Int {
        var addedCount = 0
        for (uri in uris) {
            val savedResult = MediaStorageManager.saveMediaToInternalStorage(context, albumId, uri)
            if (savedResult != null) {
                val (filePath, isVideo) = savedResult
                val mediaItem = MediaItem(
                    albumId = albumId,
                    uriString = filePath,
                    isVideo = isVideo
                )
                mediaDao.insertMedia(mediaItem)
                addedCount++
            }
        }
        return addedCount
    }

    suspend fun deleteMediaItem(item: MediaItem) {
        MediaStorageManager.deleteFile(item.uriString)
        mediaDao.deleteMedia(item)
    }

    companion object {
        fun create(context: Context): AlbumRepository {
            val db = AppDatabase.getDatabase(context)
            val scheduler = ReminderAlarmScheduler(context)
            return AlbumRepository(
                db.albumDao(),
                db.mediaDao(),
                db.reminderDao(),
                scheduler
            )
        }
    }
}
