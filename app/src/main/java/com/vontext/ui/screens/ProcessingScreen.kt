package com.vontext.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

@Composable
fun ProcessingScreen(
    viewModel: VideoViewModel = hiltViewModel(),
    onComplete: () -> Unit,
    onBack: () -> Unit
) {
    val processingState by viewModel.processingState.collectAsState()
    
    // Observar completado
    LaunchedEffect(processingState) {
        if (processingState is ProcessingState.Completed) {
            onComplete()
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
            ) {
                // Etapas
                item {
                    SectionLabel("Etapas")
                }
                
                item {
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
                                        processingState is ProcessingState.Processing && (processingState as ProcessingState.Processing).progress >= 40 -> "Whisper API · segmento 8/12..."
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
                }
                
                // Log en vivo
                item {
                    SectionLabel("Log en vivo")
                }
                
                item {
                    TerminalLog(
                        logs = when (processingState) {
                            is ProcessingState.Processing -> (processingState as ProcessingState.Processing).logs
                            else -> emptyList()
                        }
                    )
                }
                
                // Cola
                item {
                    SectionLabel("Cola")
                }
                
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.large,
                        shadowElevation = 2.dp
                    ) {
                        Column {
                            // Video actual
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RadioButtonChecked,
                                    contentDescription = null,
                                    tint = GreenVontext,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "video_actual.mp4",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "En proceso",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GreenVontext,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            // Videos en cola (ejemplo estático)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "video_pendiente_1.mp4",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "En cola",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                // Botón cancelar
                item {
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
                }
                
                // Spacer final
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
