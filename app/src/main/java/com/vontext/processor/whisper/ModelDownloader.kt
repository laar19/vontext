package com.vontext.processor.whisper

import android.content.Context
import com.vontext.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelDownloader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(Constants.MODEL_DOWNLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    suspend fun downloadModel(
        model: WhisperModel,
        progressCallback: ((Long, Long) -> Unit)? = null
    ): Result<File> {
        val modelDir = File(context.filesDir, "whisper_models").apply { mkdirs() }
        val modelFile = File(modelDir, "${model.filename}.bin")

        if (modelFile.exists() && modelFile.length() > 0) {
            return Result.success(modelFile)
        }

        val url = getModelUrl(model)

        return withContext(Dispatchers.IO) {
            retryWithBackoff(Constants.MODEL_DOWNLOAD_RETRIES) { attempt ->
                val tempFile = File(modelDir, "${model.filename}.bin.downloading")

                val rangeStart = if (tempFile.exists()) tempFile.length() else 0L

                val request = Request.Builder()
                    .url(url)
                    .apply {
                        if (rangeStart > 0) {
                            header("Range", "bytes=$rangeStart-")
                        }
                    }
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful && response.code != 206) {
                    tempFile.delete()
                    return@retryWithBackoff Result.failure(
                        Exception("Error de descarga: ${response.code}")
                    )
                }

                val body = response.body
                if (body == null) {
                    tempFile.delete()
                    return@retryWithBackoff Result.failure(Exception("Sin cuerpo de respuesta"))
                }

                val modelTotal = model.sizeMb * 1024L * 1024L
                val contentLen = body.contentLength()
                val totalBytes = if (rangeStart > 0L) {
                    modelTotal.coerceAtLeast(rangeStart)
                } else if (contentLen > 0L) {
                    contentLen
                } else {
                    modelTotal
                }
                FileOutputStream(tempFile, rangeStart > 0).use { outputStream ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalRead = rangeStart

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                            totalRead += bytesRead
                            progressCallback?.invoke(totalRead, totalBytes)
                        }
                    }
                }

                if (!tempFile.renameTo(modelFile)) {
                    modelFile.delete()
                    tempFile.renameTo(modelFile)
                }

                Result.success(modelFile)
            }
        }
    }

    private suspend fun retryWithBackoff(
        maxRetries: Int,
        block: suspend (Int) -> Result<File>
    ): Result<File> {
        var lastError: Throwable? = null
        for (attempt in 1..maxRetries) {
            try {
                val result = block(attempt)
                if (result.isSuccess) return result
                lastError = result.exceptionOrNull()
            } catch (e: Exception) {
                lastError = e
            }
            if (attempt < maxRetries) {
                delay((1000L * attempt).coerceAtMost(4000L))
            }
        }
        return Result.failure(lastError ?: Exception("Descarga fallida después de $maxRetries intentos"))
    }

    fun deletePartialDownload(model: WhisperModel) {
        val modelDir = File(context.filesDir, "whisper_models")
        val tempFile = File(modelDir, "${model.filename}.bin.downloading")
        if (tempFile.exists()) {
            tempFile.delete()
        }
    }

    private fun getModelUrl(model: WhisperModel): String {
        return "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/${model.filename}.bin"
    }
}
