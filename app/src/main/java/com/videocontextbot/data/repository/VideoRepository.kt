package com.videocontextbot.data.repository

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun resolveUriToFile(uri: Uri): File {
        val jobId = UUID.randomUUID().toString()
        val inputDir = File(context.cacheDir, "input/$jobId").apply { mkdirs() }
        val outputFile = File(inputDir, "video.mp4")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(outputFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("No se pudo abrir la URI: $uri")
        return outputFile
    }
}
