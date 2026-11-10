package com.example.app_finances.feature.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_finances.core.model.Catalog
import com.example.app_finances.core.model.TransactionItem
import com.example.app_finances.core.model.observeCatalog
import com.example.app_finances.core.model.toItems
import com.example.app_finances.core.ui.WhileUiSubscribed
import com.example.app_finances.core.ui.runForUserMessage
import com.example.app_finances.data.local.entity.TransactionEntity
import com.example.app_finances.data.repository.AccountRepository
import com.example.app_finances.data.repository.CategoryRepository
import com.example.app_finances.data.repository.TransactionRepository
import com.example.app_finances.domain.MonthTotals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth

data class TransactionsUiState(
    val month: YearMonth = YearMonth.now(),
    val filter: TransactionFilter = TransactionFilter(),
    val days: List<DayGroup> = emptyList(),
    val totals: MonthTotals = MonthTotals(),
    val catalog: Catalog = Catalog(),
    val isLoading: Boolean = true,
)

/** Avisos de uma só vez para a tela (snackbar). */
sealed interface TransactionsEvent {
    data class Deleted(val description: String) : TransactionsEvent
    data class Error(val message: String) : TransactionsEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsViewModel(
    private val transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    accountRepository: AccountRepository,
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now())
    private val filter = MutableStateFlow(TransactionFilter())
    private val events = Channel<TransactionsEvent>(Channel.BUFFERED)
    private var lastDeleted: TransactionEntity? = null

    val eventFlow: Flow<TransactionsEvent> = events.receiveAsFlow()

    /** Itens e totais do mês: só são refeitos quando os dados mudam, não a cada letra da busca. */
    private val monthData: Flow<MonthData> = combine(
        month.flatMapLatest(transactionRepository::observeMonth),
        observeCatalog(categoryRepository, accountRepository),
    ) { transactions, catalog -> MonthData(transactions.toItems(catalog), MonthTotals.of(transactions), catalog) }

    val state: StateFlow<TransactionsUiState> = combine(month, filter, monthData) { month, filter, data ->
        TransactionsUiState(
            month = month,
            filter = filter,
            days = data.items.filter(filter::matches).groupByDay(),
            totals = data.totals,
            catalog = data.catalog,
            isLoading = false,
        )
    }.stateIn(viewModelScope, WhileUiSubscribed, TransactionsUiState())

    fun setMonth(value: YearMonth) = month.update { value }

    fun setFilter(value: TransactionFilter) = filter.update { value }

    fun setQuery(query: String) = filter.update { it.copy(query = query) }

    fun delete(item: TransactionItem) {
        viewModelScope.launch {
            val error = runForUserMessage { transactionRepository.delete(item.transaction) }
            if (error != null) return@launch events.send(TransactionsEvent.Error(error))
            lastDeleted = item.transaction
            events.send(TransactionsEvent.Deleted(item.transaction.description))
        }
    }

    /** Recoloca o último lançamento apagado, com o mesmo id. */
    fun undoDelete() = viewModelScope.launch {
        val deleted = lastDeleted ?: return@launch
        lastDeleted = null
        runForUserMessage { transactionRepository.add(deleted) }?.let { events.send(TransactionsEvent.Error(it)) }
    }
}

private data class MonthData(val items: List<TransactionItem>, val totals: MonthTotals, val catalog: Catalog)
