package com.example.app_finances.feature.networth

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.app_finances.R
import com.example.app_finances.core.designsystem.component.AmountLine
import com.example.app_finances.core.designsystem.component.AmountTone
import com.example.app_finances.core.designsystem.component.MoneyText
import com.example.app_finances.core.designsystem.component.ScreenColumn
import com.example.app_finances.core.designsystem.component.SectionCard
import com.example.app_finances.core.ui.containerViewModel
import com.example.app_finances.domain.NetWorth

@Composable
fun NetWorthScreen(scaffoldPadding: PaddingValues, onBack: () -> Unit) {
    val viewModel = containerViewModel {
        NetWorthViewModel(NetWorthSources(it.accountRepository, it.transactionRepository, it.cardRepository, it.recurringRepository), it.today)
    }
    val netWorth by viewModel.state.collectAsStateWithLifecycle()
    ScreenColumn(title = stringResource(R.string.networth_title), scaffoldPadding = scaffoldPadding, onBack = onBack) {
        val value = netWorth ?: return@ScreenColumn
        item { NetCard(value) }
        item { AssetsCard(value) }
        item { DebtsCard(value) }
    }
}

@Composable
private fun NetCard(value: NetWorth) {
    SectionCard {
        Text(stringResource(R.string.networth_net), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        MoneyText(value.netCents, tone = AmountTone.BySign, style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.networth_net_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AssetsCard(value: NetWorth) {
    SectionCard(title = stringResource(R.string.networth_assets)) {
        AmountLine(stringResource(R.string.networth_accounts), value.accountsCents)
        AmountLine(stringResource(R.string.networth_receivable), value.receivableCents)
        AmountLine(stringResource(R.string.networth_total), value.assetsCents, emphasized = true)
    }
}

@Composable
private fun DebtsCard(value: NetWorth) {
    SectionCard(title = stringResource(R.string.networth_debts)) {
        value.cards.forEach { AmountLine(stringResource(R.string.networth_card, it.name), it.cents) }
        value.loans.forEach { AmountLine(it.name, it.cents) }
        AmountLine(stringResource(R.string.networth_payable), value.payableCents)
        AmountLine(stringResource(R.string.networth_total), value.debtsCents, emphasized = true)
    }
}
