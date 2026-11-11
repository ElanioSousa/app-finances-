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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.app_finances.core.ui.LocalSnackbarHostState
import com.example.app_finances.core.ui.containerViewModel
import com.example.app_finances.core.model.TransactionItem
import com.example.app_finances.core.ui.TransactionRow
import com.example.app_finances.domain.MonthTotals
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
import com.example.app_finances.core.designsystem.component.MonthSelector
import com.example.app_finances.core.designsystem.component.color
import com.example.app_finances.core.designsystem.theme.EmeraldDeep
import com.example.app_finances.core.designsystem.theme.Ink850

/** Ações do Início, agrupadas. */
data class DashboardActions(
    val onOpenTransaction: (Long) -> Unit,
    val onOpenCard: (Long) -> Unit,
    val onSeeAll: () -> Unit,
)

@Composable
fun DashboardScreen(scaffoldPadding: PaddingValues, actions: DashboardActions) {
    val viewModel = containerViewModel {
        DashboardViewModel(DashboardSources(it.accountRepository, it.transactionRepository, it.cardRepository, it.categoryRepository, it.recurringRepository), it.today)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarHostState.current
    LaunchedEffect(viewModel) { viewModel.errorFlow.collect { snackbar.showSnackbar(it) } }
    val data = state.data

    ScreenColumn(title = stringResource(R.string.home_greeting), scaffoldPadding = scaffoldPadding) {
        // Só mostra números depois de carregar: "R$ 0,00" provisório engana.
        if (data == null) return@ScreenColumn
        item { MonthSelector(state.month, viewModel::setMonth) }
        item { BalanceCard(data.balanceCents, data.monthTotals, data.includesProjected) }
        item { UpcomingCard(data.upcoming, data.today, UpcomingActions(actions.onOpenTransaction, actions.onOpenCard, viewModel::markPaid)) }
        if (data.budgets.isNotEmpty()) item { BudgetsCard(data.budgets) }
        if (data.cards.isNotEmpty()) item { CardsCard(data.cards, data.today, actions.onOpenCard) }
        item { SpendingCard(data.spending, data.categoryById) }
        item { HistoryCard(data.history, state.selectedMonth, viewModel::selectMonth) }
        item { RecentCard(data.recent, actions.onOpenTransaction, actions.onSeeAll) }
    }
}

@Composable
private fun RecentCard(recent: List<TransactionItem>, onOpenTransaction: (Long) -> Unit, onSeeAll: () -> Unit) {
    if (recent.isEmpty()) {
        SectionCard {
            EmptyState(
                icon = Icons.Outlined.Savings,
                title = stringResource(R.string.home_empty_title),
                body = stringResource(R.string.home_empty_body),
            )
        }
        return
    }
    SectionCard(
        title = stringResource(R.string.home_recent),
        action = { TextButton(onClick = onSeeAll) { Text(stringResource(R.string.home_see_all)) } },
    ) {
        recent.forEach { item ->
            TransactionRow(item, onClick = { onOpenTransaction(item.id) }, modifier = Modifier.clip(MaterialTheme.shapes.medium))
        }
    }
}

/** O tema é fixo, então o degradê do cartão de saldo pode ser criado uma vez só. */
private val BalanceGradient = Brush.linearGradient(listOf(EmeraldDeep, Ink850))

@Composable
private fun BalanceCard(balanceCents: Long, month: MonthTotals, includesProjected: Boolean) {
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
            MoneyText(cents = balanceCents, style = MaterialTheme.typography.displaySmall)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            FlowTile(FlowTileData(R.string.home_income, month.incomeCents, Icons.AutoMirrored.Filled.TrendingUp, AmountTone.Income), Modifier.weight(1f))
            FlowTile(FlowTileData(R.string.home_expenses, month.expenseCents, Icons.AutoMirrored.Filled.TrendingDown, AmountTone.Expense), Modifier.weight(1f))
        }
        if (includesProjected) {
            Text(stringResource(R.string.home_includes_projected), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
