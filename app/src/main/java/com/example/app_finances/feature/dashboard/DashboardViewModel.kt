package com.example.app_finances.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_finances.core.model.TransactionItem
import com.example.app_finances.core.model.observeCatalog
import com.example.app_finances.core.model.toItems
import com.example.app_finances.core.ui.WhileUiSubscribed
import com.example.app_finances.data.repository.AccountRepository
import com.example.app_finances.data.repository.CategoryRepository
import com.example.app_finances.data.repository.TransactionRepository
import com.example.app_finances.domain.BalanceCalculator
import com.example.app_finances.domain.MonthTotals
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth

/** Topo do Início: saldo de todas as contas e totais do mês atual. */
data class DashboardUiState(
    val balanceCents: Long = 0,
    val monthTotals: MonthTotals = MonthTotals(),
    val recent: List<TransactionItem> = emptyList(),
    val isLoading: Boolean = true,
)

class DashboardViewModel(
    accountRepository: AccountRepository,
    transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
) : ViewModel() {

    val state: StateFlow<DashboardUiState> = combine(
        accountRepository.observeBalances(),
        transactionRepository.observeMonth(YearMonth.now()),
        observeCatalog(categoryRepository, accountRepository),
    ) { balances, monthTransactions, catalog ->
        DashboardUiState(
            balanceCents = BalanceCalculator.total(balances),
            monthTotals = MonthTotals.of(monthTransactions),
            recent = monthTransactions.take(RECENT_COUNT).toItems(catalog),
            isLoading = false,
        )
    }.stateIn(viewModelScope, WhileUiSubscribed, DashboardUiState())

    private companion object {
        const val RECENT_COUNT = 5
    }
}
