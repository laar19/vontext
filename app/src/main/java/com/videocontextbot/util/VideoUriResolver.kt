package com.videocontextbot.util

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoUriResolver @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun resolveToFile(uri: Uri, jobId: String): File {
        val inputDir = File(context.cacheDir, "input/$jobId").apply { mkdirs() }
        val extension = getExtension(uri)
        val outputFile = File(inputDir, "video$extension")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(outputFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("No se pudo leer la URI: $uri")
        return outputFile
    }

    private fun getExtension(uri: Uri): String {
        val mimeType = context.contentResolver.getType(uri)
        return when (mimeType) {
            "video/mp4" -> ".mp4"
            "video/mkv" -> ".mkv"
            "video/webm" -> ".webm"
            "video/3gpp" -> ".3gp"
            else -> ".mp4"
        }
    }
}
