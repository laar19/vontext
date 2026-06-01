package com.vontext.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.vontext.ui.theme.ErrorBg
import com.vontext.ui.theme.Error
import com.vontext.ui.theme.GreenLight
import com.vontext.ui.theme.GreenDark
import com.vontext.ui.theme.SuccessBg
import com.vontext.ui.theme.Success

/**
 * Etapa del procesamiento (usada en ProcessingScreen)
 */
@Composable
fun StepItem(
    stage: ProcessingStage,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 13.dp)
    ) {
        // Icono de estado
        StepIcon(stage = stage)
        
        // Info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = stage.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (stage.detail != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stage.detail!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StepIcon(stage: ProcessingStage) {
    val backgroundColor = when (stage.status) {
        StageStatus.Completed -> SuccessBg
        StageStatus.Active -> GreenLight
        StageStatus.Pending -> MaterialTheme.colorScheme.surfaceVariant
        StageStatus.Error -> ErrorBg
    }
    
    val iconColor = when (stage.status) {
        StageStatus.Completed -> Success
        StageStatus.Active -> GreenDark
        StageStatus.Pending -> MaterialTheme.colorScheme.onSurfaceVariant
        StageStatus.Error -> Error
    }
    
    val icon = when (stage.type) {
        ProcessingStageType.Carga -> Icons.Default.Image
        ProcessingStageType.Frames -> Icons.Default.Image
        ProcessingStageType.Transcripcion -> Icons.Default.Mic
        ProcessingStageType.Pdf -> Icons.Default.PictureAsPdf
        ProcessingStageType.Zip -> Icons.Default.FolderZip
    }
    
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(backgroundColor, MaterialTheme.shapes.medium),
        contentAlignment = Alignment.Center
    ) {
        when (stage.status) {
            StageStatus.Completed -> {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            StageStatus.Active -> {
                val infiniteTransition = rememberInfiniteTransition(label = "spin")
                val rotation by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "rotation"
                )
                
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(rotation)
                )
            }
            StageStatus.Pending -> {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            StageStatus.Error -> {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Tipos de etapa de procesamiento
 */
enum class ProcessingStageType {
    Carga,
    Frames,
    Transcripcion,
    Pdf,
    Zip
}

/**
 * Estado de una etapa
 */
enum class StageStatus {
    Pending,
    Active,
    Completed,
    Error
}

/**
 * Modelo de etapa de procesamiento
 */
data class ProcessingStage(
    val type: ProcessingStageType,
    val title: String,
    val detail: String? = null,
    val status: StageStatus = StageStatus.Pending
)

/**
 * Helper para crear etapas con título por defecto
 */
fun processingStage(
    type: ProcessingStageType,
    detail: String? = null,
    status: StageStatus = StageStatus.Pending
): ProcessingStage {
    val title = when (type) {
        ProcessingStageType.Carga -> "Carga de archivo"
        ProcessingStageType.Frames -> "Extracción de frames"
        ProcessingStageType.Transcripcion -> "Transcripción de audio"
        ProcessingStageType.Pdf -> "Generar PDF"
        ProcessingStageType.Zip -> "Empaquetar ZIP"
    }
    return ProcessingStage(type = type, title = title, detail = detail, status = status)
}
