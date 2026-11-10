package com.example.app_finances.feature.recurring

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.app_finances.R
import com.example.app_finances.core.designsystem.component.formatMoney
import com.example.app_finances.core.designsystem.component.AmountTone
import com.example.app_finances.core.designsystem.component.ConfirmDialog
import com.example.app_finances.core.designsystem.component.ConfirmTexts
import com.example.app_finances.core.designsystem.component.DialogActions
import com.example.app_finances.core.designsystem.component.EmptyState
import com.example.app_finances.core.designsystem.component.MoneyText
import com.example.app_finances.core.designsystem.component.ScreenColumn
import com.example.app_finances.core.designsystem.component.SectionCard
import com.example.app_finances.core.ui.LocalSnackbarHostState
import com.example.app_finances.core.ui.containerViewModel

@Composable
fun RecurringScreen(scaffoldPadding: PaddingValues, onBack: () -> Unit) {
    val viewModel = containerViewModel { RecurringViewModel(it.recurringRepository, it.catalogSources, it.today) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarHostState.current
    LaunchedEffect(viewModel) { viewModel.errorFlow.collect { snackbar.showSnackbar(it) } }
    val rowActions = RecurringRowActions(viewModel::openEdit, viewModel::setActive, viewModel::askDelete)

    ScreenColumn(title = stringResource(R.string.recurring_title), scaffoldPadding = scaffoldPadding, onBack = onBack) {
        if (state.isLoading) return@ScreenColumn
        val lists = state.lists
        if (lists.subscriptions.isEmpty() && lists.fixed.isEmpty()) {
            item {
                EmptyState(Icons.Outlined.EventRepeat, stringResource(R.string.recurring_empty_title), stringResource(R.string.recurring_empty_body))
            }
        } else {
            item { TotalsCard(lists.totals) }
        }
        section(R.string.recurring_subscriptions, lists.subscriptions, rowActions)
        section(R.string.recurring_fixed, lists.fixed, rowActions)
        item {
            FilledTonalButton(onClick = viewModel::openNew, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(stringResource(R.string.recurring_new), Modifier.padding(start = 8.dp))
            }
        }
    }
    RecurringDialogs(state, viewModel)
}

@Composable
private fun RecurringDialogs(state: RecurringUiState, viewModel: RecurringViewModel) {
    state.editor?.let { editing ->
        RecurringFormDialog(editing, state.lists.catalog, RecurringFormActions(viewModel::edit, DialogActions(viewModel::save, viewModel::close)))
    }
    if (state.confirmDelete != null) {
        val texts = ConfirmTexts(
            title = stringResource(R.string.recurring_delete_title),
            body = stringResource(R.string.recurring_delete_body),
            confirmLabel = stringResource(R.string.action_delete),
        )
        ConfirmDialog(texts, DialogActions(viewModel::confirmDelete, viewModel::cancelDelete))
    }
}

@Composable
private fun TotalsCard(totals: RecurringTotals) {
    SectionCard {
        Text(stringResource(R.string.recurring_subscriptions_monthly), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        MoneyText(totals.subscriptionsCents, tone = AmountTone.Expense, style = MaterialTheme.typography.headlineMedium)
        Text(
            stringResource(R.string.recurring_fixed_summary, formatMoney(totals.fixedIncomeCents), formatMoney(totals.fixedExpenseCents)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun LazyListScope.section(titleRes: Int, rows: List<RecurringRow>, actions: RecurringRowActions) {
    if (rows.isEmpty()) return
    item(key = "title-$titleRes") {
        Text(stringResource(titleRes), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
    }
    items(rows, key = { it.recurring.id }) { row -> RecurringRowItem(row, actions) }
}
