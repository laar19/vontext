package com.vontext.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vontext.ui.theme.TerminalBg
import com.vontext.ui.theme.TerminalHighlight
import com.vontext.ui.theme.TerminalSuccess
import com.vontext.ui.theme.TerminalText

/**
 * Terminal oscura para logs en vivo
 */
@Composable
fun TerminalLog(
    logs: List<String>,
    modifier: Modifier = Modifier
) {
    // Limitar a últimos 50 logs para no saturar memoria
    val displayedLogs = remember(logs) { logs.takeLast(50) }
    
    val listState = rememberLazyListState()
    
    // Auto-scroll al final
    LaunchedEffect(displayedLogs.size) {
        if (displayedLogs.isNotEmpty()) {
            listState.animateScrollToItem(displayedLogs.size - 1)
        }
    }
    
    androidx.compose.material3.Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp),
        color = TerminalBg,
        shape = MaterialTheme.shapes.medium
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            itemsIndexed(displayedLogs) { index, log ->
                val logColor = when {
                    log.contains("✓", ignoreCase = true) || log.contains("OK", ignoreCase = true) -> TerminalSuccess
                    log.contains("→", ignoreCase = true) || log.contains("Enviando", ignoreCase = true) -> TerminalHighlight
                    else -> TerminalText
                }
                
                Text(
                    text = log,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                    color = logColor,
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (index < displayedLogs.size - 1) {
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(2.dp))
                }
            }
        }
    }
}
