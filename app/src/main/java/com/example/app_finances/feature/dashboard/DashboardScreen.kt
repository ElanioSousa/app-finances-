package com.example.app_finances.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.app_finances.R
import com.example.app_finances.core.designsystem.component.AmountTone
import com.example.app_finances.core.designsystem.component.CategoryIcon
import com.example.app_finances.core.designsystem.component.EmptyState
import com.example.app_finances.core.designsystem.component.MoneyText
import com.example.app_finances.core.designsystem.component.ScreenColumn
import com.example.app_finances.core.designsystem.component.SectionCard
import com.example.app_finances.core.designsystem.component.color
import com.example.app_finances.core.designsystem.theme.EmeraldDeep
import com.example.app_finances.core.designsystem.theme.Ink850

/** Resumo do mês exibido no topo do Início. Os valores vêm do banco a partir da fase 2. */
data class MonthSummary(
    val balanceCents: Long = 0,
    val incomeCents: Long = 0,
    val expenseCents: Long = 0,
)

@Composable
fun DashboardScreen(scaffoldPadding: PaddingValues, summary: MonthSummary = MonthSummary()) {
    ScreenColumn(title = stringResource(R.string.home_greeting), scaffoldPadding = scaffoldPadding) {
        item { BalanceCard(summary) }
        item {
            SectionCard {
                EmptyState(
                    icon = Icons.Outlined.Savings,
                    title = stringResource(R.string.home_empty_title),
                    body = stringResource(R.string.home_empty_body),
                )
            }
        }
    }
}

/** O tema é fixo, então o degradê do cartão de saldo pode ser criado uma vez só. */
private val BalanceGradient = Brush.linearGradient(listOf(EmeraldDeep, Ink850))

@Composable
private fun BalanceCard(summary: MonthSummary) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(BalanceGradient)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.home_total_balance),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            MoneyText(cents = summary.balanceCents, style = MaterialTheme.typography.displaySmall)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            FlowTile(FlowTileData(R.string.home_income, summary.incomeCents, Icons.AutoMirrored.Filled.TrendingUp, AmountTone.Income), Modifier.weight(1f))
            FlowTile(FlowTileData(R.string.home_expenses, summary.expenseCents, Icons.AutoMirrored.Filled.TrendingDown, AmountTone.Expense), Modifier.weight(1f))
        }
    }
}

private data class FlowTileData(
    val labelRes: Int,
    val cents: Long,
    val icon: ImageVector,
    val tone: AmountTone,
)

@Composable
private fun FlowTile(data: FlowTileData, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CategoryIcon(icon = data.icon, color = data.tone.color(), size = 36.dp)
        Column {
            Text(
                text = stringResource(data.labelRes),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            MoneyText(cents = data.cents, tone = data.tone)
        }
    }
}
