package com.example.app_finances.feature.transactions.form

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
import androidx.compose.runtime.remember
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
import com.example.app_finances.core.designsystem.component.ConfirmTexts
import com.example.app_finances.core.designsystem.component.DialogActions
import com.example.app_finances.core.designsystem.component.DateField
import com.example.app_finances.core.designsystem.component.LabeledSwitch
import com.example.app_finances.core.designsystem.component.Option
import com.example.app_finances.core.designsystem.component.OptionDropdown
import com.example.app_finances.core.designsystem.component.OptionSegmentedRow
import com.example.app_finances.core.format.Money
import com.example.app_finances.core.model.PaymentOrigin
import com.example.app_finances.core.ui.TransactionTypesInDisplayOrder
import com.example.app_finances.core.ui.containerViewModel
import com.example.app_finances.core.ui.labelRes
import com.example.app_finances.core.ui.paidLabelRes
import com.example.app_finances.core.ui.paymentOriginOptions
import com.example.app_finances.data.local.entity.CategoryEntity
import com.example.app_finances.data.local.entity.TransactionType

private data class FormActions(
    val onUpdate: ((TransactionDraft) -> TransactionDraft) -> Unit,
    val onSave: () -> Unit,
    val onClose: () -> Unit,
    val onDelete: () -> Unit,
)

/** Folha inferior para criar, lançar compra no cartão ou editar um lançamento (conforme o [request]). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormSheet(request: FormRequest, onDismiss: () -> Unit) {
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
        TransactionForm(state, FormActions(viewModel::update, viewModel::save, onDismiss, viewModel::delete))
    }
}

@Composable
private fun TransactionForm(state: TransactionFormUiState, actions: FormActions) {
    val draft = state.draft
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        FormHeader(state, actions)
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
private fun FormHeader(state: TransactionFormUiState, actions: FormActions) {
    Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(if (state.status.isEditing) R.string.form_edit_title else R.string.form_new_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f),
        )
        if (state.status.isEditing && !state.status.isLoading) DeleteButton(state.status.editedInstallment, actions.onDelete)
        IconButton(onClick = actions.onClose) { Icon(Icons.Filled.Close, stringResource(R.string.action_close)) }
    }
    TypeSelector(state.draft.type) { type -> actions.onUpdate { it.withType(type) } }
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
    if (state.canChooseInstallments) {
        OptionDropdown(
            stringResource(R.string.form_installments),
            installmentOptions(state.installmentPreview),
            draft.installments,
            { count -> actions.onUpdate { it.copy(installments = count) } },
            Modifier.fillMaxWidth(),
        )
    }
    state.status.editedInstallment?.let { info ->
        Text(stringResource(R.string.form_installment_info, info.number, info.count), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** "À vista", "2x de R$ 50,00", … usando o valor real da 1ª parcela de cada opção. */
@Composable
private fun installmentOptions(firstInstallments: List<Long>): List<Option<Int>> {
    val cash = stringResource(R.string.form_cash)
    val template = stringResource(R.string.form_installment_option)
    return remember(firstInstallments, cash, template) {
        firstInstallments.mapIndexed { index, cents ->
            val count = index + 1
            Option(count, if (count == 1) cash else template.format(count, Money.format(cents)))
        }
    }
}

@Composable
private fun TypeSelector(selected: TransactionType, onSelect: (TransactionType) -> Unit) {
    val options = TransactionTypesInDisplayOrder.map { Option(it, stringResource(it.labelRes)) }
    OptionSegmentedRow(options, selected, onSelect)
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
