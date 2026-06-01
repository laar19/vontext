package com.vontext.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Task
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vontext.ui.theme.Error
import com.vontext.ui.theme.ErrorBg
import com.vontext.ui.theme.GreenDark
import com.vontext.ui.theme.GreenLight
import com.vontext.ui.theme.PdfIcon
import com.vontext.ui.theme.Success
import com.vontext.ui.theme.SuccessBg
import com.vontext.ui.theme.ZipIcon

/**
 * Tarjeta de trabajo en Historial
 */
@Composable
fun JobCard(
    filename: String,
    date: String,
    status: JobCardStatus,
    onSharePdf: () -> Unit,
    onShareZip: () -> Unit,
    onRetry: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    progress: Int? = null,
    progressMessage: String? = null,
    errorMessage: String? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Icon
                JobStatusIcon(status = status)
                
                // Info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = filename,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = date,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Badge
                StatusBadge(status = status)
            }
            
            // Progress bar for in-progress jobs
            if (status == JobCardStatus.Processing && progress != null) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = progress / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    color = GreenDark
                )
                if (progressMessage != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = progressMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Error message
            if (status == JobCardStatus.Error && errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = Error,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = Error
                    )
                }
            }
            
            // Actions
            Spacer(modifier = Modifier.height(12.dp))
            
            when (status) {
                JobCardStatus.Completed -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ActionButton(
                            text = "Compartir PDF",
                            icon = Icons.Default.Share,
                            iconColor = PdfIcon,
                            onClick = onSharePdf,
                            modifier = Modifier.weight(1f)
                        )
                        ActionButton(
                            text = "Compartir ZIP",
                            icon = Icons.Default.Share,
                            iconColor = ZipIcon,
                            onClick = onShareZip,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                JobCardStatus.Error -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (onRetry != null) {
                            ActionButton(
                                text = "Reintentar",
                                icon = Icons.Default.Refresh,
                                iconColor = Error,
                                onClick = onRetry,
                                modifier = Modifier.weight(1f),
                                isDestructive = true
                            )
                        }
                        if (onDelete != null) {
                            ActionButton(
                                text = "Eliminar",
                                icon = Icons.Default.Delete,
                                iconColor = Error,
                                onClick = onDelete,
                                modifier = Modifier.weight(1f),
                                isDestructive = true
                            )
                        }
                    }
                }
                JobCardStatus.Processing -> {
                    // No actions while processing
                }
            }
        }
    }
}

@Composable
private fun JobStatusIcon(status: JobCardStatus) {
    val (backgroundColor, iconColor, icon) = when (status) {
        JobCardStatus.Completed -> Triple(SuccessBg, Success, Icons.Default.Task)
        JobCardStatus.Processing -> Triple(GreenLight, GreenDark, Icons.Default.Sync)
        JobCardStatus.Error -> Triple(ErrorBg, Error, Icons.Default.Error)
    }
    
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
    
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier
                .size(24.dp)
                .then(if (status == JobCardStatus.Processing) Modifier.rotate(rotation) else Modifier)
        )
    }
}

@Composable
private fun StatusBadge(status: JobCardStatus) {
    val (backgroundColor, textColor, text) = when (status) {
        JobCardStatus.Completed -> Triple(SuccessBg, Success, "Completado")
        JobCardStatus.Processing -> Triple(GreenLight, GreenDark, "En proceso")
        JobCardStatus.Error -> Triple(ErrorBg, Error, "Error")
    }
    
    Surface(
        color = backgroundColor,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun ActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isDestructive) Error else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier.height(34.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = if (isDestructive) Error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Estados posibles para una JobCard
 */
enum class JobCardStatus {
    Completed,
    Processing,
    Error
}
