package com.vontext.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vontext.data.local.preferences.SettingsRepository
import com.vontext.data.repository.JobRepository
import com.vontext.domain.model.ProcessingConfig
import com.vontext.domain.usecase.ProcessVideoUseCase
import com.vontext.processor.whisper.WhisperMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class VideoViewModel @Inject constructor(
    private val processVideoUseCase: ProcessVideoUseCase,
    private val jobRepository: JobRepository,
    private val settingsRepository: SettingsRepository
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
            _processingState.value = ProcessingState.Processing(0, "Iniciando...", emptyList())
            
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
                    // TODO: Implementar concatenación de videos
                    // Por ahora procesamos el primer video
                    processSingleVideo(videos.first(), config, onProgress, onComplete)
                } else {
                    // Procesar cada video por separado
                    val results = mutableListOf<Result<String>>()
                    for ((index, video) in videos.withIndex()) {
                        _processingState.value = ProcessingState.Processing(
                            progress = (index * 100) / videos.size,
                            message = "Procesando video ${index + 1} de ${videos.size}",
                            logs = listOf("Procesando video ${index + 1}...")
                        )
                        
                        val result = processSingleVideoSync(video, config, onProgress)
                        results.add(result)
                    }
                    
                    // Completar con el último jobId exitoso
                    val lastSuccess = results.lastOrNull { it.isSuccess }
                    if (lastSuccess != null) {
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
