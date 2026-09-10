package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.data.model.AlbumItem
import com.example.data.model.MediaItem
import com.example.data.model.Priority
import com.example.data.model.ReminderItem
import com.example.data.model.RepeatMode

class Converters {
    @TypeConverter
    fun fromPriority(priority: Priority?): String = priority?.name ?: Priority.MEDIUM.name

    @TypeConverter
    fun toPriority(value: String?): Priority =
        value?.let { Priority.fromString(it) } ?: Priority.MEDIUM

    @TypeConverter
    fun fromRepeatMode(repeatMode: RepeatMode?): String = repeatMode?.name ?: RepeatMode.NONE.name

    @TypeConverter
    fun toRepeatMode(value: String?): RepeatMode =
        value?.let { RepeatMode.fromString(it) } ?: RepeatMode.NONE
}

@Database(
    entities = [ReminderItem::class, AlbumItem::class, MediaItem::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun reminderDao(): ReminderDao
    abstract fun albumDao(): AlbumDao
    abstract fun mediaDao(): MediaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "yf_reminder_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
