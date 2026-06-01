package com.vontext.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileOutputStream

object PdfToImageConverter {

    /**
     * Convierte un PDF a una imagen JPG vertical larga (concatenación de todas las páginas)
     * @return El archivo JPG generado o null si falla
     */
    fun convert(context: Context, pdfPath: String): File? {
        val pdfFile = File(pdfPath)
        if (!pdfFile.exists()) return null

        val fileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val pdfRenderer = PdfRenderer(fileDescriptor)

        try {
            if (pdfRenderer.pageCount == 0) return null

            // Renderizar todas las páginas a bitmaps
            val bitmaps = mutableListOf<Bitmap>()
            for (i in 0 until pdfRenderer.pageCount) {
                val page = pdfRenderer.openPage(i)
                val bitmap = Bitmap.createBitmap(
                    page.width,
                    page.height,
                    Bitmap.Config.ARGB_8888
                )
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmaps.add(bitmap)
                page.close()
            }

            // Calcular dimensiones del bitmap final
            val maxWidth = bitmaps.maxOf { it.width }
            val totalHeight = bitmaps.sumOf { it.height }

            // Crear bitmap concatenado
            val combinedBitmap = Bitmap.createBitmap(maxWidth, totalHeight, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(combinedBitmap)
            canvas.drawColor(android.graphics.Color.WHITE)

            var currentY = 0
            for (bitmap in bitmaps) {
                // Centrar horizontalmente si las páginas tienen diferentes anchos
                val xOffset = (maxWidth - bitmap.width) / 2
                canvas.drawBitmap(bitmap, xOffset.toFloat(), currentY.toFloat(), null)
                currentY += bitmap.height
                bitmap.recycle()
            }

            // Guardar como JPG
            val outputDir = File(context.cacheDir, "pdf_images")
            outputDir.mkdirs()
            val outputFile = File(outputDir, "${pdfFile.nameWithoutExtension}_full.jpg")

            FileOutputStream(outputFile).use { out ->
                combinedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }

            combinedBitmap.recycle()

            return outputFile
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Error al convertir PDF a imagen")
            return null
        } finally {
            pdfRenderer.close()
            fileDescriptor.close()
        }
    }

    /**
     * Elimina las imágenes JPG temporales generadas
     */
    fun cleanupTempImages(context: Context) {
        try {
            val tempDir = File(context.cacheDir, "pdf_images")
            if (tempDir.exists()) {
                tempDir.deleteRecursively()
            }
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Error al limpiar imágenes temporales")
        }
    }
}
