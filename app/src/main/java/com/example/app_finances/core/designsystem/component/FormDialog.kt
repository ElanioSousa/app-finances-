package com.example.app_finances.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.app_finances.R
import com.example.app_finances.core.ui.Editing

/** Botões de um diálogo de formulário. */
data class DialogActions(
    val onConfirm: () -> Unit,
    val onDismiss: () -> Unit,
)

/**
 * Diálogo padrão de cadastro: campos roláveis, mensagem de erro e botões
 * Cancelar / [confirmLabel] (desabilitado enquanto salva).
 */
@Composable
fun FormDialog(
    title: String,
    editing: Editing<*>,
    actions: DialogActions,
    confirmLabel: String = stringResource(R.string.action_save),
    content: @Composable ColumnScope.() -> Unit,
) {
    AlertDialog(
        onDismissRequest = actions.onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                content()
                editing.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(onClick = actions.onConfirm, enabled = !editing.isSaving) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = actions.onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

/** Campo de texto de uma linha, largura total. */
@Composable
fun TextInput(value: String, label: String, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true, modifier = Modifier.fillMaxWidth())
}
