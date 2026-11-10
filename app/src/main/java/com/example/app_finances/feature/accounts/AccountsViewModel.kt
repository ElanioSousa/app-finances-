package com.example.app_finances.feature.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_finances.core.ui.WhileUiSubscribed
import com.example.app_finances.core.ui.runForUserMessage
import com.example.app_finances.data.local.entity.AccountEntity
import com.example.app_finances.data.local.entity.AccountType
import com.example.app_finances.data.repository.AccountRepository
import com.example.app_finances.domain.AccountBalance
import com.example.app_finances.domain.BalanceCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Conta sendo criada ou editada no diálogo. */
data class AccountEditor(
    val original: AccountEntity? = null,
    val name: String = original?.name.orEmpty(),
    val type: AccountType = original?.type ?: AccountType.CHECKING,
    val initialBalanceCents: Long = original?.initialBalanceCents ?: 0,
    val colorArgb: Int = original?.colorArgb ?: DEFAULT_COLOR,
    val isSaving: Boolean = false,
    val error: String? = null,
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
    val editor: AccountEditor? = null,
) {
    val totalCents: Long get() = BalanceCalculator.total(balances)
}

class AccountsViewModel(private val accountRepository: AccountRepository) : ViewModel() {

    private val editor = MutableStateFlow<AccountEditor?>(null)

    val state: StateFlow<AccountsUiState> = combine(accountRepository.observeBalances(), editor, ::AccountsUiState)
        .stateIn(viewModelScope, WhileUiSubscribed, AccountsUiState())

    fun openNew() = editor.update { AccountEditor() }

    fun openEdit(account: AccountEntity) = editor.update { AccountEditor(original = account) }

    fun close() = editor.update { null }

    fun edit(transform: (AccountEditor) -> AccountEditor) = editor.update { it?.let(transform)?.copy(error = null) }

    fun save() {
        val current = editor.value ?: return
        if (current.isSaving) return
        editor.update { it?.copy(isSaving = true) }
        viewModelScope.launch {
            val entity = current.toEntity()
            val error = runForUserMessage {
                if (current.original == null) accountRepository.add(entity) else accountRepository.update(entity)
            }
            editor.update { if (error == null) null else it?.copy(isSaving = false, error = error) }
        }
    }
}
