package com.example.app_finances.feature.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.app_finances.R
import com.example.app_finances.core.designsystem.component.AmountTone
import com.example.app_finances.core.designsystem.component.EmptyState
import com.example.app_finances.core.designsystem.component.MoneyText
import com.example.app_finances.core.designsystem.component.MonthSelector
import com.example.app_finances.core.designsystem.component.ScreenColumn
import com.example.app_finances.core.designsystem.theme.FinanceTheme
import com.example.app_finances.core.format.Dates
import com.example.app_finances.core.model.TransactionItem
import com.example.app_finances.core.ui.LocalSnackbarHostState
import com.example.app_finances.core.ui.TransactionRow
import com.example.app_finances.core.ui.containerViewModel
import com.example.app_finances.domain.MonthTotals
import java.time.YearMonth

/** Ações da tela agrupadas, para não espalhar callbacks por todos os componentes. */
private data class TransactionsActions(
    val onMonthChange: (YearMonth) -> Unit,
    val onFilterChange: (TransactionFilter) -> Unit,
    val onOpen: (Long) -> Unit,
    val onDelete: (TransactionItem) -> Unit,
)

@Composable
fun TransactionsScreen(scaffoldPadding: PaddingValues, onOpenTransaction: (Long) -> Unit) {
    val viewModel = containerViewModel {
        TransactionsViewModel(it.transactionRepository, it.catalogSources)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    TransactionEvents(viewModel)
    val actions = TransactionsActions(viewModel::setMonth, viewModel::setFilter, onOpenTransaction, viewModel::delete)

    ScreenColumn(title = stringResource(R.string.nav_transactions), scaffoldPadding = scaffoldPadding) {
        item { MonthSelector(state.month, actions.onMonthChange) }
        item { MonthTotalsRow(state.totals) }
        item { SearchField(state.filter.query, viewModel::setQuery) }
        item { FilterRow(state.filter, state.catalog, actions.onFilterChange) }
        if (!state.isLoading && state.days.isEmpty()) {
            item { TransactionsEmpty(filtered = state.filter.isActive) }
        }
        dayGroups(state.days, actions)
    }
}

/** Mostra "excluído — Desfazer" e erros vindos do ViewModel. */
@Composable
private fun TransactionEvents(viewModel: TransactionsViewModel) {
    val snackbar = LocalSnackbarHostState.current
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is TransactionsEvent.Error -> snackbar.showSnackbar(event.message)
                is TransactionsEvent.Deleted -> {
                    val result = snackbar.showSnackbar(
                        message = context.getString(R.string.transaction_deleted, event.description),
                        actionLabel = context.getString(R.string.action_undo),
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete()
                }
            }
        }
    }
}

@Composable
private fun MonthTotalsRow(totals: MonthTotals) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        LabeledAmount(stringResource(R.string.home_income), totals.incomeCents, AmountTone.Income)
        LabeledAmount(stringResource(R.string.home_expenses), totals.expenseCents, AmountTone.Expense)
        LabeledAmount(stringResource(R.string.month_result), totals.resultCents, AmountTone.BySign)
    }
}

@Composable
private fun LabeledAmount(label: String, cents: Long, tone: AmountTone) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        MoneyText(cents = cents, tone = tone)
    }
}

@Composable
private fun TransactionsEmpty(filtered: Boolean) {
    if (filtered) {
        EmptyState(
            Icons.Outlined.SearchOff,
            stringResource(R.string.transactions_no_match_title),
            stringResource(R.string.transactions_no_match_body),
        )
    } else {
        EmptyState(
            Icons.AutoMirrored.Outlined.ReceiptLong,
            stringResource(R.string.transactions_empty_title),
            stringResource(R.string.transactions_empty_body),
        )
    }
}

private fun LazyListScope.dayGroups(days: List<DayGroup>, actions: TransactionsActions) {
    days.forEach { day ->
        item(key = "day-${day.date}") {
            Text(
                Dates.dayHeader(day.date),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        items(day.items, key = { it.id }) { item -> SwipeableRow(item, actions) }
    }
}

/**
 * Arrastar da direita para a esquerda exclui (com opção de desfazer).
 * O estado do arraste usa `remember`, não o salvo: se o item voltar pelo "Desfazer"
 * com o mesmo id, ele precisa reaparecer parado — um estado "dispensado" restaurado
 * dispararia a exclusão de novo.
 */
@Composable
private fun SwipeableRow(item: TransactionItem, actions: TransactionsActions) {
    val swipeState = remember { SwipeToDismissBoxState(SwipeToDismissBoxValue.Settled, positionalThreshold = { it * 0.5f }) }
    SwipeToDismissBox(
        state = swipeState,
        enableDismissFromStartToEnd = false,
        onDismiss = { if (it == SwipeToDismissBoxValue.EndToStart) actions.onDelete(item) },
        backgroundContent = { DeleteBackground() },
        modifier = Modifier.clip(MaterialTheme.shapes.large),
    ) {
        Box(Modifier.background(MaterialTheme.colorScheme.surfaceContainer)) {
            TransactionRow(item, onClick = { actions.onOpen(item.id) })
        }
    }
}

@Composable
private fun DeleteBackground() {
    Box(
        Modifier
            .fillMaxSize()
            .background(FinanceTheme.colors.expenseContainer)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Icon(Icons.Outlined.Delete, stringResource(R.string.action_delete), tint = FinanceTheme.colors.expense)
    }
}
