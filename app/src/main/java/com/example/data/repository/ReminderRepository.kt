package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.ReminderDao
import com.example.data.model.ReminderItem
import com.example.receiver.ReminderAlarmScheduler
import kotlinx.coroutines.flow.Flow

class ReminderRepository(
    private val reminderDao: ReminderDao,
    private val alarmScheduler: ReminderAlarmScheduler
) {
    val allReminders: Flow<List<ReminderItem>> = reminderDao.getAllReminders()

    suspend fun getReminderById(id: Long): ReminderItem? =
        reminderDao.getReminderById(id)

    suspend fun addReminder(reminder: ReminderItem): Long {
        val id = reminderDao.insert(reminder)
        val created = reminder.copy(id = id)
        alarmScheduler.schedule(created)
        return id
    }

    suspend fun updateReminder(reminder: ReminderItem) {
        reminderDao.update(reminder)
        if (reminder.isCompleted) {
            alarmScheduler.cancel(reminder.id)
        } else {
            alarmScheduler.schedule(reminder)
        }
    }

    suspend fun deleteReminder(reminder: ReminderItem) {
        reminderDao.delete(reminder)
        alarmScheduler.cancel(reminder.id)
    }

    suspend fun toggleCompleted(reminder: ReminderItem) {
        val newStatus = !reminder.isCompleted
        val completedAt = if (newStatus) System.currentTimeMillis() else null
        val updated = reminder.copy(isCompleted = newStatus, completedAt = completedAt)
        reminderDao.update(updated)
        if (newStatus) {
            alarmScheduler.cancel(reminder.id)
        } else {
            alarmScheduler.schedule(updated)
        }
    }

    suspend fun snoozeReminder(reminder: ReminderItem, minutes: Int) {
        val snoozedDue = System.currentTimeMillis() + minutes * 60 * 1000
        val updated = reminder.copy(dueTimestamp = snoozedDue, isCompleted = false)
        reminderDao.update(updated)
        alarmScheduler.schedule(updated)
    }

    suspend fun getUpcomingPendingReminders(): List<ReminderItem> =
        reminderDao.getUpcomingPendingReminders()

    companion object {
        fun create(context: Context): ReminderRepository {
            val db = AppDatabase.getDatabase(context)
            val scheduler = ReminderAlarmScheduler(context)
            return ReminderRepository(db.reminderDao(), scheduler)
        }
    }
}
