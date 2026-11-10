package com.example.app_finances.feature.accounts

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.app_finances.R
import com.example.app_finances.core.designsystem.component.AmountInput
import com.example.app_finances.core.designsystem.component.ColorPicker
import com.example.app_finances.core.designsystem.component.DialogActions
import com.example.app_finances.core.designsystem.component.FormDialog
import com.example.app_finances.core.designsystem.component.TextInput
import com.example.app_finances.core.ui.Editing
import com.example.app_finances.core.designsystem.component.Option
import com.example.app_finances.core.designsystem.component.OptionDropdown
import com.example.app_finances.data.local.entity.AccountType

data class AccountFormActions(
    val onEdit: ((AccountEditor) -> AccountEditor) -> Unit,
    val dialog: DialogActions,
)

@Composable
fun AccountFormDialog(editing: Editing<AccountEditor>, actions: AccountFormActions) {
    val editor = editing.value
    val title = stringResource(if (editor.original == null) R.string.accounts_new else R.string.accounts_edit)
    FormDialog(title, editing, actions.dialog) {
        val onEdit = actions.onEdit
        TextInput(editor.name, stringResource(R.string.account_name)) { name -> onEdit { it.copy(name = name) } }
        OptionDropdown(stringResource(R.string.account_type), accountTypeOptions(), editor.type, { type -> onEdit { it.copy(type = type) } })
        AmountInput(
            cents = editor.initialBalanceCents,
            onCentsChange = { cents -> onEdit { it.copy(initialBalanceCents = cents) } },
            label = stringResource(R.string.account_initial_balance),
            modifier = Modifier.fillMaxWidth(),
        )
        ColorPicker(editor.colorArgb) { color -> onEdit { it.copy(colorArgb = color) } }
    }
}

@Composable
private fun accountTypeOptions(): List<Option<AccountType>> =
    AccountType.entries.map { Option(it, stringResource(accountTypeLabel(it))) }
