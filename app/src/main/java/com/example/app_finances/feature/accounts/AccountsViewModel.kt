package com.example.app_finances.feature.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_finances.core.ui.EditorController
import com.example.app_finances.core.ui.Editing
import com.example.app_finances.core.ui.WhileUiSubscribed
import com.example.app_finances.data.local.entity.AccountEntity
import com.example.app_finances.data.local.entity.AccountType
import com.example.app_finances.data.repository.AccountRepository
import com.example.app_finances.domain.AccountBalance
import com.example.app_finances.domain.BalanceCalculator
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Conta sendo criada ou editada no diálogo. */
data class AccountEditor(
    val original: AccountEntity? = null,
    val name: String = original?.name.orEmpty(),
    val type: AccountType = original?.type ?: AccountType.CHECKING,
    val initialBalanceCents: Long = original?.initialBalanceCents ?: 0,
    val colorArgb: Int = original?.colorArgb ?: DEFAULT_COLOR,
) {
    fun toEntity(): AccountEntity = (original ?: AccountEntity(name = "", type = type, colorArgb = colorArgb)).copy(
        name = name.trim(),
        type = type,
        initialBalanceCents = initialBalanceCents,
        colorArgb = colorArgb,
    )

    private companion object {
        const val DEFAULT_COLOR = 0xFF60A5FA.toInt()
    }
}

data class AccountsUiState(
    val balances: List<AccountBalance> = emptyList(),
    val editor: Editing<AccountEditor>? = null,
) {
    val totalCents: Long get() = BalanceCalculator.total(balances)
}

class AccountsViewModel(private val accountRepository: AccountRepository) : ViewModel() {

    private val editor = EditorController<AccountEditor>(viewModelScope)

    val state: StateFlow<AccountsUiState> = combine(accountRepository.observeBalances(), editor.state, ::AccountsUiState)
        .stateIn(viewModelScope, WhileUiSubscribed, AccountsUiState())

    fun openNew() = editor.open(AccountEditor())

    fun openEdit(account: AccountEntity) = editor.open(AccountEditor(original = account))

    fun close() = editor.close()

    fun edit(transform: (AccountEditor) -> AccountEditor) = editor.edit(transform)

    fun save() = editor.save { draft ->
        val entity = draft.toEntity()
        if (draft.original == null) accountRepository.add(entity) else accountRepository.update(entity)
    }
}
