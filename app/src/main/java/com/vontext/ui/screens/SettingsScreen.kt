package com.vontext.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vontext.viewmodel.SettingsViewModel
import com.vontext.viewmodel.VideoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    videoViewModel: VideoViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val settings by settingsViewModel.settings.collectAsState(initial = null)
    val isModelDownloaded by videoViewModel.isModelDownloaded.collectAsState(initial = false)
    val modelDownloadProgress by videoViewModel.modelDownloadProgress.collectAsState(initial = null)
    val isDownloadingModel by videoViewModel.isDownloadingModel.collectAsState(initial = false)
    var showClearDialog by remember { mutableStateOf(false) }
    var justDownloadedModel by remember { mutableStateOf(false) }
    
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showEndpointDialog by remember { mutableStateOf(false) }
    var showModelDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showModelDownloadDialog by remember { mutableStateOf(false) }
    
    // Detectar cuando la descarga se completa para mostrar estado de éxito
    androidx.compose.runtime.LaunchedEffect(isModelDownloaded) {
        if (isModelDownloaded && !justDownloadedModel) {
            justDownloadedModel = true
        }
    }
    
    var apiKey by remember { mutableStateOf("") }
    var endpoint by remember { mutableStateOf("https://api.openai.com/v1") }
    var model by remember { mutableStateOf("whisper-1") }
    var showApiKey by remember { mutableStateOf(false) }
    var language by remember { mutableStateOf("es") }
    
    androidx.compose.runtime.LaunchedEffect(settings) {
        settings?.let {
            apiKey = it.openaiApiKey ?: ""
            endpoint = it.openaiBaseUrl ?: "https://api.openai.com/v1"
            model = it.whisperModel
            language = it.language
        }
    }

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = {
                    Text(
                        text = "Ajustes",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(bottom = 100.dp)
        ) {
            // API de Whisper section
            ListSubheader("API de Whisper")
            
            OutlinedCard {
                ConfigRow(
                    icon = Icons.Default.Key,
                    label = "API Key",
                    value = maskApiKey(apiKey),
                    onClick = { showApiKeyDialog = true }
                )
                ConfigRow(
                    icon = Icons.Default.Language,
                    label = "Endpoint",
                    value = endpoint,
                    onClick = { showEndpointDialog = true }
                )
                ConfigRow(
                    icon = Icons.Outlined.SmartToy,
                    label = "Modelo",
                    value = model,
                    onClick = { showModelDialog = true }
                )
                ConfigRow(
                    icon = Icons.Default.Download,
                    label = "Modelo Local",
                    value = if (isModelDownloaded) "Descargado (~466 MB)" else "No descargado",
                    onClick = { showModelDownloadDialog = true }
                )
                ConfigRow(
                    icon = Icons.Default.Language,
                    label = "Idioma",
                    value = if (language == "es") "Español" else "English",
                    onClick = { showLanguageDialog = true }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Acerca de section
            ListSubheader("Acerca de")
            
            OutlinedCard {
                ConfigRow(
                    icon = Icons.Outlined.Info,
                    label = "Versión",
                    value = "1.0.8",
                    showChevron = false
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Danger zone
            ListSubheader("Datos")
            
            Button(
                onClick = { showClearDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Limpiar historial",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    
    // API Key Dialog
    if (showApiKeyDialog) {
        EditDialog(
            title = "API Key",
            value = apiKey,
            onValueChange = { apiKey = it },
            onSave = {
                settingsViewModel.updateApiKey(apiKey)
                showApiKeyDialog = false
            },
            onDismiss = { showApiKeyDialog = false },
            placeholder = "sk-...",
            isPassword = true,
            showPasswordToggle = true,
            showPassword = showApiKey,
            onPasswordToggle = { showApiKey = !showApiKey }
        )
    }
    
    // Endpoint Dialog
    if (showEndpointDialog) {
        EditDialog(
            title = "Endpoint",
            value = endpoint,
            onValueChange = { endpoint = it },
            onSave = {
                settingsViewModel.updateEndpoint(endpoint)
                showEndpointDialog = false
            },
            onDismiss = { showEndpointDialog = false },
            placeholder = "https://api.openai.com/v1"
        )
    }
    
    // Model Dialog
    if (showModelDialog) {
        EditDialog(
            title = "Modelo",
            value = model,
            onValueChange = { model = it },
            onSave = {
                settingsViewModel.updateModel(model)
                showModelDialog = false
            },
            onDismiss = { showModelDialog = false },
            placeholder = "whisper-1"
        )
    }
    
    // Language Dialog
    if (showLanguageDialog) {
        LanguageDialog(
            currentLanguage = language,
            onLanguageSelected = { lang ->
                settingsViewModel.updateLanguage(lang)
                language = lang
            },
            onDismiss = { showLanguageDialog = false }
        )
    }
    
    // Model Download Dialog
    if (showModelDownloadDialog) {
        AlertDialog(
            onDismissRequest = { 
                if (!isDownloadingModel) {
                    justDownloadedModel = false
                    showModelDownloadDialog = false
                }
            },
            title = {
                Text(text = "Modelo Local Whisper")
            },
            text = {
                Column {
                    if (justDownloadedModel) {
                        Text(
                            text = "✅ Modelo descargado exitosamente",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "El modelo Small (~466 MB) está listo para usar en transcripción offline.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (isModelDownloaded) {
                        Text(
                            text = "El modelo Small (~466 MB) ya está descargado en tu dispositivo.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "💡 El modelo se usa para transcripción offline. Puedes eliminarlo para liberar espacio y volver a descargarlo cuando lo necesites.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    } else if (isDownloadingModel) {
                        Text(
                            text = "Descargando modelo Small (~466 MB)...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = (modelDownloadProgress?.percent ?: 0) / 100f,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        modelDownloadProgress?.let { prog ->
                            Text(
                                text = "${prog.percent}% (${formatBytes(prog.downloaded)} / ${formatBytes(prog.total)})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(
                            text = "Descarga el modelo Small (~466 MB) para usar transcripción offline sin necesidad de conexión a internet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "💡 La descarga puede tardar varios minutos dependiendo de tu conexión. El modelo se guarda en tu dispositivo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                if (justDownloadedModel) {
                    Button(
                        onClick = {
                            justDownloadedModel = false
                            showModelDownloadDialog = false
                        }
                    ) {
                        Text("Aceptar")
                    }
                } else if (isModelDownloaded) {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            // TODO: Implementar eliminar modelo
                            showModelDownloadDialog = false
                        }
                    ) {
                        Text("Eliminar")
                    }
                } else if (!isDownloadingModel) {
                    Button(
                        onClick = {
                            videoViewModel.downloadModel()
                        }
                    ) {
                        Text("Descargar")
                    }
                }
            },
            dismissButton = {
                when {
                    justDownloadedModel -> {
                        androidx.compose.material3.TextButton(
                            onClick = {
                                // TODO: Implementar eliminar modelo
                                showModelDownloadDialog = false
                            }
                        ) {
                            Text("Eliminar")
                        }
                    }
                    isDownloadingModel -> {
                        androidx.compose.material3.TextButton(
                            onClick = {
                                videoViewModel.cancelDownload()
                            }
                        ) {
                            Text("Cancelar")
                        }
                    }
                    else -> {
                        androidx.compose.material3.TextButton(
                            onClick = { showModelDownloadDialog = false }
                        ) {
                            Text("Cancelar")
                        }
                    }
                }
            }
        )
    }
    
    // Clear Data Dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("¿Limpiar historial?") },
            text = { Text("Esta acción eliminará todos los trabajos procesados. No se pueden deshacer los cambios.") },
            confirmButton = {
                Button(
                    onClick = {
                        settingsViewModel.clearHistory()
                        showClearDialog = false
                    }
                ) {
                    Text("Limpiar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1_000_000_000 -> String.format("%.1f GB", bytes / 1_000_000_000.0)
        bytes >= 1_000_000 -> String.format("%.1f MB", bytes / 1_000_000.0)
        bytes >= 1_000 -> String.format("%.1f KB", bytes / 1_000.0)
        else -> "$bytes B"
    }
}

@Composable
private fun ListSubheader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun OutlinedCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun ConfigRow(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: (() -> Unit)? = null,
    showChevron: Boolean = true
) {
    val isClickable = onClick != null
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isClickable) Modifier.clickable(onClick = onClick!!) else Modifier),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (showChevron) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Editar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
    // Divider
    androidx.compose.material3.Divider(
        modifier = Modifier.padding(start = 56.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

@Composable
private fun EditDialog(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    placeholder: String = "",
    isPassword: Boolean = false,
    showPasswordToggle: Boolean = false,
    showPassword: Boolean = false,
    onPasswordToggle: () -> Unit = {}
) {
    var localValue by remember { mutableStateOf(value) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = localValue,
                onValueChange = { localValue = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(placeholder) },
                visualTransformation = if (isPassword && !showPassword) {
                    PasswordVisualTransformation()
                } else {
                    VisualTransformation.None
                },
                trailingIcon = if (showPasswordToggle) {
                    {
                        IconButton(onClick = onPasswordToggle) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (showPassword) "Ocultar" else "Mostrar"
                            )
                        }
                    }
                } else null,
                maxLines = 1
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onValueChange(localValue)
                    onSave()
                }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun LanguageDialog(
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedLanguage by remember { mutableStateOf(currentLanguage) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Seleccionar idioma / Select language")
        },
        text = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedLanguage = "es" }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.RadioButton(
                        selected = selectedLanguage == "es",
                        onClick = { selectedLanguage = "es" }
                    )
                    Text(
                        text = "Español",
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedLanguage = "en" }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.RadioButton(
                        selected = selectedLanguage == "en",
                        onClick = { selectedLanguage = "en" }
                    )
                    Text(
                        text = "English",
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Nota: Debes reiniciar la app para que el cambio surta efecto.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onLanguageSelected(selectedLanguage)
                }
            ) {
                Text("Aceptar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

private fun maskApiKey(key: String): String {
    if (key.isEmpty()) return "No configurada"
    if (key.length < 10) return "••••••••"
    return "sk-${key.takeLast(8).padStart(12, '•')}"
}
