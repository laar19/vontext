package com.vontext.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
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
    var interval by remember { mutableIntStateOf(0) }
    var notes by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var progressMessage by remember { mutableStateOf("") }
    var logs by remember { mutableStateOf<List<String>>(emptyList()) }
    var showResults by remember { mutableStateOf(false) }
    var pdfPath by remember { mutableStateOf<String?>(null) }
    var zipPath by remember { mutableStateOf<String?>(null) }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { 
            if (it !in selectedVideos) {
                selectedVideos.add(it)
            }
        }
    }

    val listState = rememberLazyListState()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC)),
            state = listState
        ) {
            // Header
            item {
                HeaderSection(onNavigateToSettings, onNavigateToHistory)
            }

            // Features
            item {
                FeaturesSection()
            }

            // Upload Zone
            item {
                UploadSection(
                    selectedVideos = selectedVideos,
                    onPickVideo = { videoPicker.launch("video/*") },
                    onRemoveVideo = { index -> selectedVideos.removeAt(index) },
                    processTogether = processTogether,
                    onProcessTogetherChange = { processTogether = it },
                    onReorder = { from, to ->
                        selectedVideos.add(to, selectedVideos.removeAt(from))
                    }
                )
            }

            // Interval
            item {
                IntervalSection(
                    interval = interval,
                    onIntervalChange = { interval = it }
                )
            }

            // Notes
            item {
                NotesSection(notes = notes, onNotesChange = { notes = it })
            }

            // Process Button
            item {
                ProcessButton(
                    videoCount = selectedVideos.size,
                    enabled = selectedVideos.isNotEmpty() && !isProcessing,
                    onClick = {
                        isProcessing = true
                        showResults = false
                        scope.launch {
                            viewModel.processVideos(
                                videos = selectedVideos.toList(),
                                processTogether = processTogether,
                                interval = interval,
                                notes = notes.ifBlank { null },
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
                                            // Obtener el job de la BD para obtener pdfPath y zipPath
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
                )
            }

            // Progress & Logs (during processing)
            if (isProcessing) {
                item {
                    ProgressSection(
                        progress = progress,
                        message = progressMessage,
                        logs = logs
                    )
                }
            }

            // Results (after completion)
            if (showResults) {
                item {
                    ResultsSection(
                        pdfPath = pdfPath,
                        zipPath = zipPath
                    )
                }
            }

            // Info Section
            item {
                InfoSection()
            }

            // Bottom padding
            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
private fun HeaderSection(
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF6366F1))
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "🎬 Vontext",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Row {
                IconButton(onClick = onNavigateToHistory) {
                    Icon(
                        imageVector = Icons.Default.VideoFile,
                        contentDescription = "Historial",
                        tint = Color.White
                    )
                }
                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Configuración",
                        tint = Color.White
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Procesa videos de grabaciones de pantalla y genera contexto rico para agentes de IA.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.9f)
        )
    }
}

@Composable
private fun FeaturesSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Características:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B)
        )
        Spacer(modifier = Modifier.height(12.dp))
        FeatureItem("🎯", "Extracción inteligente de frames únicos")
        FeatureItem("🎤", "Transcripción de audio con Whisper API")
        FeatureItem("📄", "Generación de PDF profesional")
        FeatureItem("📦", "Descarga completa en ZIP")
    }
}

@Composable
private fun FeatureItem(icon: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icon, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF64748B)
        )
    }
}

@Composable
private fun UploadSection(
    selectedVideos: List<Uri>,
    onPickVideo: () -> Unit,
    onRemoveVideo: (Int) -> Unit,
    processTogether: Boolean,
    onProcessTogetherChange: (Boolean) -> Unit,
    onReorder: (Int, Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "📤 Subir Video",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Upload zone
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .border(
                        border = BorderStroke(2.dp, Color(0xFF6366F1).copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .background(Color(0xFFEEF2FF))
                    .clickable(onClick = onPickVideo),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Upload",
                        modifier = Modifier.size(48.dp),
                        tint = Color(0xFF6366F1)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Toca para seleccionar",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF6366F1),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "o arrastra el video aquí",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                }
            }

            // Selected videos
            if (selectedVideos.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Videos seleccionados (${selectedVideos.size})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(8.dp))

                selectedVideos.forEachIndexed { index, uri ->
                    VideoItem(
                        uri = uri,
                        index = index,
                        totalItems = selectedVideos.size,
                        canReorder = selectedVideos.size > 1 && processTogether,
                        onRemove = { onRemoveVideo(index) },
                        onMoveUp = { 
                            if (index > 0) {
                                val newList = selectedVideos.toMutableList()
                                val temp = newList[index]
                                newList[index] = newList[index - 1]
                                newList[index - 1] = temp
                                onReorder(index, index - 1)
                            }
                        },
                        onMoveDown = { 
                            if (index < selectedVideos.size - 1) {
                                val newList = selectedVideos.toMutableList()
                                val temp = newList[index]
                                newList[index] = newList[index + 1]
                                newList[index + 1] = temp
                                onReorder(index, index + 1)
                            }
                        }
                    )
                }

                // Process mode toggle
                if (selectedVideos.size > 1) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProcessModeChip(
                            label = "🔗 Procesar juntos",
                            selected = processTogether,
                            onClick = { onProcessTogetherChange(true) }
                        )
                        ProcessModeChip(
                            label = "📎 Procesar por separado",
                            selected = !processTogether,
                            onClick = { onProcessTogetherChange(false) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoItem(
    uri: Uri,
    index: Int,
    totalItems: Int,
    canReorder: Boolean,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
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
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = "Subir",
                            tint = if (index > 0) Color(0xFF6366F1) else Color(0xFF94A3B8),
                            modifier = Modifier
                                .size(16.dp)
                                .graphicsLayer { rotationZ = 180f }
                        )
                    }
                    IconButton(
                        onClick = onMoveDown,
                        enabled = index < totalItems - 1,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = "Bajar",
                            tint = if (index < totalItems - 1) Color(0xFF6366F1) else Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF6366F1)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Video ${index + 1}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF1E293B),
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Eliminar",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ProcessModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) Color(0xFF6366F1) else Color.White,
        border = BorderStroke(1.dp, Color(0xFF6366F1)),
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = if (selected) Color.White else Color(0xFF6366F1),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun IntervalSection(
    interval: Int,
    onIntervalChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "🎯 Intervalo de Captura",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = if (interval == 0) "" else interval.toString(),
                    onValueChange = { 
                        onIntervalChange(it.toIntOrNull() ?: 0)
                    },
                    label = { Text("Segundos") },
                    placeholder = { Text("0 = Auto") },
                    modifier = Modifier.width(100.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "0 = Auto / detección de escenas",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Valores recomendados: 1, 2, 3, 5, 9, 30",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF64748B)
            )
        }
    }
}

@Composable
private fun NotesSection(
    notes: String,
    onNotesChange: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "📝 Notas Adicionales (Opcional)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChange,
                label = { Text("Pegar notas/logs") },
                placeholder = { Text("Pega aquí cualquier nota, log o información adicional…") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5
            )
        }
    }
}

@Composable
private fun ProcessButton(
    videoCount: Int,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(56.dp)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (enabled) Color(0xFF6366F1) else Color(0xFF94A3B8),
        shadowElevation = 4.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = if (videoCount > 1) "🚀 Procesar $videoCount Videos" else "🚀 Procesar Video",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ProgressSection(
    progress: Int,
    message: String,
    logs: List<String>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📊 Progreso",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = "$progress%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6366F1),
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = progress / 100f,
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF6366F1)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF64748B)
            )

            if (logs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "📋 Logs de Procesamiento",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(Color(0xFFF1F5F9))
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        items(logs.size) { index ->
                            Text(
                                text = logs[index],
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B),
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "🎁 Resultados",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Status
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.1f))
            ) {
                Text(
                    text = "✅ Completado",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF10B981),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // PDF
            if (pdfPath != null) {
                ResultItem(
                    icon = "📄",
                    title = "PDF Report",
                    onClick = { /* TODO: Open/share PDF */ }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ZIP
            if (zipPath != null) {
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
private fun ResultItem(
    icon: String,
    title: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFEEF2FF),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF6366F1),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Acción",
                tint = Color(0xFF6366F1),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun InfoSection() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "ℹ️ Información:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            Spacer(modifier = Modifier.height(12.dp))
            InfoItem("• Tamaño máximo: 2GB")
            InfoItem("• Formatos: MP4, MKV, AVI, MOV, WebM")
            InfoItem("• El procesamiento puede tomar varios minutos dependiendo del tamaño del video")
            InfoItem("• Usá el bot de Telegram para procesamiento más rápido")
        }
    }
}

@Composable
private fun InfoItem(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = Color(0xFF64748B),
        modifier = Modifier.padding(vertical = 4.dp)
    )
}
