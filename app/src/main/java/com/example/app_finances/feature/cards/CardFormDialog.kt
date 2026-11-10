package com.example.app_finances.feature.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.app_finances.R
import com.example.app_finances.core.designsystem.component.AmountInput
import com.example.app_finances.core.designsystem.component.ColorPicker
import com.example.app_finances.core.designsystem.component.DialogActions
import com.example.app_finances.core.designsystem.component.FormDialog
import com.example.app_finances.core.designsystem.component.Option
import com.example.app_finances.core.designsystem.component.OptionDropdown
import com.example.app_finances.core.designsystem.component.TextInput
import com.example.app_finances.core.ui.Editing

data class CardFormActions(
    val onEdit: ((CardEditor) -> CardEditor) -> Unit,
    val dialog: DialogActions,
)

@Composable
fun CardFormDialog(editing: Editing<CardEditor>, accountOptions: List<Option<Long>>, actions: CardFormActions) {
    val editor = editing.value
    val onEdit = actions.onEdit
    val title = stringResource(if (editor.original == null) R.string.cards_new else R.string.cards_edit)
    FormDialog(title, editing, actions.dialog) {
        TextInput(editor.name, stringResource(R.string.card_name)) { v -> onEdit { it.copy(name = v) } }
        TextInput(editor.brand, stringResource(R.string.card_brand)) { v -> onEdit { it.copy(brand = v) } }
        AmountInput(
            cents = editor.limitCents,
            onCentsChange = { cents -> onEdit { it.copy(limitCents = cents) } },
            label = stringResource(R.string.card_limit),
            modifier = Modifier.fillMaxWidth(),
        )
        DayFields(editor, onEdit)
        OptionDropdown(stringResource(R.string.card_payment_account), accountOptions, editor.paymentAccountId, { id -> onEdit { it.copy(paymentAccountId = id) } })
        ColorPicker(editor.colorArgb) { color -> onEdit { it.copy(colorArgb = color) } }
    }
}

@Composable
private fun DayFields(editor: CardEditor, onEdit: ((CardEditor) -> CardEditor) -> Unit) {
    val dayTemplate = stringResource(R.string.card_day)
    val days = remember(dayTemplate) { (1..31).map { Option(it, dayTemplate.format(it)) } }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OptionDropdown(stringResource(R.string.card_closing_day), days, editor.closingDay, { d -> onEdit { it.copy(closingDay = d) } }, Modifier.weight(1f))
        OptionDropdown(stringResource(R.string.card_due_day), days, editor.dueDay, { d -> onEdit { it.copy(dueDay = d) } }, Modifier.weight(1f))
    }
}
