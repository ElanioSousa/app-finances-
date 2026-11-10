package com.example.app_finances.feature.accounts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.app_finances.R
import com.example.app_finances.core.designsystem.component.AmountTone
import com.example.app_finances.core.designsystem.component.CategoryIcon
import com.example.app_finances.core.designsystem.component.DialogActions
import com.example.app_finances.core.designsystem.component.MoneyText
import com.example.app_finances.core.designsystem.component.ScreenColumn
import com.example.app_finances.core.designsystem.component.SectionCard
import com.example.app_finances.core.ui.containerViewModel
import com.example.app_finances.data.local.entity.AccountType
import com.example.app_finances.domain.AccountBalance

@Composable
fun AccountsScreen(scaffoldPadding: PaddingValues, onBack: () -> Unit) {
    val viewModel = containerViewModel { AccountsViewModel(it.accountRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    ScreenColumn(title = stringResource(R.string.accounts_title), scaffoldPadding = scaffoldPadding, onBack = onBack) {
        item { TotalCard(state.totalCents) }
        items(state.balances, key = { it.account.id }) { balance ->
            AccountRow(balance, onClick = { viewModel.openEdit(balance.account) })
        }
        item {
            FilledTonalButton(onClick = viewModel::openNew, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(stringResource(R.string.accounts_new), Modifier.padding(start = 8.dp))
            }
        }
    }
    state.editor?.let { editor ->
        AccountFormDialog(editor, AccountFormActions(viewModel::edit, DialogActions(viewModel::save, viewModel::close)))
    }
}

@Composable
private fun TotalCard(totalCents: Long) {
    SectionCard {
        Text(stringResource(R.string.accounts_total), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        MoneyText(totalCents, tone = AmountTone.BySign, style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
private fun AccountRow(balance: AccountBalance, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CategoryIcon(accountTypeIcon(balance.account.type), Color(balance.account.colorArgb))
        Column(Modifier.weight(1f)) {
            Text(balance.account.name, style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(accountTypeLabel(balance.account.type)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        MoneyText(balance.balanceCents, tone = if (balance.balanceCents < 0) AmountTone.Expense else AmountTone.Neutral)
    }
}

fun accountTypeIcon(type: AccountType): ImageVector = when (type) {
    AccountType.CHECKING -> Icons.Outlined.AccountBalance
    AccountType.SAVINGS -> Icons.Outlined.Savings
    AccountType.CASH -> Icons.Outlined.AccountBalanceWallet
    AccountType.INVESTMENT -> Icons.AutoMirrored.Outlined.ShowChart
}

fun accountTypeLabel(type: AccountType): Int = when (type) {
    AccountType.CHECKING -> R.string.account_type_checking
    AccountType.SAVINGS -> R.string.account_type_savings
    AccountType.CASH -> R.string.account_type_cash
    AccountType.INVESTMENT -> R.string.account_type_investment
}
