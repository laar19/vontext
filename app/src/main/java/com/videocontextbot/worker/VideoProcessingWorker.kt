package com.videocontextbot.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.videocontextbot.data.repository.JobRepository
import com.videocontextbot.domain.model.JobStatus
import com.videocontextbot.domain.model.ProcessingConfig
import com.videocontextbot.processor.VideoProcessor
import com.videocontextbot.processor.whisper.WhisperMode
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File

@HiltWorker
class VideoProcessingWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val videoProcessor: VideoProcessor,
    private val jobRepository: JobRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val jobId = inputData.getString("job_id") ?: return Result.failure()
        val videoPath = inputData.getString("video_path") ?: return Result.failure()
        val whisperModeName = inputData.getString("whisper_mode") ?: "LOCAL_SMALL"
        val frameInterval = inputData.getInt("frame_interval", 0)

        val whisperMode = try {
            WhisperMode.valueOf(whisperModeName)
        } catch (_: Exception) {
            WhisperMode.LOCAL_SMALL
        }

        val config = ProcessingConfig(
            frameInterval = frameInterval,
            whisperMode = whisperMode
        )

        val outputDir = File(inputData.getString("output_dir") ?: return Result.failure())

        return try {
            val result = videoProcessor.process(
                videoFile = File(videoPath),
                jobId = jobId,
                config = config,
                outputDir = outputDir,
                progressCallback = { progress, message ->
                    jobRepository.updateProgress(jobId, progress, message)
                    setProgress(workDataOf("progress" to progress, "message" to message))
                }
            )

            result.fold(
                onSuccess = {
                    jobRepository.updateStatus(jobId, JobStatus.COMPLETED)
                    Result.success()
                },
                onFailure = { error ->
                    jobRepository.updateError(jobId, JobStatus.FAILED, error.message)
                    Result.failure()
                }
            )
        } catch (e: kotlinx.coroutines.CancellationException) {
            jobRepository.updateStatus(jobId, JobStatus.CANCELLED)
            throw e
        } catch (e: Exception) {
            jobRepository.updateError(jobId, JobStatus.FAILED, e.message)
            Result.failure()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            NOTIFICATION_ID,
            android.app.Notification.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Procesando video")
                .setContentText("Iniciando...")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setOngoing(true)
                .build()
        )
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val NOTIFICATION_CHANNEL_ID = "processing_channel"
    }
}
