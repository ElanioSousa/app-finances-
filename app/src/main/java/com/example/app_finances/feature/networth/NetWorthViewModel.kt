package com.example.app_finances.feature.networth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_finances.core.ui.WhileUiSubscribed
import com.example.app_finances.data.repository.AccountRepository
import com.example.app_finances.data.repository.CardRepository
import com.example.app_finances.data.repository.RecurringRepository
import com.example.app_finances.data.repository.TransactionRepository
import com.example.app_finances.domain.BalanceCalculator
import com.example.app_finances.domain.NetWorth
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

/** Repositórios que a tela de patrimônio lê. */
data class NetWorthSources(
    val accounts: AccountRepository,
    val transactions: TransactionRepository,
    val cards: CardRepository,
    val recurrings: RecurringRepository,
)

/** Patrimônio de hoje; nulo enquanto carrega. */
class NetWorthViewModel(sources: NetWorthSources, today: () -> LocalDate) : ViewModel() {

    val state: StateFlow<NetWorth?> = combine(
        sources.accounts.observeBalances(),
        sources.transactions.observeOpenPending(),
        sources.cards.observeCardData(),
        sources.recurrings.observeAll(),
    ) { balances, pending, cards, recurrings ->
        val now = today()
        NetWorth.of(BalanceCalculator.total(balances), pending, cards.summaries(now), recurrings, now)
    }.stateIn(viewModelScope, WhileUiSubscribed, null)
}
