package com.vontext.ui.screens

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vontext.processor.whisper.WhisperMode
import com.vontext.ui.components.InfoStrip
import com.vontext.ui.components.ModeChipRow
import com.vontext.ui.components.NotesField
import com.vontext.ui.components.SectionLabel
import com.vontext.ui.components.StepperBlock
import com.vontext.ui.components.VideoQueueItem
import com.vontext.ui.theme.GreenDark
import com.vontext.ui.theme.GreenLight
import com.vontext.ui.theme.GreenVontext
import com.vontext.viewmodel.SettingsViewModel
import com.vontext.viewmodel.VideoViewModel
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: VideoViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    selectedVideos: MutableList<Uri>,
    onNavigateToProcessing: (List<Uri>, Boolean, Int, String?, WhisperMode) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    var processTogether by remember { mutableStateOf(true) }
    var interval by remember { mutableIntStateOf(5) }
    var notes by remember { mutableStateOf("") }
    var showModelDialog by remember { mutableStateOf(false) }
    var pendingProcess by remember { mutableStateOf(false) }

    val settings by settingsViewModel.settings.collectAsState(initial = null)
    val isModelDownloaded by viewModel.isModelDownloaded.collectAsState(initial = false)
    val currentMode by viewModel.whisperMode.collectAsState()

    val hasApiKey = !settings?.openaiApiKey.isNullOrBlank()

    val thumbnailColors = remember { mutableMapOf<String, Color>() }
    fun getThumbnailColor(uri: Uri): Color {
        return thumbnailColors.getOrPut(uri.toString()) {
            val random = Random(uri.toString().hashCode())
            val r = random.nextInt(100, 200)
            val g = random.nextInt(150, 220)
            val b = random.nextInt(100, 180)
            Color(r, g, b)
        }
    }

    // Diálogo de selección de modelo
    if (showModelDialog) {
        ModelSelectionDialog(
            isModelDownloaded = isModelDownloaded,
            hasApiKey = hasApiKey,
            currentMode = currentMode,
            onSelectMode = { mode ->
                viewModel.updateWhisperMode(mode)
                showModelDialog = false
                if (pendingProcess) {
                    pendingProcess = false
                    onNavigateToProcessing(
                        selectedVideos.toList(),
                        processTogether,
                        interval,
                        notes.ifBlank { null },
                        mode
                    )
                }
            },
            onGoToSettings = {
                showModelDialog = false
                pendingProcess = false
                onNavigateToSettings()
            },
            onDismiss = {
                showModelDialog = false
                pendingProcess = false
            }
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = {
                    Text(
                        text = "Vontext",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (selectedVideos.isNotEmpty()) {
            item {
                SectionLabel("Videos \u00B7 ${selectedVideos.size} archivos")
            }

            items(
                items = selectedVideos,
                key = { it.toString() }
            ) { uri ->
                val random = Random(uri.toString().hashCode())
                VideoQueueItem(
                    name = uri.path?.substringAfterLast('/') ?: "Video",
                    size = "~${random.nextInt(50, 200)} MB",
                    duration = "${random.nextInt(1, 10)}:${random.nextInt(10, 60).toString().padStart(2, '0')}",
                    onRemove = { selectedVideos.remove(uri) },
                    thumbnailColor = getThumbnailColor(uri)
                )
            }

            item {
                SectionLabel("Modo de procesamiento")
                ModeChipRow(
                    processTogether = processTogether,
                    onModeChange = { processTogether = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                SectionLabel("Modelo de transcripci\u00F3n")
                ModeSelector(
                    currentMode = currentMode,
                    isModelDownloaded = isModelDownloaded,
                    hasApiKey = hasApiKey,
                    onClick = { showModelDialog = true }
                )
            }
        }

        item {
            SectionLabel("Configuraci\u00F3n")
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(
                    0.5.dp,
                    MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    StepperBlock(
                        value = interval,
                        onValueChange = { interval = it }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    androidx.compose.material3.Divider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    NotesField(
                        value = notes,
                        onValueChange = { notes = it }
                    )
                }
            }
        }

        // Botón Procesar al final
        if (selectedVideos.isNotEmpty()) {
            item {
                Button(
                    onClick = {
                        val isLocalMode = currentMode.name.startsWith("LOCAL")
                        if (isLocalMode && !isModelDownloaded) {
                            showModelDialog = true
                            pendingProcess = true
                        } else if (!isModelDownloaded && !hasApiKey) {
                            showModelDialog = true
                            pendingProcess = true
                        } else {
                            onNavigateToProcessing(
                                selectedVideos.toList(),
                                processTogether,
                                interval,
                                notes.ifBlank { null },
                                currentMode
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GreenVontext
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PlayCircle,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Procesar ${selectedVideos.size} videos",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        item {
            InfoStrip(
                items = listOf(
                    "Usa el bot\u00F3n + para seleccionar videos",
                    "Tama\u00F1o m\u00E1ximo 2 GB por archivo",
                    "El procesamiento puede tomar varios minutos"
                )
            )
        }
    }
    }
}

@Composable
private fun ModeSelector(
    currentMode: WhisperMode,
    isModelDownloaded: Boolean,
    hasApiKey: Boolean,
    onClick: () -> Unit
) {
    val label = when {
        currentMode.name.startsWith("LOCAL") -> "Local (${currentMode.name.removePrefix("LOCAL_").lowercase()})"
        currentMode.name.startsWith("REMOTE") -> "API (${currentMode.name.removePrefix("REMOTE_").lowercase()})"
        else -> "No configurado"
    }
    val subtitle = when {
        currentMode.name.startsWith("LOCAL") && !isModelDownloaded -> "No descargado - ve a Ajustes"
        isModelDownloaded && hasApiKey -> "Local + API disponibles"
        isModelDownloaded -> "Solo local disponible"
        hasApiKey -> "Solo API disponible"
        else -> "Ninguno configurado"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (currentMode.name.startsWith("LOCAL")) Icons.Default.PhoneAndroid else Icons.Default.Cloud,
                contentDescription = null,
                tint = if (isModelDownloaded || hasApiKey) GreenVontext else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Cambiar",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ModelSelectionDialog(
    isModelDownloaded: Boolean,
    hasApiKey: Boolean,
    currentMode: WhisperMode,
    onSelectMode: (WhisperMode) -> Unit,
    onGoToSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!isModelDownloaded && !hasApiKey) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Sin modelo configurado") },
            text = {
                Text("No tienes ning\u00FAn modelo de transcripci\u00F3n configurado. Descarga el modelo local o configura una API en Ajustes.")
            },
            confirmButton = {
                Button(onClick = onGoToSettings) {
                    Text("Ir a Ajustes")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Seleccionar modelo") },
            text = {
                Column {
                    if (isModelDownloaded) {
                        WhisperModeOption(
                            label = "Local (small)",
                            description = "Transcripci\u00F3n offline en el dispositivo",
                            isSelected = currentMode == WhisperMode.LOCAL_SMALL,
                            onClick = { onSelectMode(WhisperMode.LOCAL_SMALL) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    if (hasApiKey) {
                        WhisperModeOption(
                            label = "API (OpenAI)",
                            description = "Transcripci\u00F3n por API remota",
                            isSelected = currentMode == WhisperMode.REMOTE_OPENAI,
                            onClick = { onSelectMode(WhisperMode.REMOTE_OPENAI) }
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun WhisperModeOption(
    label: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) GreenLight else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (isSelected) GreenVontext else MaterialTheme.colorScheme.outlineVariant
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.PhoneAndroid,
                contentDescription = null,
                tint = if (isSelected) GreenVontext else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    color = if (isSelected) GreenDark else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
