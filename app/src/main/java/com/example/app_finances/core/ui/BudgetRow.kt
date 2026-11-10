package com.example.app_finances.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.app_finances.R
import com.example.app_finances.core.designsystem.component.ThinBar
import com.example.app_finances.core.designsystem.theme.FinanceTheme
import com.example.app_finances.core.format.Money
import com.example.app_finances.domain.BudgetLevel
import com.example.app_finances.domain.BudgetStatus
import kotlin.math.roundToInt

/**
 * Orçamento de uma categoria: gasto × teto, barra e a situação em texto
 * ("Atenção: 90% usado", "Estourou em R$ 50,00") — nunca só pela cor.
 */
@Composable
fun BudgetRow(status: BudgetStatus, modifier: Modifier = Modifier) {
    val category = status.category
    val levelColor = levelColor(status.level)
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        CategoryBadge(category, size = 32.dp)
        Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row {
                Text(category.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    stringResource(R.string.budget_progress, Money.format(status.spentCents), Money.format(status.budgetCents)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ThinBar(status.fraction, levelColor)
            Text(levelText(status), style = MaterialTheme.typography.labelSmall, color = levelTextColor(status.level))
        }
    }
}

@Composable
private fun levelColor(level: BudgetLevel): Color = when (level) {
    BudgetLevel.OK -> MaterialTheme.colorScheme.primary
    BudgetLevel.WARNING -> FinanceTheme.colors.warning
    BudgetLevel.OVER -> FinanceTheme.colors.expense
}

/** Texto em situação normal fica discreto; atenção e estouro usam a cor do alerta. */
@Composable
private fun levelTextColor(level: BudgetLevel): Color =
    if (level == BudgetLevel.OK) MaterialTheme.colorScheme.onSurfaceVariant else levelColor(level)

@Composable
private fun levelText(status: BudgetStatus): String {
    val percent = (status.fraction * 100).roundToInt()
    return when (status.level) {
        BudgetLevel.OK -> stringResource(R.string.budget_ok, percent)
        BudgetLevel.WARNING -> stringResource(R.string.budget_warning, percent)
        BudgetLevel.OVER -> stringResource(R.string.budget_over, Money.format(-status.remainingCents))
    }
}
