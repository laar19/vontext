package com.videocontextbot.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.videocontextbot.viewmodel.VideoViewModel

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

@Composable
fun HomeScreen(
    viewModel: VideoViewModel? = null,
    onNavigateToProcessing: (String) -> Unit
) {
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var notes by remember { mutableStateOf("") }
    var frameInterval by remember { mutableIntStateOf(0) }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        selectedVideoUri = uri
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            onClick = { videoPicker.launch("video/*") }
        ) {
            selectedVideoUri?.let { uri ->
                AsyncImage(
                    model = uri,
                    contentDescription = "Video seleccionado",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } ?: run {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = "Seleccionar video",
                            modifier = Modifier.size(48.dp)
                        )
                        Text("Seleccionar video")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notas adicionales (opcional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Intervalo de frames:")
            Text(
                text = if (frameInterval == 0) "Auto (detección de escenas)" else "$frameInterval segundos",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Slider(
            value = frameInterval.toFloat(),
            onValueChange = { frameInterval = it.toInt() },
            valueRange = 0f..30f,
            steps = 29
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                selectedVideoUri?.let { uri ->
                    viewModel?.startProcessing(uri, notes.ifBlank { null }, frameInterval)
                    viewModel?.processingState?.value?.jobId?.let { jobId ->
                        onNavigateToProcessing(jobId)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedVideoUri != null
        ) {
            Text("Procesar Video")
        }
    }
}
