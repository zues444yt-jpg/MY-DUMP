package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.MediaItem
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM media_items WHERE albumId = :albumId ORDER BY addedAt DESC")
    fun getMediaForAlbum(albumId: Long): Flow<List<MediaItem>>

    @Query("SELECT COUNT(*) FROM media_items WHERE albumId = :albumId")
    fun getMediaCountForAlbum(albumId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM media_items WHERE albumId = :albumId AND isVideo = 0")
    fun getImageCountForAlbum(albumId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM media_items WHERE albumId = :albumId AND isVideo = 1")
    fun getVideoCountForAlbum(albumId: Long): Flow<Int>

    @Query("SELECT * FROM media_items WHERE albumId = :albumId ORDER BY addedAt DESC LIMIT 1")
    suspend fun getLatestMediaForAlbum(albumId: Long): MediaItem?

    @Query("SELECT * FROM media_items")
    fun getAllMedia(): Flow<List<MediaItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(item: MediaItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllMedia(items: List<MediaItem>)

    @Delete
    suspend fun deleteMedia(item: MediaItem)

    @Query("DELETE FROM media_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM media_items WHERE albumId = :albumId")
    suspend fun deleteMediaForAlbum(albumId: Long)
}
