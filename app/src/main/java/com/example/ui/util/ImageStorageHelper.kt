package com.example.ui.util

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ImageStorageHelper {
    suspend fun saveImageToInternalStorage(context: Context, sourceUri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val imagesDir = File(context.filesDir, "node_images")
            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
            }
            val fileName = "img_${UUID.randomUUID()}.jpg"
            val destFile = File(imagesDir, fileName)

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
