package com.example.app_finances.feature.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.app_finances.R
import com.example.app_finances.core.designsystem.component.AmountInput
import com.example.app_finances.core.designsystem.component.Option
import com.example.app_finances.core.designsystem.component.OptionDropdown
import com.example.app_finances.core.designsystem.theme.PickerPalette
import com.example.app_finances.data.local.entity.AccountType

data class AccountFormActions(
    val onEdit: ((AccountEditor) -> AccountEditor) -> Unit,
    val onSave: () -> Unit,
    val onDismiss: () -> Unit,
)

@Composable
fun AccountFormDialog(editor: AccountEditor, actions: AccountFormActions) {
    AlertDialog(
        onDismissRequest = actions.onDismiss,
        title = { Text(stringResource(if (editor.original == null) R.string.accounts_new else R.string.accounts_edit)) },
        text = { AccountFields(editor, actions.onEdit) },
        confirmButton = {
            TextButton(onClick = actions.onSave, enabled = !editor.isSaving) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = actions.onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun AccountFields(editor: AccountEditor, onEdit: ((AccountEditor) -> AccountEditor) -> Unit) {
    val typeOptions = AccountType.entries.map { Option(it, stringResource(accountTypeLabel(it))) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = editor.name,
            onValueChange = { name -> onEdit { it.copy(name = name) } },
            label = { Text(stringResource(R.string.account_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OptionDropdown(stringResource(R.string.account_type), typeOptions, editor.type, { type -> onEdit { it.copy(type = type) } })
        AmountInput(
            cents = editor.initialBalanceCents,
            onCentsChange = { cents -> onEdit { it.copy(initialBalanceCents = cents) } },
            label = stringResource(R.string.account_initial_balance),
            modifier = Modifier.fillMaxWidth(),
        )
        ColorPicker(editor.colorArgb) { color -> onEdit { it.copy(colorArgb = color) } }
        editor.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun ColorPicker(selectedArgb: Int, onSelect: (Int) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PickerPalette.forEach { color ->
            val argb = color.toArgb()
            val selected = argb == selectedArgb
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(if (selected) 3.dp else 0.dp, if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape)
                    .clickable { onSelect(argb) },
            )
        }
    }
}
