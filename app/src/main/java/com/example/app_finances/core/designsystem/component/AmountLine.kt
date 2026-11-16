package com.example.app_finances.core.designsystem.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/** "Rótulo ............ R$ 0,00" numa linha. [emphasized]: linha de total. */
@Composable
fun AmountLine(label: String, cents: Long, tone: AmountTone = AmountTone.Neutral, emphasized: Boolean = false) {
    val style = if (emphasized) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = style, color = if (emphasized) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
        MoneyText(cents, tone = tone, style = style)
    }
}
