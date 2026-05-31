package com.vontext.processor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ZipCreator @Inject constructor() {

    suspend fun create(outputDir: File): Result<File> {
        return withContext(Dispatchers.IO) {
            try {
                val parentDir = outputDir.parentFile
                    ?: return@withContext Result.failure(
                        IllegalStateException("El directorio de salida no tiene directorio padre")
                    )
                val zipFile = File(parentDir, "${outputDir.name}.zip")

                ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                    outputDir.walkTopDown()
                        .filter { it.isFile }
                        .forEach { file ->
                            val entryName = file.relativeTo(outputDir).path
                            val entry = ZipEntry(entryName)
                            zos.putNextEntry(entry)
                            file.inputStream().use { fis ->
                                fis.copyTo(zos)
                            }
                            zos.closeEntry()
                        }
                }

                Result.success(zipFile)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
