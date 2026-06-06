package com.vontext.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.vontext.ui.components.InfoStrip
import com.vontext.ui.components.ModeChipRow
import com.vontext.ui.components.NotesField
import com.vontext.ui.components.SectionLabel
import com.vontext.ui.components.StepperBlock
import com.vontext.ui.components.VideoQueueItem
import com.vontext.ui.theme.GreenVontext
import com.vontext.viewmodel.VideoViewModel
import kotlin.random.Random

@Composable
fun HomeScreen(
    viewModel: VideoViewModel = hiltViewModel(),
    selectedVideos: MutableList<Uri>,
    onNavigateToProcessing: (List<Uri>, Boolean, Int, String?) -> Unit
) {
    var processTogether by remember { mutableStateOf(true) }
    var interval by remember { mutableIntStateOf(5) }
    var notes by remember { mutableStateOf("") }

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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
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
                Spacer(modifier = Modifier.height(4.dp))

                androidx.compose.material3.Button(
                    onClick = {
                        onNavigateToProcessing(
                            selectedVideos.toList(),
                            processTogether,
                            interval,
                            notes.ifBlank { null }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = GreenVontext
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PlayCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Procesar ${selectedVideos.size} videos",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                SectionLabel("Modo de procesamiento")
                ModeChipRow(
                    processTogether = processTogether,
                    onModeChange = { processTogether = it }
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
                border = androidx.compose.foundation.BorderStroke(
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
