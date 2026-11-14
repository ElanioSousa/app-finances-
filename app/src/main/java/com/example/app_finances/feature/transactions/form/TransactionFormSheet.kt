package com.example.app_finances.feature.transactions.form

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreOwner
import com.example.app_finances.R
import com.example.app_finances.core.designsystem.categoryIcon
import com.example.app_finances.core.designsystem.component.AmountInput
import com.example.app_finances.core.designsystem.component.ConfirmDialog
import com.example.app_finances.core.designsystem.component.CountInput
import com.example.app_finances.core.designsystem.component.ConfirmTexts
import com.example.app_finances.core.designsystem.component.DialogActions
import com.example.app_finances.core.designsystem.component.DateField
import com.example.app_finances.core.designsystem.component.LabeledSwitch
import com.example.app_finances.core.designsystem.component.Option
import com.example.app_finances.core.designsystem.component.OptionDropdown
import com.example.app_finances.core.designsystem.component.OptionSegmentedRow
import com.example.app_finances.core.format.Money
import com.example.app_finances.core.model.PaymentOrigin
import com.example.app_finances.core.ui.containerViewModel
import com.example.app_finances.core.ui.labelRes
import com.example.app_finances.core.ui.paidLabelRes
import com.example.app_finances.core.ui.paymentOriginOptions
import com.example.app_finances.data.local.entity.CategoryEntity
import com.example.app_finances.data.local.entity.TransactionType
import com.example.app_finances.domain.Installments

private data class FormActions(
    val onUpdate: ((TransactionDraft) -> TransactionDraft) -> Unit,
    val onSave: () -> Unit,
    val onClose: () -> Unit,
    val onDelete: () -> Unit,
    val onQuickText: (String) -> Unit,
    /** "Recorrência" no tipo: troca este formulário pelo cadastro de recorrência/assinatura. */
    val onNewRecurring: () -> Unit,
)

/**
 * Folha inferior para criar, lançar compra no cartão, transferir ou editar um lançamento (conforme o [request]).
 * Escolher "Recorrência" no tipo fecha a folha e chama [onNewRecurring].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormSheet(request: FormRequest, onDismiss: () -> Unit, onNewRecurring: () -> Unit) {
    // Dono próprio: o ViewModel nasce com a folha e é descartado quando ela fecha.
    val owner = rememberViewModelStoreOwner()
    val viewModel = containerViewModel(owner) {
        TransactionFormViewModel(request, it.transactionRepository, it.catalogSources)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.saved.collect { onDismiss() } }

    // A folha não responde a arrastes: o formulário tem quase a altura da tela, e a folha
    // disputando o gesto com a rolagem do conteúdo fazia a tela tremer. Fecha pelo ✕,
    // pelo botão voltar ou tocando fora — e não fecha mais por acidente ao rolar.
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        sheetGesturesEnabled = false,
        dragHandle = null,
    ) {
        val actions = FormActions(viewModel::update, viewModel::save, onDismiss, viewModel::delete, viewModel::applyQuickText, onNewRecurring)
        TransactionForm(state, actions, startWithVoice = request is FormRequest.Voice)
    }
}

@Composable
private fun TransactionForm(state: TransactionFormUiState, actions: FormActions, startWithVoice: Boolean) {
    val draft = state.draft
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        FormHeader(state, actions, startWithVoice)
        if (!draft.isTransfer) {
            CategoryPicker(state.categoriesForType, draft.categoryId) { id -> actions.onUpdate { it.copy(categoryId = id) } }
        }
        DescriptionField(draft.description) { text -> actions.onUpdate { it.copy(description = text) } }
        OriginSection(state, actions)
        DateField(draft.date, { date -> actions.onUpdate { it.copy(date = date) } })
        if (!draft.isCardPurchase) PaidSwitch(draft, actions)
        NotesField(draft.notes) { text -> actions.onUpdate { it.copy(notes = text) } }
        state.status.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
        SaveButton(state.status, actions.onSave)
    }
}

@Composable
private fun FormHeader(state: TransactionFormUiState, actions: FormActions, startWithVoice: Boolean) {
    Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(formTitle(state)),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f),
        )
        if (state.status.isEditing && !state.status.isLoading) DeleteButton(state.status.editedInstallment, actions.onDelete)
        IconButton(onClick = actions.onClose) { Icon(Icons.Filled.Close, stringResource(R.string.action_close)) }
    }
    val isNew = !state.status.isEditing
    // Transferência fica como está (aberta pela tela "Mais"): o texto rápido e o tipo não se aplicam a ela.
    if (!state.draft.isTransfer) {
        if (isNew) QuickEntryField(actions.onQuickText, startWithVoice)
        TypeSelector(state.draft.type, withRecurring = isNew, actions)
    }
    AmountInput(
        cents = state.draft.amountCents,
        onCentsChange = { cents -> actions.onUpdate { it.copy(amountCents = cents) } },
        label = stringResource(R.string.form_amount),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Lixeira do lançamento em edição; pergunta antes (numa parcela, a compra inteira sai). */
@Composable
private fun DeleteButton(installment: InstallmentInfo?, onDelete: () -> Unit) {
    var asking by rememberSaveable { mutableStateOf(false) }
    IconButton(onClick = { asking = true }) {
        Icon(Icons.Outlined.Delete, stringResource(R.string.action_delete), tint = MaterialTheme.colorScheme.error)
    }
    if (!asking) return
    val body = installment?.let { stringResource(R.string.form_delete_installments_body, it.count) }
        ?: stringResource(R.string.form_delete_body)
    ConfirmDialog(
        ConfirmTexts(stringResource(R.string.form_delete_title), body, stringResource(R.string.action_delete)),
        DialogActions(onConfirm = { asking = false; onDelete() }, onDismiss = { asking = false }),
    )
}

/** Despesa pode sair de uma conta ou de um cartão; receita e transferência usam contas. */
@Composable
private fun OriginSection(state: TransactionFormUiState, actions: FormActions) {
    val draft = state.draft
    if (draft.type == TransactionType.EXPENSE && state.canChangeOrigin) {
        OriginSelector(draft.origin) { origin -> actions.onUpdate { it.copy(origin = origin) } }
    }
    if (draft.isCardPurchase) CardFields(state, actions) else AccountFields(state, actions)
}

@Composable
private fun OriginSelector(selected: PaymentOrigin, onSelect: (PaymentOrigin) -> Unit) =
    OptionSegmentedRow(paymentOriginOptions(), selected, onSelect)

@Composable
private fun CardFields(state: TransactionFormUiState, actions: FormActions) {
    if (state.cardOptions.isEmpty()) {
        Text(stringResource(R.string.form_no_cards), color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val draft = state.draft
    if (state.canChangeOrigin) {
        OptionDropdown(stringResource(R.string.form_card), state.cardOptions, draft.cardId, { id -> actions.onUpdate { it.copy(cardId = id) } }, Modifier.fillMaxWidth())
    }
    if (state.canChooseInstallments) InstallmentFields(state, actions)
    state.status.editedInstallment?.let { info ->
        Text(stringResource(R.string.form_installment_info, info.number, info.count), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** "À vista no cartão" ou "Compra parcelada" — e, parcelada, a quantidade de parcelas. */
@Composable
private fun InstallmentFields(state: TransactionFormUiState, actions: FormActions) {
    val draft = state.draft
    val options = listOf(
        Option(false, stringResource(R.string.form_card_cash)),
        Option(true, stringResource(R.string.form_card_installments)),
    )
    OptionDropdown(stringResource(R.string.form_payment), options, draft.inInstallments, { parcelled -> actions.onUpdate { it.withInstallments(parcelled) } }, Modifier.fillMaxWidth())
    if (!draft.inInstallments) return
    val hint = state.firstInstallmentCents?.let { stringResource(R.string.form_installment_option, draft.installments, Money.format(it)) }
        ?: stringResource(R.string.form_installments_range, Installments.MAX)
    CountInput(draft.installments, stringResource(R.string.form_installments_count), Installments.MAX, hint) { count ->
        actions.onUpdate { it.copy(installments = count) }
    }
}

private fun formTitle(state: TransactionFormUiState): Int = when {
    state.status.isEditing -> R.string.form_edit_title
    state.draft.isTransfer -> R.string.form_new_transfer_title
    else -> R.string.form_new_title
}

/** Opções do topo do formulário: Recorrência não é um tipo de lançamento, abre o cadastro dela. */
private enum class EntryKind(val type: TransactionType?, @param:StringRes val labelRes: Int) {
    EXPENSE(TransactionType.EXPENSE, R.string.type_expense),
    INCOME(TransactionType.INCOME, R.string.type_income),
    RECURRING(null, R.string.type_recurring),
}

/** Despesa | Receita | Recorrência (esta só num lançamento novo). */
@Composable
private fun TypeSelector(selected: TransactionType, withRecurring: Boolean, actions: FormActions) {
    val kinds = if (withRecurring) EntryKind.entries else EntryKind.entries - EntryKind.RECURRING
    val options = kinds.map { Option(it, stringResource(it.labelRes)) }
    OptionSegmentedRow(options, kinds.firstOrNull { it.type == selected } ?: EntryKind.EXPENSE) { kind ->
        val type = kind.type
        if (type == null) actions.onNewRecurring() else actions.onUpdate { it.withType(type) }
    }
}

@Composable
private fun CategoryPicker(categories: List<CategoryEntity>, selectedId: Long?, onSelect: (Long?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.form_category), style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(end = 8.dp)) {
            items(categories, key = { it.id }) { category ->
                val selected = category.id == selectedId
                FilterChip(
                    selected = selected,
                    onClick = { onSelect(if (selected) null else category.id) },
                    label = { Text(category.name) },
                    leadingIcon = {
                        Icon(categoryIcon(category.iconKey), contentDescription = null, tint = Color(category.colorArgb), modifier = Modifier.size(18.dp))
                    },
                )
            }
        }
    }
}

@Composable
private fun DescriptionField(value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(R.string.form_description)) },
        placeholder = { Text(stringResource(R.string.form_description_hint)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun AccountFields(state: TransactionFormUiState, actions: FormActions) {
    val draft = state.draft
    val fromLabel = stringResource(if (draft.isTransfer) R.string.form_from_account else R.string.form_account)
    OptionDropdown(fromLabel, state.accountOptions, draft.accountId, { id -> actions.onUpdate { it.copy(accountId = id) } }, Modifier.fillMaxWidth())
    if (draft.isTransfer) {
        OptionDropdown(
            stringResource(R.string.form_to_account),
            state.destinationOptions,
            draft.destinationAccountId,
            { id -> actions.onUpdate { it.copy(destinationAccountId = id) } },
            Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PaidSwitch(draft: TransactionDraft, actions: FormActions) =
    LabeledSwitch(stringResource(draft.type.paidLabelRes), draft.paid) { paid -> actions.onUpdate { it.copy(paid = paid) } }

@Composable
private fun NotesField(value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(R.string.form_notes)) },
        minLines = 2,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SaveButton(status: FormStatus, onSave: () -> Unit) {
    Button(
        onClick = onSave,
        enabled = !status.isSaving && !status.isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .height(52.dp),
    ) {
        if (status.isSaving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        else Text(stringResource(R.string.action_save))
    }
}
