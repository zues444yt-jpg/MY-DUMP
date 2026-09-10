package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ReminderItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders ORDER BY isCompleted ASC, dueTimestamp ASC")
    fun getAllReminders(): Flow<List<ReminderItem>>

    @Query("SELECT * FROM reminders WHERE isCompleted = 0 AND dueTimestamp > :now ORDER BY dueTimestamp ASC")
    suspend fun getUpcomingPendingReminders(now: Long = System.currentTimeMillis()): List<ReminderItem>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: Long): ReminderItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: ReminderItem): Long

    @Update
    suspend fun update(reminder: ReminderItem)

    @Delete
    suspend fun delete(reminder: ReminderItem)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE reminders SET isCompleted = :completed, completedAt = :completedAt WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean, completedAt: Long?)

    @Query("SELECT * FROM reminders WHERE albumId = :albumId LIMIT 1")
    suspend fun getReminderByAlbumId(albumId: Long): ReminderItem?

    @Query("DELETE FROM reminders WHERE albumId = :albumId")
    suspend fun deleteByAlbumId(albumId: Long)
}
