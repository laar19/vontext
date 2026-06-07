package com.vontext.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vontext.data.local.preferences.SettingsRepository
import com.vontext.data.repository.JobRepository
import com.vontext.domain.model.ProcessingConfig
import com.vontext.domain.usecase.ProcessVideoUseCase
import com.vontext.processor.whisper.LocalWhisperTranscriber
import com.vontext.processor.whisper.ModelDownloader
import com.vontext.processor.whisper.WhisperModel
import com.vontext.processor.whisper.WhisperMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class DownloadProgress(val downloaded: Long, val total: Long) {
    val percent: Int = if (total > 0) ((downloaded * 100) / total).toInt() else 0
}

@HiltViewModel
class VideoViewModel @Inject constructor(
    private val processVideoUseCase: ProcessVideoUseCase,
    private val jobRepository: JobRepository,
    private val settingsRepository: SettingsRepository,
    private val localWhisperTranscriber: LocalWhisperTranscriber,
    private val modelDownloader: ModelDownloader
) : ViewModel() {

    private val _processingState = MutableStateFlow<ProcessingState>(ProcessingState.Idle)
    val processingState: StateFlow<ProcessingState> = _processingState.asStateFlow()

    private val _whisperMode = MutableStateFlow(WhisperMode.LOCAL_SMALL)
    val whisperMode: StateFlow<WhisperMode> = _whisperMode.asStateFlow()

    val settings = settingsRepository.settings

    init {
        viewModelScope.launch {
            settings.collect { settings ->
                settings?.let {
                    _whisperMode.value = it.defaultWhisperMode
                }
            }
        }
    }

    val allJobs = jobRepository.getAllJobs()

    fun updateWhisperMode(mode: WhisperMode) {
        _whisperMode.value = mode
        viewModelScope.launch {
            settingsRepository.updateWhisperMode(mode)
        }
    }

    private val _modelDownloadProgress = MutableStateFlow<DownloadProgress?>(null)
    val modelDownloadProgress: StateFlow<DownloadProgress?> = _modelDownloadProgress.asStateFlow()

    private val _isDownloadingModel = MutableStateFlow(false)
    val isDownloadingModel: StateFlow<Boolean> = _isDownloadingModel.asStateFlow()

    private val _isModelDownloaded = MutableStateFlow(localWhisperTranscriber.isModelDownloaded(WhisperModel.SMALL))
    val isModelDownloaded: StateFlow<Boolean> = _isModelDownloaded.asStateFlow()

    private var downloadJob: kotlinx.coroutines.Job? = null

    fun checkModelDownloadStatus() {
        _isModelDownloaded.value = localWhisperTranscriber.isModelDownloaded(WhisperModel.SMALL)
    }

    fun downloadModel() {
        downloadJob?.cancel()
        downloadJob = viewModelScope.launch {
            _isDownloadingModel.value = true
            modelDownloader.downloadModel(
                model = WhisperModel.SMALL,
                progressCallback = { downloaded, total ->
                    _modelDownloadProgress.value = DownloadProgress(downloaded, total)
                }
            ).onSuccess {
                _isDownloadingModel.value = false
                _modelDownloadProgress.value = null
                _isModelDownloaded.value = true
            }.onFailure { error ->
                _isDownloadingModel.value = false
                _modelDownloadProgress.value = null
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        _isDownloadingModel.value = false
        _modelDownloadProgress.value = null
    }

    fun processVideos(
        videos: List<Uri>,
        processTogether: Boolean,
        interval: Int,
        notes: String?,
        whisperMode: WhisperMode = _whisperMode.value,
        onProgress: (Int, String) -> Unit,
        onComplete: (Result<String>) -> Unit
    ) {
        viewModelScope.launch {
            val logs = mutableListOf<String>()

            fun updateState(progress: Int, message: String) {
                _processingState.value = ProcessingState.Processing(
                    progress = progress,
                    message = message,
                    logs = logs.toList()
                )
            }

            updateState(0, "Iniciando...")

            if (videos.isEmpty()) {
                _processingState.value = ProcessingState.Idle
                onComplete(Result.failure(IllegalStateException("No hay videos para procesar")))
                return@launch
            }

            try {
                val config = ProcessingConfig(
                    frameInterval = interval,
                    whisperMode = whisperMode,
                    additionalNotes = notes
                )

                if (processTogether && videos.size > 1) {
                    processSingleVideo(videos.first(), config, onProgress, onComplete)
                } else {
                    val totalVideos = videos.size
                    val results = mutableListOf<Result<String>>()

                    for ((index, video) in videos.withIndex()) {
                        val baseProgress = (index * 100) / totalVideos
                        logs.add("Procesando video ${index + 1} de $totalVideos...")
                        updateState(baseProgress, "Procesando video ${index + 1} de $totalVideos")

                        val result = processSingleVideoSync(video, config) { progress, message ->
                            val overallProgress = baseProgress + (progress / totalVideos)
                            if (message.isNotBlank()) {
                                logs.add(message)
                                if (logs.size > 50) logs.removeAt(0)
                            }
                            updateState(overallProgress, message)
                            onProgress(overallProgress, message)
                        }
                        results.add(result)
                    }

                    val lastSuccess = results.lastOrNull { it.isSuccess }
                    if (lastSuccess != null) {
                        logs.add("Procesamiento completado")
                        _processingState.value = ProcessingState.Completed
                        onComplete(lastSuccess)
                    } else {
                        _processingState.value = ProcessingState.Idle
                        onComplete(Result.failure(IllegalStateException("Todos los videos fallaron")))
                    }
                }
            } catch (e: Exception) {
                _processingState.value = ProcessingState.Idle
                onComplete(Result.failure(e))
            }
        }
    }

    private suspend fun processSingleVideoSync(
        video: Uri,
        config: ProcessingConfig,
        onProgress: (Int, String) -> Unit
    ): Result<String> {
        return processVideoUseCase(
            videoUri = video,
            config = config,
            progressCallback = onProgress
        ).onSuccess { jobId ->
            cleanupTempFiles(video)
        }
    }

    private fun processSingleVideo(
        video: Uri,
        config: ProcessingConfig,
        onProgress: (Int, String) -> Unit,
        onComplete: (Result<String>) -> Unit
    ) {
        viewModelScope.launch {
            val result = processVideoUseCase(
                videoUri = video,
                config = config,
                progressCallback = onProgress
            )
            
            result.fold(
                onSuccess = { jobId ->
                    cleanupTempFiles(video)
                    _processingState.value = ProcessingState.Completed
                    onComplete(Result.success(jobId))
                },
                onFailure = { error ->
                    _processingState.value = ProcessingState.Idle
                    onComplete(Result.failure(error))
                }
            )
        }
    }

    private fun cleanupTempFiles(videoUri: Uri) {
        // Los archivos temporales se limpian automáticamente al ser cacheDir
        // Pero podemos forzar limpieza de archivos antiguos
        viewModelScope.launch {
            try {
                val cacheDir = File("/data/data/com.vontext/cache")
                val inputDir = File(cacheDir, "input")
                if (inputDir.exists()) {
                    inputDir.deleteRecursively()
                }
            } catch (e: Exception) {
                // Ignorar errores de limpieza
            }
        }
    }

    fun getJob(jobId: String) = jobRepository.getJobById(jobId)

    fun cancelProcessing() {
        _processingState.value = ProcessingState.Idle
    }

    sealed class ProcessingState {
        object Idle : ProcessingState()
        data class Processing(
            val progress: Int,
            val message: String,
            val logs: List<String>
        ) : ProcessingState()
        object Completed : ProcessingState()
        data class Error(val message: String) : ProcessingState()
    }
}
