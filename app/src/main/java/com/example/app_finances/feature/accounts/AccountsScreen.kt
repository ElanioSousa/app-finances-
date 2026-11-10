package com.example.app_finances.feature.accounts

import androidx.compose.foundation.layout.PaddingValues
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
import com.example.app_finances.core.designsystem.component.ListRow
import com.example.app_finances.core.designsystem.component.RowText
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
    val account = balance.account
    ListRow(
        text = RowText(account.name, stringResource(accountTypeLabel(account.type))),
        onClick = onClick,
        modifier = Modifier.clip(MaterialTheme.shapes.large),
        leading = { CategoryIcon(accountTypeIcon(account.type), Color(account.colorArgb)) },
        trailing = { MoneyText(balance.balanceCents, tone = if (balance.balanceCents < 0) AmountTone.Expense else AmountTone.Neutral) },
    )
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
