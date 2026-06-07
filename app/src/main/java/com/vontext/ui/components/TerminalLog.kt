package com.vontext.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

@Composable
fun TerminalLog(
    logs: List<String>,
    modifier: Modifier = Modifier
) {
    val displayedLogs = remember(logs) { logs.takeLast(50) }
    val scrollState = rememberScrollState()

    LaunchedEffect(displayedLogs.size) {
        if (displayedLogs.isNotEmpty()) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    androidx.compose.material3.Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp),
        color = TerminalBg,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .verticalScroll(scrollState)
        ) {
            displayedLogs.forEachIndexed { index, log ->
                val logColor = when {
                    log.contains("✓", ignoreCase = true) || log.contains("OK", ignoreCase = true) -> TerminalSuccess
                    log.contains("→", ignoreCase = true) || log.contains("Enviando", ignoreCase = true) -> TerminalHighlight
                    else -> TerminalText
                }

                Text(
                    text = log,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    ),
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
