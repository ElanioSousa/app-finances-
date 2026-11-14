package com.example.app_finances.feature.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_finances.core.model.Catalog
import com.example.app_finances.core.model.CatalogSources
import com.example.app_finances.core.model.observeCatalog
import com.example.app_finances.core.ui.EditorController
import com.example.app_finances.core.ui.UserMessages
import com.example.app_finances.core.ui.Editing
import com.example.app_finances.core.ui.WhileUiSubscribed
import com.example.app_finances.data.local.entity.CategoryEntity
import com.example.app_finances.data.local.entity.RecurringEntity
import com.example.app_finances.data.local.entity.TransactionType
import com.example.app_finances.data.repository.RecurringRepository
import com.example.app_finances.domain.RecurrenceScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate

/** Uma recorrência pronta para a lista. */
data class RecurringRow(
    val recurring: RecurringEntity,
    val category: CategoryEntity?,
    val originName: String,
    val nextDate: LocalDate?,
    val monthlyCents: Long,
)

/** Totais mensais do topo da tela (só recorrências ativas). */
data class RecurringTotals(
    val subscriptionsCents: Long = 0,
    val fixedIncomeCents: Long = 0,
    val fixedExpenseCents: Long = 0,
)

/** O que a tela lista, já separado e somado. */
data class RecurringLists(
    val subscriptions: List<RecurringRow> = emptyList(),
    val fixed: List<RecurringRow> = emptyList(),
    val totals: RecurringTotals = RecurringTotals(),
    val catalog: Catalog = Catalog(),
)

data class RecurringUiState(
    val lists: RecurringLists = RecurringLists(),
    val editor: Editing<RecurringEditor>? = null,
    val confirmDelete: RecurringEntity? = null,
    val isLoading: Boolean = true,
)

class RecurringViewModel(
    private val repository: RecurringRepository,
    private val catalogSources: CatalogSources,
    private val today: () -> LocalDate,
    /** Aberto pelo "Novo lançamento": já começa no cadastro de uma recorrência nova. */
    openNewOnStart: Boolean = false,
) : ViewModel() {

    private val editor = EditorController<RecurringEditor>(viewModelScope)
    private val confirmDelete = MutableStateFlow<RecurringEntity?>(null)
    private val messages = UserMessages(viewModelScope)

    /** Falhas de pausar/excluir, para a tela mostrar num aviso. */
    val errorFlow: Flow<String> = messages.flow

    /** Linhas e totais: refeitos só quando os dados mudam (não a cada tecla no diálogo). */
    private val lists: Flow<RecurringLists> =
        combine(repository.observeAll(), observeCatalog(catalogSources)) { list, catalog ->
            val (subscriptions, fixed) = list.map { it.toRow(catalog) }.partition { it.recurring.isSubscription }
            RecurringLists(subscriptions, fixed, totalsOf(subscriptions, fixed), catalog)
        }

    val state: StateFlow<RecurringUiState> = combine(lists, editor.state, confirmDelete) { lists, editing, deleting ->
        RecurringUiState(lists, editing, deleting, isLoading = false)
    }.stateIn(viewModelScope, WhileUiSubscribed, RecurringUiState())

    init {
        if (openNewOnStart) openNew()
    }

    /** Fechou o diálogo (gravado ou cancelado). */
    val editorClosed: Flow<Unit> = editor.closed

    /** Espera o catálogo carregar, para já sugerir a conta e o cartão. */
    fun openNew() = messages.launch { editor.open(RecurringEditor.new(observeCatalog(catalogSources).first(), today())) }

    fun openEdit(recurring: RecurringEntity) = editor.open(RecurringEditor.from(recurring))

    fun edit(transform: (RecurringEditor) -> RecurringEditor) = editor.edit(transform)

    fun close() = editor.close()

    fun save() = editor.save { draft ->
        val entity = draft.toEntity()
        if (draft.original == null) repository.add(entity) else repository.update(entity)
    }

    fun setActive(recurring: RecurringEntity, active: Boolean) = messages.launch { repository.setActive(recurring, active) }

    fun askDelete(recurring: RecurringEntity) = confirmDelete.update { recurring }

    fun cancelDelete() = confirmDelete.update { null }

    fun confirmDelete() {
        val recurring = confirmDelete.value ?: return
        confirmDelete.update { null }
        messages.launch { repository.delete(recurring) }
    }

    private fun RecurringEntity.toRow(catalog: Catalog) = RecurringRow(
        recurring = this,
        category = categoryId?.let(catalog.categoryById::get),
        originName = cardId?.let(catalog.cardNames::get) ?: accountId?.let(catalog.accountNames::get).orEmpty(),
        nextDate = RecurrenceScheduler.nextOccurrence(this, today()),
        monthlyCents = RecurrenceScheduler.monthlyEquivalent(this),
    )

    /** Só recorrências ativas entram nos totais mensais. */
    private fun totalsOf(subscriptions: List<RecurringRow>, fixed: List<RecurringRow>): RecurringTotals {
        fun List<RecurringRow>.activeSum(type: TransactionType? = null) =
            filter { it.recurring.active && (type == null || it.recurring.type == type) }.sumOf { it.monthlyCents }
        return RecurringTotals(
            subscriptionsCents = subscriptions.activeSum(),
            fixedIncomeCents = fixed.activeSum(TransactionType.INCOME),
            fixedExpenseCents = fixed.activeSum(TransactionType.EXPENSE),
        )
    }
}
