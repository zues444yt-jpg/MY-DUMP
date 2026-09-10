package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AlbumItem
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumDao {
    @Query("SELECT * FROM albums ORDER BY createdAt DESC")
    fun getAllAlbums(): Flow<List<AlbumItem>>

    @Query("SELECT * FROM albums WHERE id = :id")
    fun getAlbumById(id: Long): Flow<AlbumItem?>

    @Query("SELECT * FROM albums WHERE id = :id")
    suspend fun getAlbumByIdDirect(id: Long): AlbumItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(album: AlbumItem): Long

    @Update
    suspend fun update(album: AlbumItem)

    @Delete
    suspend fun delete(album: AlbumItem)

    @Query("DELETE FROM albums WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE albums SET reminderTimestamp = :timestamp, reminderId = :reminderId WHERE id = :albumId")
    suspend fun updateReminder(albumId: Long, timestamp: Long?, reminderId: Long?)
}
