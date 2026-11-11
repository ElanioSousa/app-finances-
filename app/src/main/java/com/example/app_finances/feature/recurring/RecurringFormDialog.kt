package com.example.app_finances.feature.recurring

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.app_finances.R
import com.example.app_finances.core.designsystem.component.AmountInput
import com.example.app_finances.core.designsystem.component.CountInput
import com.example.app_finances.core.designsystem.component.DateField
import com.example.app_finances.core.designsystem.component.DialogActions
import com.example.app_finances.core.designsystem.component.FormDialog
import com.example.app_finances.core.designsystem.component.LabeledSwitch
import com.example.app_finances.core.designsystem.component.Option
import com.example.app_finances.core.designsystem.component.OptionDropdown
import com.example.app_finances.core.designsystem.component.OptionSegmentedRow
import com.example.app_finances.core.designsystem.component.TextInput
import com.example.app_finances.core.model.Catalog
import com.example.app_finances.core.model.toOptions
import com.example.app_finances.core.ui.Editing
import com.example.app_finances.core.ui.labelRes
import com.example.app_finances.core.ui.paymentOriginOptions
import com.example.app_finances.data.local.entity.Frequency
import com.example.app_finances.data.local.entity.TransactionType
import com.example.app_finances.domain.RecurrenceScheduler

data class RecurringFormActions(
    val onEdit: ((RecurringEditor) -> RecurringEditor) -> Unit,
    val dialog: DialogActions,
)

@Composable
fun RecurringFormDialog(editing: Editing<RecurringEditor>, catalog: Catalog, actions: RecurringFormActions) {
    val editor = editing.value
    val onEdit = actions.onEdit
    val title = stringResource(if (editor.original == null) R.string.recurring_new else R.string.recurring_edit)
    FormDialog(title, editing, actions.dialog) {
        OptionSegmentedRow(typeOptions(), editor.type) { type -> onEdit { it.withType(type) } }
        AmountInput(
            cents = editor.amountCents,
            onCentsChange = { cents -> onEdit { it.copy(amountCents = cents) } },
            label = stringResource(R.string.form_amount),
            modifier = Modifier.fillMaxWidth(),
        )
        TextInput(editor.description, stringResource(R.string.form_description)) { v -> onEdit { it.copy(description = v) } }
        CategoryField(editor, catalog, actions)
        OriginFields(editor, catalog, actions)
        OptionDropdown(
            stringResource(R.string.recurring_frequency),
            frequencyOptions(),
            editor.frequency,
            { f -> onEdit { it.copy(frequency = f) } },
        )
        Text(stringResource(R.string.recurring_first_date), style = MaterialTheme.typography.labelLarge)
        DateField(editor.startDate, { date -> onEdit { it.copy(startDate = date) } })
        TermFields(editor, onEdit)
        LabeledSwitch(stringResource(R.string.recurring_is_subscription), editor.isSubscription) { checked ->
            onEdit { it.copy(isSubscription = checked) }
        }
    }
}

/** Sem prazo (aluguel, salário) ou com um número de parcelas (empréstimo). */
@Composable
private fun TermFields(editor: RecurringEditor, onEdit: ((RecurringEditor) -> RecurringEditor) -> Unit) {
    val options = listOf(
        Option(false, stringResource(R.string.recurring_term_none)),
        Option(true, stringResource(R.string.recurring_term_installments)),
    )
    OptionDropdown(stringResource(R.string.recurring_term), options, editor.hasTerm, { has -> onEdit { it.copy(hasTerm = has) } })
    if (!editor.hasTerm) return
    CountInput(
        value = editor.occurrences,
        label = stringResource(R.string.recurring_occurrences),
        max = RecurrenceScheduler.MAX_OCCURRENCES,
        hint = stringResource(R.string.recurring_occurrences_hint),
    ) { count -> onEdit { it.copy(occurrences = count) } }
}

@Composable
private fun typeOptions(): List<Option<TransactionType>> =
    listOf(TransactionType.EXPENSE, TransactionType.INCOME).map { Option(it, stringResource(it.labelRes)) }

@Composable
private fun frequencyOptions(): List<Option<Frequency>> = Frequency.entries.map { Option(it, stringResource(it.labelRes)) }

@Composable
private fun CategoryField(editor: RecurringEditor, catalog: Catalog, actions: RecurringFormActions) {
    val none = stringResource(R.string.category_none)
    val options = remember(catalog, editor.type, none) {
        listOf(Option<Long?>(null, none)) + catalog.categoriesFor(editor.type).map { Option<Long?>(it.id, it.name) }
    }
    OptionDropdown(stringResource(R.string.form_category), options, editor.categoryId, { id -> actions.onEdit { it.copy(categoryId = id) } })
}

@Composable
private fun OriginFields(editor: RecurringEditor, catalog: Catalog, actions: RecurringFormActions) {
    val onEdit = actions.onEdit
    if (editor.type == TransactionType.EXPENSE && catalog.cards.isNotEmpty()) {
        OptionSegmentedRow(paymentOriginOptions(), editor.origin) { origin -> onEdit { it.copy(origin = origin) } }
    }
    if (editor.isCard) {
        val cards = catalog.cards.toOptions()
        OptionDropdown(stringResource(R.string.form_card), cards, editor.cardId, { id -> onEdit { it.copy(cardId = id) } })
    } else {
        val accounts = catalog.accounts.toOptions()
        OptionDropdown(stringResource(R.string.form_account), accounts, editor.accountId, { id -> onEdit { it.copy(accountId = id) } })
    }
}

