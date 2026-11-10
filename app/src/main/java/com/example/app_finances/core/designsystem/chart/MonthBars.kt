package com.example.app_finances.core.designsystem.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.app_finances.core.designsystem.theme.ChartExpense
import com.example.app_finances.core.designsystem.theme.ChartIncome

/** Um grupo do gráfico: rótulo curto do mês e os dois valores (centavos). */
@Immutable
data class MonthBarGroup(val label: String, val income: Long, val expense: Long)

/** Dados e seleção do gráfico mensal. */
@Immutable
data class MonthBarsState(
    val groups: List<MonthBarGroup>,
    val selected: Int,
    val description: String,
)

/**
 * Receitas × despesas por mês, barras lado a lado sobre a mesma base (um eixo só).
 * Tocar num mês o seleciona; os valores do mês selecionado aparecem em texto fora do gráfico.
 */
@Composable
fun MonthBars(state: MonthBarsState, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val baseline = MaterialTheme.colorScheme.outline
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(140.dp)
                .semantics { contentDescription = state.description }
                .pointerInput(state.groups.size) {
                    detectTapGestures { offset -> onSelect((offset.x / (size.width / state.groups.size)).toInt().coerceIn(0, state.groups.lastIndex)) }
                },
        ) { drawGroups(state, baseline) }
        Row(Modifier.fillMaxWidth()) {
            state.groups.forEachIndexed { index, group ->
                val color = if (index == state.selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                Text(group.label, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium, color = color)
            }
        }
    }
}

private fun DrawScope.drawGroups(state: MonthBarsState, baseline: Color) {
    val max = state.groups.maxOfOrNull { maxOf(it.income, it.expense) }?.takeIf { it > 0 } ?: 1L
    val slot = size.width / state.groups.size
    val barWidth = (slot * 0.28f).coerceAtMost(14.dp.toPx())
    val gap = 2.dp.toPx()
    state.groups.forEachIndexed { index, group ->
        val alpha = if (index == state.selected) 1f else 0.45f
        val center = slot * index + slot / 2
        drawBar(Offset(center - gap / 2 - barWidth, 0f), barWidth, group.income.toFloat() / max, ChartIncome.copy(alpha = alpha))
        drawBar(Offset(center + gap / 2, 0f), barWidth, group.expense.toFloat() / max, ChartExpense.copy(alpha = alpha))
    }
    drawLine(baseline, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
}

/** Barra ancorada na base, com o topo arredondado (4 dp). */
private fun DrawScope.drawBar(origin: Offset, width: Float, fraction: Float, color: Color) {
    if (fraction <= 0f) return
    val height = (size.height * fraction).coerceAtLeast(4.dp.toPx())
    val radius = CornerRadius(4.dp.toPx())
    drawRoundRect(color, Offset(origin.x, size.height - height), Size(width, height), radius)
    // Cobre os cantos de baixo: só o topo fica arredondado.
    drawRect(color, Offset(origin.x, size.height - radius.y), Size(width, radius.y))
}

/** Legenda: quadradinho da cor + nome da série. */
@Composable
fun ChartLegend(items: List<Pair<String, Color>>) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        items.forEach { (label, color) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(color))
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
