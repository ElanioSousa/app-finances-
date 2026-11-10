package com.example.app_finances.feature.transactions.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_finances.core.designsystem.component.Option
import com.example.app_finances.core.model.Catalog
import com.example.app_finances.core.model.CatalogSources
import com.example.app_finances.core.model.observeCatalog
import com.example.app_finances.core.model.toOptions
import com.example.app_finances.core.ui.WhileUiSubscribed
import com.example.app_finances.core.ui.runForUserMessage
import com.example.app_finances.data.local.entity.CategoryEntity
import com.example.app_finances.data.local.entity.TransactionEntity
import com.example.app_finances.data.repository.TransactionRepository
import com.example.app_finances.domain.Installments
import com.example.app_finances.domain.categoryKind
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/** Situação do envio, separada do rascunho. */
data class FormStatus(
    val isEditing: Boolean = false,
    /** Até carregar (lançamento em edição ou conta padrão), não dá para salvar. */
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    /** Parcela em edição (só ela muda; origem e cartão ficam travados). */
    val editedInstallment: InstallmentInfo? = null,
)

data class InstallmentInfo(val number: Int, val count: Int)

data class TransactionFormUiState(
    val draft: TransactionDraft = TransactionDraft(),
    val status: FormStatus = FormStatus(),
    val catalog: Catalog = Catalog(),
) {
    /** Só as categorias que o tipo escolhido aceita (regra do domínio). */
    val categoriesForType: List<CategoryEntity>
        get() = catalog.categories.filter { it.kind == draft.type.categoryKind }

    val accountOptions: List<Option<Long>>
        get() = catalog.accounts.toOptions()

    val cardOptions: List<Option<Long>>
        get() = catalog.cards.toOptions()

    /** Parcelamento só ao criar: editar uma parcela mexe só nela. */
    val canChooseInstallments: Boolean
        get() = draft.isCardPurchase && !status.isEditing

    /** Uma parcela pertence ao cartão da compra: não dá para mudar a origem nem o cartão. */
    val canChangeOrigin: Boolean
        get() = status.editedInstallment == null

    /** Valor da 1ª parcela para cada quantidade (é ela que leva os centavos que sobram). */
    val installmentPreview: List<Long>
        get() = (1..Installments.MAX).map { Installments.split(draft.amountCents, it).first() }

    /** Destino de transferência: qualquer conta menos a de origem. */
    val destinationOptions: List<Option<Long>>
        get() = accountOptions.filter { it.value != draft.accountId }
}

/**
 * Formulário de novo lançamento ([editingId] nulo) ou de edição.
 * Vive só enquanto a folha está aberta; confirma apenas depois que o banco aceitar.
 */
class TransactionFormViewModel(
    private val editingId: Long?,
    private val transactionRepository: TransactionRepository,
    private val catalogSources: CatalogSources,
) : ViewModel() {

    private val draft = MutableStateFlow(TransactionDraft())
    private val status = MutableStateFlow(FormStatus(isEditing = editingId != null))
    private val catalog = observeCatalog(catalogSources)
    private val savedEvents = Channel<Unit>(Channel.CONFLATED)
    private var original: TransactionEntity? = null

    val state: StateFlow<TransactionFormUiState> = combine(draft, status, catalog, ::TransactionFormUiState)
        .stateIn(viewModelScope, WhileUiSubscribed, TransactionFormUiState(status = status.value))

    /** Emite uma vez por gravação aceita. */
    val saved: Flow<Unit> = savedEvents.receiveAsFlow()

    init {
        viewModelScope.launch {
            val error = runForUserMessage { load() }
            status.update { it.copy(isLoading = false, error = error) }
        }
    }

    fun update(transform: (TransactionDraft) -> TransactionDraft) {
        draft.update(transform)
        status.update { it.copy(error = null) }
    }

    fun save() {
        if (status.value.isSaving || status.value.isLoading) return
        status.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            val error = runForUserMessage { persist() }
            status.update { it.copy(isSaving = false, error = error) }
            if (error == null) savedEvents.send(Unit)
        }
    }

    private suspend fun load() {
        val existing = editingId?.let { transactionRepository.getById(it) }
        if (existing != null) {
            original = existing
            draft.value = TransactionDraft.from(existing)
            val installment = existing.installmentNumber?.let { InstallmentInfo(it, existing.installmentCount ?: it) }
            status.update { it.copy(editedInstallment = installment) }
        } else {
            val defaults = catalog.first()
            draft.update {
                it.copy(
                    accountId = it.accountId ?: defaults.accounts.firstOrNull()?.id,
                    cardId = it.cardId ?: defaults.cards.firstOrNull()?.id,
                )
            }
        }
    }

    private suspend fun persist() {
        val current = draft.value
        val categoryName = catalog.first().categories.firstOrNull { it.id == current.categoryId }?.name
        val editing = original
        if (editing != null) {
            transactionRepository.update(current.toEntity(editing, categoryName))
        } else {
            transactionRepository.addAll(current.toNewEntities(categoryName) { UUID.randomUUID().toString() })
        }
    }
}
