package com.example.app_finances.feature.categories

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.app_finances.R
import com.example.app_finances.core.designsystem.CategoryIcons
import com.example.app_finances.core.designsystem.component.AmountInput
import com.example.app_finances.core.designsystem.component.ChoiceCircle
import com.example.app_finances.core.designsystem.component.ChoiceGrid
import com.example.app_finances.core.designsystem.component.ColorPicker
import com.example.app_finances.core.designsystem.component.DialogActions
import com.example.app_finances.core.designsystem.component.FormDialog
import com.example.app_finances.core.designsystem.component.TextInput
import com.example.app_finances.core.ui.Editing
import com.example.app_finances.data.local.entity.CategoryEntity
import com.example.app_finances.data.local.entity.CategoryKind

data class CategoryFormActions(
    val onEdit: ((CategoryEditor) -> CategoryEditor) -> Unit,
    val dialog: DialogActions,
    val onArchive: (CategoryEntity) -> Unit,
)

@Composable
fun CategoryFormDialog(editing: Editing<CategoryEditor>, actions: CategoryFormActions) {
    val editor = editing.value
    val onEdit = actions.onEdit
    val title = stringResource(if (editor.original == null) R.string.categories_new else R.string.categories_edit)
    FormDialog(title, editing, actions.dialog) {
        TextInput(editor.name, stringResource(R.string.category_name)) { v -> onEdit { it.copy(name = v) } }
        Text(stringResource(R.string.category_icon), style = MaterialTheme.typography.labelLarge)
        IconPicker(editor.iconKey, Color(editor.colorArgb)) { key -> onEdit { it.copy(iconKey = key) } }
        ColorPicker(editor.colorArgb) { color -> onEdit { it.copy(colorArgb = color) } }
        if (editor.kind == CategoryKind.EXPENSE) {
            AmountInput(
                cents = editor.budgetCents,
                onCentsChange = { cents -> onEdit { it.copy(budgetCents = cents) } },
                label = stringResource(R.string.category_budget),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(stringResource(R.string.category_budget_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        editor.original?.let { original ->
            TextButton(onClick = { actions.onArchive(original) }) { Text(stringResource(R.string.category_archive), color = MaterialTheme.colorScheme.error) }
        }
    }
}

/** Grade de ícones; o escolhido fica com o fundo da cor da categoria. */
@Composable
private fun IconPicker(selectedKey: String, color: Color, onSelect: (String) -> Unit) {
    ChoiceGrid {
        CategoryIcons.forEach { (key, icon) ->
            val selected = key == selectedKey
            ChoiceCircle(
                selected = selected,
                background = if (selected) color.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceContainerHighest,
                onClick = { onSelect(key) },
                modifier = Modifier.semantics { contentDescription = key },
            ) {
                Icon(icon, contentDescription = null, tint = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
