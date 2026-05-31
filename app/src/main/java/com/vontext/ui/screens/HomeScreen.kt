package com.vontext.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.outlined.CropFree
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vontext.processor.whisper.WhisperMode
import com.vontext.viewmodel.VideoViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: VideoViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val selectedVideos = remember { mutableStateListOf<Uri>() }
    var processTogether by remember { mutableStateOf(true) }
    var interval by remember { mutableIntStateOf(5) }
    var notes by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var progressMessage by remember { mutableStateOf("") }
    var logs by remember { mutableStateOf<List<String>>(emptyList()) }
    var showResults by remember { mutableStateOf(false) }
    var pdfPath by remember { mutableStateOf<String?>(null) }
    var zipPath by remember { mutableStateOf<String?>(null) }
    var showWhisperModeDialog by remember { mutableStateOf(false) }
    var pendingVideos by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var pendingProcessTogether by remember { mutableStateOf(true) }
    var pendingInterval by remember { mutableIntStateOf(5) }
    var pendingNotes by remember { mutableStateOf<String?>(null) }

    val settings by viewModel.settings.collectAsState(initial = null)

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { 
            if (it !in selectedVideos) {
                selectedVideos.add(it)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // Hero Banner
        item {
            HeroBanner()
        }

        // Body
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Upload zone
                Text(
                    text = "Video",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                UploadZone(
                    onPickVideo = { videoPicker.launch("video/*") }
                )

                // Selected videos card
                if (selectedVideos.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    SelectedVideosCard(
                        videos = selectedVideos,
                        onRemoveVideo = { index -> selectedVideos.removeAt(index) },
                        onClearAll = { selectedVideos.clear() },
                        onMoveUp = { idx ->
                            if (idx > 0) {
                                val item = selectedVideos.removeAt(idx)
                                selectedVideos.add(idx - 1, item)
                            }
                        },
                        onMoveDown = { idx ->
                            if (idx < selectedVideos.size - 1) {
                                val item = selectedVideos.removeAt(idx)
                                selectedVideos.add(idx + 1, item)
                            }
                        },
                        processTogether = processTogether,
                        onProcessTogetherChange = { processTogether = it }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Interval configuration
                Text(
                    text = "Configuración",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                IntervalStepper(
                    interval = interval,
                    onIntervalChange = { interval = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Notes
                NotesTextArea(
                    notes = notes,
                    onNotesChange = { notes = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Process FAB Extended
                ExtendedFloatingActionButton(
                    onClick = {
                        if (selectedVideos.isNotEmpty() && !isProcessing) {
                            pendingVideos = selectedVideos.toList()
                            pendingProcessTogether = processTogether
                            pendingInterval = interval
                            pendingNotes = notes.ifBlank { null }
                            showWhisperModeDialog = true
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RocketLaunch,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (selectedVideos.size > 1) "Procesar ${selectedVideos.size} Videos" else "Procesar Video",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Progress section (during processing)
                if (isProcessing) {
                    ProgressSection(
                        progress = progress,
                        message = progressMessage,
                        logs = logs,
                        onCancel = {
                            viewModel.cancelProcessing()
                            isProcessing = false
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Results section (after completion)
                if (showResults && !isProcessing) {
                    ResultsSection(
                        pdfPath = pdfPath,
                        zipPath = zipPath
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Info banner
                InfoBanner()
            }
        }
    }

    // Whisper Mode Dialog
    if (showWhisperModeDialog) {
        val currentMode = viewModel.whisperMode.collectAsState(initial = WhisperMode.LOCAL_SMALL).value
        val hasRemoteConfigured = settings?.openaiApiKey != null
        
        AlertDialog(
            onDismissRequest = { showWhisperModeDialog = false },
            title = {
                Text(text = "Seleccionar modo de transcripción")
            },
            text = {
                Column {
                    var selectedMode by remember { mutableStateOf(currentMode) }
                    
                    // Local mode option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedMode = WhisperMode.LOCAL_SMALL }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = selectedMode == WhisperMode.LOCAL_SMALL,
                            onClick = { selectedMode = WhisperMode.LOCAL_SMALL }
                        )
                        Column(
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                text = "🏠 Local (whisper.cpp - Small)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Totalmente offline, ~466 MB, ~5 min de procesamiento",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // Remote mode option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                if (hasRemoteConfigured) {
                                    selectedMode = WhisperMode.REMOTE_OPENAI 
                                }
                            }
                            .padding(vertical = 12.dp)
                            .alpha(if (hasRemoteConfigured) 1f else 0.5f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = selectedMode == WhisperMode.REMOTE_OPENAI,
                            onClick = { selectedMode = WhisperMode.REMOTE_OPENAI },
                            enabled = hasRemoteConfigured
                        )
                        Column(
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                text = "☁️ Remoto (OpenAI-compatible)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (hasRemoteConfigured) 
                                    "Requiere API key, ~1 min de procesamiento" 
                                else 
                                    "Configura API key en Ajustes para habilitar",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // Store selected mode and start processing
                    androidx.compose.runtime.LaunchedEffect(selectedMode) {
                        viewModel.updateWhisperMode(selectedMode)
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        showWhisperModeDialog = false
                        isProcessing = true
                        showResults = false
                        scope.launch {
                            viewModel.processVideos(
                                videos = pendingVideos,
                                processTogether = pendingProcessTogether,
                                interval = pendingInterval,
                                notes = pendingNotes,
                                whisperMode = viewModel.whisperMode.value,
                                onProgress = { p, msg ->
                                    progress = p
                                    progressMessage = msg
                                    logs = logs + msg
                                },
                                onComplete = { result ->
                                    isProcessing = false
                                    showResults = true
                                    result.fold(
                                        onSuccess = { jobId ->
                                            scope.launch {
                                                viewModel.getJob(jobId).collect { job ->
                                                    job?.let {
                                                        pdfPath = it.pdfPath
                                                        zipPath = it.zipPath
                                                    }
                                                }
                                            }
                                        },
                                        onFailure = { error ->
                                            logs = logs + "Error: ${error.message}"
                                        }
                                    )
                                }
                            )
                        }
                    }
                ) {
                    Text("Procesar")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showWhisperModeDialog = false }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun HeroBanner() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            Color(0xFF1a73e8)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VideoFile,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Vontext",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.weight(1f))
                Surface(
                    color = Color.White.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Beta",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Convierte grabaciones de pantalla en contexto PDF para agentes de IA.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.82f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HeroChip(icon = Icons.Outlined.CropFree, text = "Frames")
                HeroChip(icon = Icons.Outlined.Mic, text = "Whisper")
                HeroChip(icon = Icons.Outlined.PictureAsPdf, text = "PDF")
                HeroChip(icon = Icons.Outlined.FolderZip, text = "ZIP")
            }
        }
    }
}

@Composable
private fun HeroChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Surface(
        color = Color.White.copy(alpha = 0.15f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color.White.copy(alpha = 0.25f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.9f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun UploadZone(onPickVideo: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPickVideo),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(
            2.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.UploadFile,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(32.dp)
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Subir video",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Toca para seleccionar o arrastra aquí",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                AssistChip("MP4")
                AssistChip("MKV")
                AssistChip("AVI")
                AssistChip("MOV")
                AssistChip("WebM")
            }
            Text(
                text = "Tamaño máximo 2 GB",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AssistChip(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun SelectedVideosCard(
    videos: List<Uri>,
    onRemoveVideo: (Int) -> Unit,
    onClearAll: () -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    processTogether: Boolean,
    onProcessTogetherChange: (Boolean) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Videos seleccionados (${videos.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                if (videos.size > 1) {
                    Text(
                        text = "Limpiar todo",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.clickable(onClick = onClearAll)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            videos.forEachIndexed { index, uri ->
                VideoItem(
                    index = index,
                    totalItems = videos.size,
                    canReorder = videos.size > 1 && processTogether,
                    onRemove = { onRemoveVideo(index) },
                    onMoveUp = { onMoveUp(index) },
                    onMoveDown = { onMoveDown(index) }
                )
                if (index < videos.size - 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            if (videos.size > 1) {
                Spacer(modifier = Modifier.height(12.dp))
                ProcessModeSegmentedButton(
                    processTogether = processTogether,
                    onProcessTogetherChange = onProcessTogetherChange
                )
            }
        }
    }
}

@Composable
private fun VideoItem(
    index: Int,
    totalItems: Int,
    canReorder: Boolean,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (canReorder) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = onMoveUp,
                        enabled = index > 0,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Subir",
                            tint = if (index > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier
                                .size(16.dp)
                                .rotate(180f)
                        )
                    }
                    IconButton(
                        onClick = onMoveDown,
                        enabled = index < totalItems - 1,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Bajar",
                            tint = if (index < totalItems - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Video",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = "Video ${index + 1}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Eliminar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ProcessModeSegmentedButton(
    processTogether: Boolean,
    onProcessTogetherChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                modifier = Modifier.weight(1f),
                color = if (processTogether) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = "🔗 Juntos",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (processTogether) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onProcessTogetherChange(true) }
                        .padding(vertical = 10.dp),
                    textAlign = TextAlign.Center
                )
            }
            Surface(
                modifier = Modifier.weight(1f),
                color = if (!processTogether) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = "📎 Separado",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (!processTogether) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onProcessTogetherChange(false) }
                        .padding(vertical = 10.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun IntervalStepper(
    interval: Int,
    onIntervalChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Intervalo de captura",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = "0 = Auto / detección de escenas",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StepperButton(
                    onClick = { onIntervalChange(maxOf(0, interval - 1)) }
                ) {
                    Text("−", style = MaterialTheme.typography.titleLarge)
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$interval",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "segundos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StepperButton(
                    onClick = { onIntervalChange(interval + 1) }
                ) {
                    Text("+", style = MaterialTheme.typography.titleLarge)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IntervalChip("Auto", 0, interval, onIntervalChange)
                IntervalChip("1s", 1, interval, onIntervalChange)
                IntervalChip("2s", 2, interval, onIntervalChange)
                IntervalChip("5s", 5, interval, onIntervalChange)
                IntervalChip("9s", 9, interval, onIntervalChange)
                IntervalChip("30s", 30, interval, onIntervalChange)
            }
        }
    }
}

@Composable
private fun StepperButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.size(40.dp),
        shape = CircleShape,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline
        ),
        color = Color.Transparent
    ) {
        Box(contentAlignment = Alignment.Center) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center
            ) {
                content()
            }
        }
    }
}

@Composable
private fun IntervalChip(
    text: String,
    value: Int,
    selected: Int,
    onClick: (Int) -> Unit
) {
    val isSelected = value == selected
    Surface(
        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .clickable { onClick(value) }
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun NotesTextArea(notes: String, onNotesChange: (String) -> Unit) {
    OutlinedTextField(
        value = notes,
        onValueChange = onNotesChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Notas / logs adicionales (opcional)") },
        placeholder = { Text("Pegá logs, descripción del bug o contexto...") },
        shape = RoundedCornerShape(12.dp),
        minLines = 3,
        maxLines = 5
    )
}

@Composable
private fun ProgressSection(
    progress: Int,
    message: String,
    logs: List<String>,
    onCancel: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Procesando...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                androidx.compose.material3.TextButton(onClick = onCancel) {
                    Text("Cancelar")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = progress / 100f,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$progress% - $message",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (logs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "📋 Logs",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp)
                ) {
                    LazyColumn {
                        items(logs.size) { index ->
                            Text(
                                text = logs[index],
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                            if (index < logs.size - 1) {
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultsSection(
    pdfPath: String?,
    zipPath: String?
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "🎁 Resultados",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (pdfPath != null) {
                ResultItem(
                    icon = "📄",
                    title = "Documento PDF",
                    onClick = { /* TODO: View PDF */ }
                )
                Spacer(modifier = Modifier.height(8.dp))
                ResultItem(
                    icon = "🔗",
                    title = "Compartir PDF",
                    onClick = { /* TODO: Share PDF */ }
                )
            }
            if (zipPath != null) {
                Spacer(modifier = Modifier.height(8.dp))
                ResultItem(
                    icon = "📦",
                    title = "ZIP Completo",
                    onClick = { /* TODO: Share ZIP */ }
                )
            }
        }
    }
}

@Composable
private fun ResultItem(icon: String, title: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun InfoBanner() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Información",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                InfoItem("Tamaño máximo: 2 GB por archivo")
                InfoItem("El procesamiento puede tomar varios minutos")
                InfoItem("Usa el bot de Telegram para mayor velocidad")
            }
        }
    }
}

@Composable
private fun InfoItem(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "·",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
