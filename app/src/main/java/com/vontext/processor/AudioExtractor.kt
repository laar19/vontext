package com.vontext.processor

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioExtractor @Inject constructor() {

    data class WavHeader(
        val sampleRate: Int,
        val channels: Int,
        val bitsPerSample: Int
    )

    suspend fun extractAudio(videoFile: File, outputDir: File): File {
        return withContext(Dispatchers.IO) {
            val audioFile = File(outputDir, "audio.wav")
            extractAudioInternal(videoFile, audioFile)
            audioFile
        }
    }

    private fun extractAudioInternal(inputFile: File, outputFile: File) {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(inputFile.absolutePath)

            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = format
                    break
                }
            }

            if (audioTrackIndex == -1) {
                throw IllegalStateException("No se encontró pista de audio en el video")
            }

            extractor.selectTrack(audioTrackIndex)

            val mime = audioFormat!!.getString(MediaFormat.KEY_MIME) ?: "audio/mp4a-latm"
            val sampleRate = audioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channels = if (audioFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                audioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            } else {
                2
            }

            val codec = MediaCodec.createDecoderByType(mime)
            try {
                codec.configure(audioFormat, null, null, 0)
                codec.start()

                val bufferInfo = MediaCodec.BufferInfo()
                val pcmData = mutableListOf<Byte>()

                var sawInputEOS = false
                var sawOutputEOS = false

                while (!sawOutputEOS) {
                    if (!sawInputEOS) {
                        val inputIndex = codec.dequeueInputBuffer(10000L)
                        if (inputIndex >= 0) {
                            val inputBuffer = codec.getInputBuffer(inputIndex)!!
                            val sampleSize = extractor.readSampleData(inputBuffer, 0)
                            if (sampleSize < 0) {
                                codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                sawInputEOS = true
                            } else {
                                val presentationTime = extractor.sampleTime
                                codec.queueInputBuffer(inputIndex, 0, sampleSize, presentationTime, 0)
                                extractor.advance()
                            }
                        }
                    }

                    val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10000L)
                    if (outputIndex >= 0) {
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            sawOutputEOS = true
                        }

                        if (bufferInfo.size > 0) {
                            val outputBuffer = codec.getOutputBuffer(outputIndex)!!
                            outputBuffer.position(bufferInfo.offset)
                            outputBuffer.limit(bufferInfo.offset + bufferInfo.size)

                            val chunk = ByteArray(bufferInfo.size)
                            outputBuffer.get(chunk)
                            pcmData.addAll(chunk.toList())
                        }

                        codec.releaseOutputBuffer(outputIndex, false)
                    } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    }
                }

                writeWavFile(outputFile, pcmData.toByteArray(), WavHeader(sampleRate, channels, 16))
            } finally {
                codec.stop()
                codec.release()
            }
        } finally {
            extractor.release()
        }
    }

    private fun writeWavFile(file: File, pcmData: ByteArray, header: WavHeader) {
        val byteRate = header.sampleRate * header.channels * (header.bitsPerSample / 8)
        val blockAlign = header.channels * (header.bitsPerSample / 8)
        val dataSize = pcmData.size
        val fileSize = 36 + dataSize

        FileOutputStream(file).use { fos ->
            val headerBuf = ByteBuffer.allocate(44).apply {
                order(ByteOrder.LITTLE_ENDIAN)
                put("RIFF".toByteArray())
                putInt(fileSize)
                put("WAVE".toByteArray())
                put("fmt ".toByteArray())
                putInt(16)
                putShort(1)
                putShort(header.channels.toShort())
                putInt(header.sampleRate)
                putInt(byteRate)
                putShort(blockAlign.toShort())
                putShort(header.bitsPerSample.toShort())
                put("data".toByteArray())
                putInt(dataSize)
            }
            fos.write(headerBuf.array())
            fos.write(pcmData)
        }
    }
}
