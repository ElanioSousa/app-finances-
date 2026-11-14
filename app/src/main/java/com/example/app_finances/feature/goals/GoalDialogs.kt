package com.example.app_finances.feature.goals

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.app_finances.R
import com.example.app_finances.core.designsystem.component.AmountInput
import com.example.app_finances.core.designsystem.component.ColorPicker
import com.example.app_finances.core.designsystem.component.ConfirmDialog
import com.example.app_finances.core.designsystem.component.ConfirmTexts
import com.example.app_finances.core.designsystem.component.DateField
import com.example.app_finances.core.designsystem.component.DialogActions
import com.example.app_finances.core.designsystem.component.FormDialog
import com.example.app_finances.core.designsystem.component.LabeledSwitch
import com.example.app_finances.core.designsystem.component.TextInput
import com.example.app_finances.core.ui.Editing

/** Os diálogos abertos da tela de metas. */
@Composable
fun GoalsDialogs(dialogs: GoalDialogs, viewModel: GoalsViewModel) {
    dialogs.editor?.let { GoalFormDialog(it, viewModel::edit, DialogActions(viewModel::save, viewModel::close)) }
    dialogs.move?.let { MoveDialog(it, viewModel::editMove, DialogActions(viewModel::saveMove, viewModel::closeMove)) }
    dialogs.confirmDelete?.let { goal ->
        val texts = ConfirmTexts(
            stringResource(R.string.goals_delete_title),
            stringResource(R.string.goals_delete_body, goal.name),
            stringResource(R.string.action_delete),
        )
        ConfirmDialog(texts, DialogActions(viewModel::confirmDelete, viewModel::cancelDelete))
    }
}

@Composable
private fun GoalFormDialog(editing: Editing<GoalEditor>, onEdit: ((GoalEditor) -> GoalEditor) -> Unit, actions: DialogActions) {
    val editor = editing.value
    val title = stringResource(if (editor.original == null) R.string.goals_new else R.string.goals_edit)
    FormDialog(title, editing, actions) {
        TextInput(editor.name, stringResource(R.string.goals_name)) { name -> onEdit { it.copy(name = name) } }
        AmountInput(editor.targetCents, { cents -> onEdit { it.copy(targetCents = cents) } }, stringResource(R.string.goals_target), Modifier.fillMaxWidth())
        LabeledSwitch(stringResource(R.string.goals_has_deadline), editor.hasDeadline) { on -> onEdit { it.copy(hasDeadline = on) } }
        if (editor.hasDeadline) DateField(editor.deadline, { date -> onEdit { it.copy(deadline = date) } })
        ColorPicker(editor.colorArgb) { color -> onEdit { it.copy(colorArgb = color) } }
    }
}

@Composable
private fun MoveDialog(editing: Editing<MoveEditor>, onEdit: ((MoveEditor) -> MoveEditor) -> Unit, actions: DialogActions) {
    val move = editing.value
    val label = stringResource(if (move.withdraw) R.string.goals_withdraw else R.string.goals_deposit)
    FormDialog("$label · ${move.goal.name}", editing, actions, confirmLabel = label) {
        AmountInput(move.amountCents, { cents -> onEdit { it.copy(amountCents = cents) } }, stringResource(R.string.form_amount), Modifier.fillMaxWidth())
    }
}
