package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class Priority(val label: String, val level: Int) {
    LOW("Low", 1),
    MEDIUM("Medium", 2),
    HIGH("High", 3);

    companion object {
        fun fromString(value: String): Priority =
            entries.find { it.name.equals(value, ignoreCase = true) } ?: MEDIUM
    }
}

enum class RepeatMode(val label: String) {
    NONE("Never"),
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly");

    companion object {
        fun fromString(value: String): RepeatMode =
            entries.find { it.name.equals(value, ignoreCase = true) } ?: NONE
    }
}

enum class ReminderCategory(val displayName: String, val colorHex: Long) {
    GENERAL("General", 0xFF6750A4),
    WORK("Work", 0xFF0288D1),
    PERSONAL("Personal", 0xFF7B1FA2),
    HEALTH("Health", 0xFF2E7D32),
    FINANCE("Finance", 0xFFE65100),
    SHOPPING("Shopping", 0xFFC2185B),
    STUDY("Study", 0xFF00796B),
    ALBUMS("Albums", 0xFFD81B60);

    companion object {
        fun fromString(name: String): ReminderCategory =
            entries.find { it.displayName.equals(name, ignoreCase = true) || it.name.equals(name, ignoreCase = true) } ?: GENERAL
    }
}

@Entity(tableName = "reminders")
data class ReminderItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val dueTimestamp: Long, // Epoch millis
    val category: String = ReminderCategory.GENERAL.displayName,
    val priority: Priority = Priority.MEDIUM,
    val repeatMode: RepeatMode = RepeatMode.NONE,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val albumId: Long? = null,
    val albumTitle: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val isOverdue: Boolean
        get() = !isCompleted && dueTimestamp < System.currentTimeMillis()
}
