package com.example.app_finances.feature.cards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_finances.core.designsystem.component.Option
import com.example.app_finances.core.model.toOptions
import com.example.app_finances.core.ui.EditorController
import com.example.app_finances.core.ui.Editing
import com.example.app_finances.core.ui.WhileUiSubscribed
import com.example.app_finances.data.local.entity.CreditCardEntity
import com.example.app_finances.data.repository.AccountRepository
import com.example.app_finances.data.repository.CardRepository
import com.example.app_finances.domain.CardSummary
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

/** Cartão sendo criado ou editado no diálogo. */
data class CardEditor(
    val original: CreditCardEntity? = null,
    val name: String = original?.name.orEmpty(),
    val brand: String = original?.brand.orEmpty(),
    val limitCents: Long = original?.limitCents ?: 0,
    val closingDay: Int = original?.closingDay ?: 5,
    val dueDay: Int = original?.dueDay ?: 12,
    val paymentAccountId: Long? = original?.paymentAccountId,
    val colorArgb: Int = original?.colorArgb ?: DEFAULT_COLOR,
) {
    fun toEntity() = CreditCardEntity(
        id = original?.id ?: 0,
        name = name.trim(),
        brand = brand.trim().ifEmpty { null },
        limitCents = limitCents,
        closingDay = closingDay,
        dueDay = dueDay,
        paymentAccountId = paymentAccountId,
        colorArgb = colorArgb,
        archived = original?.archived ?: false,
    )

    private companion object {
        const val DEFAULT_COLOR = 0xFF8B5CF6.toInt()
    }
}

data class CardsUiState(
    val summaries: List<CardSummary> = emptyList(),
    val accountOptions: List<Option<Long>> = emptyList(),
    val editor: Editing<CardEditor>? = null,
    val isLoading: Boolean = true,
)

class CardsViewModel(
    private val cardRepository: CardRepository,
    accountRepository: AccountRepository,
    today: () -> LocalDate,
) : ViewModel() {

    private val editor = EditorController<CardEditor>(viewModelScope)

    /** Resumos só são refeitos quando os dados mudam, não a cada letra digitada no diálogo. */
    private val summaries = cardRepository.observeCardData().map { data ->
        CardSummary.ofAll(data.cards, data.purchases, data.payments, today())
    }

    val state: StateFlow<CardsUiState> = combine(summaries, accountRepository.observeAccounts(), editor.state) { summaries, accounts, editing ->
        CardsUiState(summaries, accounts.toOptions(), editing, isLoading = false)
    }.stateIn(viewModelScope, WhileUiSubscribed, CardsUiState())

    fun openNew() = editor.open(CardEditor(paymentAccountId = state.value.accountOptions.firstOrNull()?.value))

    fun openEdit(card: CreditCardEntity) = editor.open(CardEditor(original = card))

    fun close() = editor.close()

    fun edit(transform: (CardEditor) -> CardEditor) = editor.edit(transform)

    fun save() = editor.save { draft ->
        val entity = draft.toEntity()
        if (draft.original == null) cardRepository.add(entity) else cardRepository.update(entity)
    }
}
