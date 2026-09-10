package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "albums")
data class AlbumItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val coverUri: String? = null,
    val colorHex: Long = 0xFF6750A4,
    val reminderTimestamp: Long? = null,
    val reminderId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "media_items",
    foreignKeys = [
        ForeignKey(
            entity = AlbumItem::class,
            parentColumns = ["id"],
            childColumns = ["albumId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["albumId"])]
)
data class MediaItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val albumId: Long,
    val uriString: String,
    val isVideo: Boolean = false,
    val caption: String = "",
    val addedAt: Long = System.currentTimeMillis()
)

data class AlbumWithDetails(
    val album: AlbumItem,
    val mediaCount: Int = 0,
    val imageCount: Int = 0,
    val videoCount: Int = 0,
    val latestMediaUri: String? = null
)
