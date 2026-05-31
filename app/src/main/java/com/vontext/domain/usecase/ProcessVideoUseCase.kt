package com.vontext.domain.usecase

import com.vontext.data.repository.JobRepository
import com.vontext.data.repository.VideoRepository
import com.vontext.domain.model.Job
import com.vontext.domain.model.JobStatus
import com.vontext.domain.model.ProcessingConfig
import com.vontext.processor.VideoProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

class ProcessVideoUseCase @Inject constructor(
    private val videoRepository: VideoRepository,
    private val videoProcessor: VideoProcessor,
    private val jobRepository: JobRepository
) {
    suspend operator fun invoke(
        videoUri: android.net.Uri,
        config: ProcessingConfig,
        progressCallback: ((Int, String) -> Unit)? = null
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val videoFile = videoRepository.resolveUriToFile(videoUri)
                val jobId = UUID.randomUUID().toString()

                val jobDir = File(videoFile.parentFile, jobId).apply { mkdirs() }

                val job = Job(
                    jobId = jobId,
                    status = JobStatus.PENDING,
                    videoPath = videoFile.absolutePath,
                    videoFilename = videoFile.name,
                    hasAudio = null,
                    videoDuration = null,
                    additionalNotes = config.additionalNotes,
                    progress = 0,
                    progressMessage = "Iniciando...",
                    errorMessage = null,
                    createdAt = System.currentTimeMillis(),
                    startedAt = null,
                    completedAt = null,
                    outputFolder = jobDir.absolutePath,
                    pdfPath = null,
                    zipPath = null,
                    frameInterval = config.frameInterval,
                    whisperMode = config.whisperMode.name,
                    transcriptionSegments = null
                )
                jobRepository.insertJob(job)
                jobRepository.updateStatus(jobId, JobStatus.ANALYZING)

                val result = videoProcessor.process(
                    videoFile = videoFile,
                    jobId = jobId,
                    config = config,
                    outputDir = jobDir,
                    progressCallback = progressCallback
                )

                result.fold(
                    onSuccess = { zipFile ->
                        jobRepository.updateStatus(jobId, JobStatus.COMPLETED)
                        jobRepository.updateProgress(jobId, 100, "Completado")
                        Result.success(jobId)
                    },
                    onFailure = { error ->
                        jobRepository.updateError(jobId, JobStatus.FAILED, error.message)
                        Result.failure(error)
                    }
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
