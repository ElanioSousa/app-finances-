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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.example.app_finances.core.designsystem.component.DateField
import com.example.app_finances.core.designsystem.component.OptionDropdown
import com.example.app_finances.core.ui.containerViewModel
import com.example.app_finances.core.ui.TransactionTypesInDisplayOrder
import com.example.app_finances.core.ui.labelRes
import com.example.app_finances.core.ui.paidLabelRes
import com.example.app_finances.data.local.entity.CategoryEntity
import com.example.app_finances.data.local.entity.TransactionType

private data class FormActions(
    val onUpdate: ((TransactionDraft) -> TransactionDraft) -> Unit,
    val onSave: () -> Unit,
)

/** Folha inferior para criar ([editingId] nulo) ou editar um lançamento. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormSheet(editingId: Long?, onDismiss: () -> Unit) {
    // Dono próprio: o ViewModel nasce com a folha e é descartado quando ela fecha.
    val owner = rememberViewModelStoreOwner()
    val viewModel = containerViewModel(owner) {
        TransactionFormViewModel(editingId, it.transactionRepository, it.accountRepository, it.categoryRepository)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.saved.collect { onDismiss() } }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        TransactionForm(state, FormActions(viewModel::update, viewModel::save))
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
        Text(
            stringResource(if (state.status.isEditing) R.string.form_edit_title else R.string.form_new_title),
            style = MaterialTheme.typography.titleLarge,
        )
        TypeSelector(draft.type) { type -> actions.onUpdate { it.withType(type) } }
        AmountInput(
            cents = draft.amountCents,
            onCentsChange = { cents -> actions.onUpdate { it.copy(amountCents = cents) } },
            label = stringResource(R.string.form_amount),
            modifier = Modifier.fillMaxWidth(),
        )
        if (!draft.isTransfer) {
            CategoryPicker(state.categoriesForType, draft.categoryId) { id -> actions.onUpdate { it.copy(categoryId = id) } }
        }
        DescriptionField(draft.description) { text -> actions.onUpdate { it.copy(description = text) } }
        AccountFields(state, actions)
        DateField(draft.date, { date -> actions.onUpdate { it.copy(date = date) } })
        PaidSwitch(draft, actions)
        NotesField(draft.notes) { text -> actions.onUpdate { it.copy(notes = text) } }
        state.status.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
        SaveButton(state.status, actions.onSave)
    }
}

@Composable
private fun TypeSelector(selected: TransactionType, onSelect: (TransactionType) -> Unit) {
    val types = TransactionTypesInDisplayOrder
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        types.forEachIndexed { index, type ->
            SegmentedButton(
                selected = type == selected,
                onClick = { onSelect(type) },
                shape = SegmentedButtonDefaults.itemShape(index, types.size),
            ) { Text(stringResource(type.labelRes)) }
        }
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
private fun PaidSwitch(draft: TransactionDraft, actions: FormActions) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(draft.type.paidLabelRes), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = draft.paid, onCheckedChange = { paid -> actions.onUpdate { it.copy(paid = paid) } })
    }
}

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
