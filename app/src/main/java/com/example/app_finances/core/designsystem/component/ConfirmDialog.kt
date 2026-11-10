package com.example.app_finances.core.designsystem.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.app_finances.R

/** Textos de uma confirmação ("Excluir recorrência?", explicação, botão de confirmar). */
data class ConfirmTexts(val title: String, val body: String, val confirmLabel: String)

/** Pergunta antes de uma ação que não dá para desfazer pela tela. */
@Composable
fun ConfirmDialog(texts: ConfirmTexts, actions: DialogActions) {
    AlertDialog(
        onDismissRequest = actions.onDismiss,
        title = { Text(texts.title) },
        text = { Text(texts.body) },
        confirmButton = { TextButton(onClick = actions.onConfirm) { Text(texts.confirmLabel) } },
        dismissButton = { TextButton(onClick = actions.onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
