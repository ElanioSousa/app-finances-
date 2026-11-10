package com.example.app_finances.core.designsystem.chart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.app_finances.core.designsystem.component.CategoryIcon
import com.example.app_finances.core.designsystem.component.ThinBar
import com.example.app_finances.core.designsystem.theme.ChartMagnitude
import kotlin.math.roundToInt

/** Uma linha do ranking: ícone, nome, valor já formatado e a fração do total. */
@Immutable
data class RankedBar(
    val label: String,
    val value: String,
    val share: Float,
    val icon: ImageVector,
    val iconColor: Color,
)

/**
 * Barras horizontais ordenadas (maior primeiro), uma cor só: comparar partes em barras
 * alinhadas é mais preciso que fatias de rosca. Cada linha diz o nome e o valor em texto.
 */
@Composable
fun RankedBars(rows: List<RankedBar>, modifier: Modifier = Modifier) {
    val largest = rows.maxOfOrNull { it.share }?.takeIf { it > 0f } ?: 1f
    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        rows.forEach { row -> RankedBarRow(row, row.share / largest) }
    }
}

@Composable
private fun RankedBarRow(row: RankedBar, relative: Float) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CategoryIcon(row.icon, row.iconColor, size = 32.dp)
        Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row {
                Text(row.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${row.value} · ${(row.share * 100).roundToInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ThinBar(relative, ChartMagnitude)
        }
    }
}
