package com.vontext.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vontext.data.local.preferences.SettingsRepository
import com.vontext.data.repository.JobRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val jobRepository: JobRepository
) : ViewModel() {

    val settings = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun updateApiKey(apiKey: String) {
        viewModelScope.launch {
            settingsRepository.updateApiKey(apiKey)
        }
    }

    fun updateEndpoint(endpoint: String) {
        viewModelScope.launch {
            settingsRepository.updateBaseUrl(endpoint)
        }
    }

    fun updateModel(model: String) {
        viewModelScope.launch {
            settingsRepository.updateWhisperModel(model)
        }
    }

    fun updateLanguage(language: String) {
        viewModelScope.launch {
            settingsRepository.updateLanguage(language)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            jobRepository.getAllJobs().collect { jobs ->
                jobs.forEach { job ->
                    jobRepository.deleteJob(job.jobId)
                }
            }
        }
    }
}
