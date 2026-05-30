package com.videocontextbot.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videocontextbot.data.repository.JobRepository
import com.videocontextbot.domain.model.Job
import com.videocontextbot.domain.model.ProcessingConfig
import com.videocontextbot.domain.usecase.ProcessVideoUseCase
import com.videocontextbot.processor.whisper.WhisperMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProcessingUiState(
    val progress: Int = 0,
    val message: String = "",
    val logs: List<String> = emptyList(),
    val status: ProcessingStatus = ProcessingStatus.IDLE,
    val jobId: String? = null
)

enum class ProcessingStatus {
    IDLE,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED
}

@HiltViewModel
class VideoViewModel @Inject constructor(
    private val processVideoUseCase: ProcessVideoUseCase,
    val jobRepository: JobRepository
) : ViewModel() {

    private val _processingState = MutableStateFlow(ProcessingUiState())
    val processingState: StateFlow<ProcessingUiState> = _processingState.asStateFlow()

    val allJobs = jobRepository.getAllJobs()

    fun startProcessing(uri: Uri, notes: String?, frameInterval: Int) {
        viewModelScope.launch {
            _processingState.value = ProcessingUiState(
                status = ProcessingStatus.PROCESSING,
                logs = listOf("Iniciando procesamiento...")
            )

            val config = ProcessingConfig(
                frameInterval = frameInterval,
                whisperMode = WhisperMode.LOCAL_SMALL,
                additionalNotes = notes
            )

            val result = processVideoUseCase(
                videoUri = uri,
                config = config,
                progressCallback = { progress, message ->
                    _processingState.value = _processingState.value.copy(
                        progress = progress,
                        message = message,
                        logs = _processingState.value.logs + message
                    )
                }
            )

            result.fold(
                onSuccess = { jobId ->
                    _processingState.value = _processingState.value.copy(
                        status = ProcessingStatus.COMPLETED,
                        jobId = jobId,
                        progress = 100,
                        message = "Procesamiento completado",
                        logs = _processingState.value.logs + "Procesamiento completado"
                    )
                },
                onFailure = { error ->
                    _processingState.value = _processingState.value.copy(
                        status = ProcessingStatus.FAILED,
                        message = error.message ?: "Error desconocido",
                        logs = _processingState.value.logs + "Error: ${error.message}"
                    )
                }
            )
        }
    }

    fun getJob(jobId: String) = jobRepository.getJobById(jobId)

    fun cancelProcessing(jobId: String) {
        viewModelScope.launch {
            _processingState.value = _processingState.value.copy(
                status = ProcessingStatus.CANCELLED,
                message = "Procesamiento cancelado",
                logs = _processingState.value.logs + "Procesamiento cancelado por el usuario"
            )
        }
    }
}
