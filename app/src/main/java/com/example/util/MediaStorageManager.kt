package com.example.util

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object MediaStorageManager {

    suspend fun saveMediaToInternalStorage(
        context: Context,
        albumId: Long,
        uri: Uri
    ): Pair<String, Boolean>? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: ""
            val isVideo = mimeType.startsWith("video", ignoreCase = true) ||
                    uri.toString().contains("video", ignoreCase = true)

            val extension = if (mimeType.isNotEmpty()) {
                MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: if (isVideo) "mp4" else "jpg"
            } else {
                if (isVideo) "mp4" else "jpg"
            }

            val albumDir = File(context.filesDir, "albums/$albumId")
            if (!albumDir.exists()) {
                albumDir.mkdirs()
            }

            val fileName = "media_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.$extension"
            val destFile = File(albumDir, fileName)

            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            if (destFile.exists() && destFile.length() > 0) {
                Pair(destFile.absolutePath, isVideo)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun deleteFile(filePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            } else {
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteAlbumDirectory(context: Context, albumId: Long) = withContext(Dispatchers.IO) {
        try {
            val albumDir = File(context.filesDir, "albums/$albumId")
            if (albumDir.exists()) {
                albumDir.deleteRecursively()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
