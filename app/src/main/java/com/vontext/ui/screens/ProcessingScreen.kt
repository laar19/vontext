package com.vontext.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vontext.ui.components.ProcessingHeader
import com.vontext.ui.components.SectionLabel
import com.vontext.ui.components.StageStatus
import com.vontext.ui.components.StepItem
import com.vontext.ui.components.TerminalLog
import com.vontext.ui.components.processingStage
import com.vontext.ui.theme.Error
import com.vontext.ui.theme.GreenVontext
import com.vontext.viewmodel.VideoViewModel
import com.vontext.viewmodel.VideoViewModel.ProcessingState

data class ProcessingParams(
    val videos: List<Uri>,
    val processTogether: Boolean,
    val interval: Int,
    val notes: String?,
    val whisperMode: com.vontext.processor.whisper.WhisperMode
)

@Composable
fun ProcessingScreen(
    viewModel: VideoViewModel = hiltViewModel(),
    params: ProcessingParams? = null,
    onComplete: () -> Unit,
    onBack: () -> Unit
) {
    val processingState by viewModel.processingState.collectAsState()
    
    var hasCompleted by remember { mutableStateOf(false) }

    // Iniciar procesamiento si hay parámetros
    LaunchedEffect(params) {
        if (params != null) {
            viewModel.processVideos(
                videos = params.videos,
                processTogether = params.processTogether,
                interval = params.interval,
                notes = params.notes,
                whisperMode = params.whisperMode,
                onProgress = { progress, message ->
                    // Actualizar progreso en el estado
                },
                onComplete = { result ->
                    if (result.isSuccess) {
                        hasCompleted = true
                    }
                }
            )
        }
    }

    // Navegar al completar
    LaunchedEffect(hasCompleted) {
        if (hasCompleted) {
            onComplete()
        }
    }

    // Manejar error
    LaunchedEffect(processingState) {
        if (processingState is ProcessingState.Error) {
            // Mostrar error - por ahora solo log
        }
    }
    
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header con progreso
            ProcessingHeader(
                progress = when (processingState) {
                    is ProcessingState.Processing -> (processingState as ProcessingState.Processing).progress
                    else -> 0
                },
                filename = when (processingState) {
                    is ProcessingState.Processing -> (processingState as ProcessingState.Processing).message
                    else -> "Iniciando..."
                },
                eta = when (processingState) {
                    is ProcessingState.Processing -> {
                        val remaining = 100 - (processingState as ProcessingState.Processing).progress
                        "${remaining / 10} min"
                    }
                    else -> null
                }
            )
            
            // Contenido scrolleable
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
            ) {
                // Etapas
                SectionLabel("Etapas")

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.large,
                    shadowElevation = 2.dp
                ) {
                    Column {
                        StepItem(
                            stage = processingStage(
                                type = com.vontext.ui.components.ProcessingStageType.Carga,
                                status = when {
                                    processingState is ProcessingState.Processing && (processingState as ProcessingState.Processing).progress >= 5 -> 
                                        com.vontext.ui.components.StageStatus.Completed
                                    else -> com.vontext.ui.components.StageStatus.Pending
                                }
                            )
                        )

                        androidx.compose.material3.Divider(
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        StepItem(
                            stage = processingStage(
                                type = com.vontext.ui.components.ProcessingStageType.Frames,
                                detail = when {
                                    processingState is ProcessingState.Processing && (processingState as ProcessingState.Processing).progress >= 15 -> "47 frames · 5s/frame"
                                    else -> null
                                },
                                status = when {
                                    processingState is ProcessingState.Processing && (processingState as ProcessingState.Processing).progress >= 30 -> 
                                        com.vontext.ui.components.StageStatus.Completed
                                    processingState is ProcessingState.Processing && (processingState as ProcessingState.Processing).progress >= 15 -> 
                                        com.vontext.ui.components.StageStatus.Active
                                    else -> com.vontext.ui.components.StageStatus.Pending
                                }
                            )
                        )

                        androidx.compose.material3.Divider(
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        StepItem(
                            stage = processingStage(
                                type = com.vontext.ui.components.ProcessingStageType.Transcripcion,
                                detail = when {
                                    processingState is ProcessingState.Processing && (processingState as ProcessingState.Processing).progress >= 40 -> {
                                        val isLocal = params?.whisperMode?.name?.startsWith("LOCAL") == true
                                        if (isLocal) "Whisper Local (${params?.whisperMode?.name?.removePrefix("LOCAL_")?.lowercase() ?: "small"}) · procesando segmento..."
                                        else "Whisper API (${params?.whisperMode?.name?.removePrefix("REMOTE_")?.lowercase() ?: "openai"}) · procesando..."
                                    }
                                    else -> null
                                },
                                status = when {
                                    processingState is ProcessingState.Processing && (processingState as ProcessingState.Processing).progress >= 70 -> 
                                        com.vontext.ui.components.StageStatus.Completed
                                    processingState is ProcessingState.Processing && (processingState as ProcessingState.Processing).progress >= 40 -> 
                                        com.vontext.ui.components.StageStatus.Active
                                    else -> com.vontext.ui.components.StageStatus.Pending
                                }
                            )
                        )

                        androidx.compose.material3.Divider(
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        StepItem(
                            stage = processingStage(
                                type = com.vontext.ui.components.ProcessingStageType.Pdf,
                                status = when {
                                    processingState is ProcessingState.Processing && (processingState as ProcessingState.Processing).progress >= 90 -> 
                                        com.vontext.ui.components.StageStatus.Completed
                                    processingState is ProcessingState.Processing && (processingState as ProcessingState.Processing).progress >= 70 -> 
                                        com.vontext.ui.components.StageStatus.Pending
                                    else -> com.vontext.ui.components.StageStatus.Pending
                                }
                            )
                        )

                        androidx.compose.material3.Divider(
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        StepItem(
                            stage = processingStage(
                                type = com.vontext.ui.components.ProcessingStageType.Zip,
                                status = when {
                                    processingState is ProcessingState.Processing && (processingState as ProcessingState.Processing).progress >= 100 -> 
                                        com.vontext.ui.components.StageStatus.Completed
                                    else -> com.vontext.ui.components.StageStatus.Pending
                                }
                            )
                        )
                    }
                }

                // Log en vivo
                SectionLabel("Log en vivo")

                TerminalLog(
                    logs = when (processingState) {
                        is ProcessingState.Processing -> (processingState as ProcessingState.Processing).logs
                        else -> emptyList()
                    }
                )

                // Cola dinámica con nombres reales
                SectionLabel("Cola")

                val videoNames = params?.videos?.map { uri ->
                    uri.path?.substringAfterLast('/') ?: "Video ${uri.hashCode()}"
                } ?: emptyList()

                if (videoNames.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column {
                            videoNames.forEachIndexed { index, name ->
                                val isActive = index == 0
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isActive) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (isActive) GreenVontext else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = if (isActive) "En proceso" else "En cola",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isActive) GreenVontext else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                if (index < videoNames.size - 1) {
                                    androidx.compose.material3.Divider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                        modifier = Modifier.padding(start = 48.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Botón cancelar
                Button(
                    onClick = { /* TODO: Cancelar procesamiento */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Error)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cancelar procesamiento",
                        style = MaterialTheme.typography.labelLarge,
                        color = Error
                    )
                }

                // Spacer final
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
