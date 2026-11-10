package com.example.app_finances.feature.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_finances.core.model.Catalog
import com.example.app_finances.core.model.TransactionItem
import com.example.app_finances.core.model.toItems
import com.example.app_finances.core.ui.UserMessages
import com.example.app_finances.core.ui.WhileUiSubscribed
import com.example.app_finances.data.local.entity.CategoryEntity
import com.example.app_finances.data.local.entity.TransactionEntity
import com.example.app_finances.data.repository.AccountRepository
import com.example.app_finances.data.repository.CardData
import com.example.app_finances.data.repository.CardRepository
import com.example.app_finances.data.repository.CategoryRepository
import com.example.app_finances.data.repository.TransactionRepository
import com.example.app_finances.domain.AccountBalance
import com.example.app_finances.domain.BalanceCalculator
import com.example.app_finances.domain.BudgetStatus
import com.example.app_finances.domain.Budgets
import com.example.app_finances.domain.CardSummary
import com.example.app_finances.domain.CategorySpend
import com.example.app_finances.domain.Insights
import com.example.app_finances.domain.MonthBar
import com.example.app_finances.domain.MonthTotals
import com.example.app_finances.domain.UpcomingBill
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.YearMonth

/** Conta a vencer, com o lançamento já descrito (nulo para fatura de cartão). */
data class UpcomingEntry(val bill: UpcomingBill, val item: TransactionItem?)

/** Tudo o que o Início mostra (exceto a seleção do gráfico). */
data class DashboardData(
    val today: LocalDate,
    val balanceCents: Long = 0,
    val monthTotals: MonthTotals = MonthTotals(),
    val upcoming: List<UpcomingEntry> = emptyList(),
    val spending: List<CategorySpend> = emptyList(),
    /** Para dar nome e cor às categorias (inclusive arquivadas). */
    val categoryById: Map<Long, CategoryEntity> = emptyMap(),
    val budgets: List<BudgetStatus> = emptyList(),
    val history: List<MonthBar> = emptyList(),
    val cards: List<CardSummary> = emptyList(),
    val recent: List<TransactionItem> = emptyList(),
)

data class DashboardUiState(
    /** Nulo enquanto carrega: o Início não mostra "R$ 0,00" provisório. */
    val data: DashboardData? = null,
    /** Mês selecionado no gráfico (índice em `history`); padrão = o mês atual. */
    val selectedMonth: Int = Insights.HISTORY_MONTHS - 1,
)

/** Repositórios que o Início lê. */
data class DashboardSources(
    val accounts: AccountRepository,
    val transactions: TransactionRepository,
    val cards: CardRepository,
    val categories: CategoryRepository,
)

class DashboardViewModel(
    private val sources: DashboardSources,
    private val today: () -> LocalDate,
) : ViewModel() {

    private val selectedMonth = MutableStateFlow(Insights.HISTORY_MONTHS - 1)
    private val messages = UserMessages(viewModelScope)

    /** Falhas ao marcar como pago, para a tela mostrar num aviso. */
    val errorFlow: Flow<String> = messages.flow

    /**
     * Cada consulta é observada uma vez só (o catálogo sai das mesmas contas e cartões);
     * gravações em sequência viram um recálculo (conflate), feito fora da thread da tela.
     */
    private val data: Flow<DashboardData> = observeSnapshot()
        .conflate()
        .map(::buildData)
        .flowOn(Dispatchers.Default)
        .catch { error ->
            Log.e(TAG, "Falha ao montar o Início", error)
            messages.send(LOAD_ERROR)
            emit(DashboardData(today()))
        }

    val state: StateFlow<DashboardUiState> = combine(data, selectedMonth, ::DashboardUiState)
        .stateIn(viewModelScope, WhileUiSubscribed, DashboardUiState())

    fun selectMonth(index: Int) = selectedMonth.update { index }

    fun markPaid(transaction: TransactionEntity) = messages.launch { sources.transactions.markPaid(transaction) }

    private class Snapshot(
        val balances: List<AccountBalance>,
        val history: List<TransactionEntity>,
        val pending: List<TransactionEntity>,
        val cards: CardData,
        val categories: List<CategoryEntity>,
    )

    private fun observeSnapshot(): Flow<Snapshot> {
        val now = today()
        val month = YearMonth.from(now)
        return combine(
            sources.accounts.observeBalances(),
            sources.transactions.observeMonths(month.minusMonths(Insights.HISTORY_MONTHS - 1L), month),
            sources.transactions.observePendingUntil(Insights.upcomingLimit(now)),
            sources.cards.observeCardData(),
            sources.categories.observeAll(),
            ::Snapshot,
        )
    }

    private fun buildData(snapshot: Snapshot): DashboardData {
        val now = today()
        val catalog = Catalog(snapshot.categories, snapshot.balances.map { it.account }, snapshot.cards.cards)
        val thisMonth = snapshot.history.filter { YearMonth.from(it.date) == YearMonth.from(now) }
        val cards = CardSummary.ofAll(snapshot.cards.cards, snapshot.cards.purchases, snapshot.cards.payments, now)
        val pendingItems = snapshot.pending.toItems(catalog).associateBy { it.id }
        return DashboardData(
            today = now,
            balanceCents = BalanceCalculator.total(snapshot.balances),
            monthTotals = MonthTotals.of(thisMonth),
            upcoming = Insights.upcomingBills(snapshot.pending, cards, now).map { bill ->
                UpcomingEntry(bill, (bill as? UpcomingBill.Pending)?.let { pendingItems[it.transaction.id] })
            },
            spending = Insights.spendingByCategory(thisMonth),
            categoryById = catalog.categoryById,
            budgets = Budgets.statuses(snapshot.categories, thisMonth),
            history = Insights.monthlyHistory(snapshot.history, YearMonth.from(now)),
            cards = cards,
            recent = thisMonth.take(RECENT_COUNT).toItems(catalog),
        )
    }

    private companion object {
        const val TAG = "DashboardViewModel"
        const val RECENT_COUNT = 5
        const val LOAD_ERROR = "Não foi possível carregar o Início."
    }
}
