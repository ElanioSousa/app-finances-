package com.example.app_finances.feature.recurring

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.example.app_finances.R
import com.example.app_finances.core.designsystem.categoryIcon
import com.example.app_finances.core.designsystem.component.CategoryIcon
import com.example.app_finances.core.designsystem.component.ListRow
import com.example.app_finances.core.designsystem.component.MoneyText
import com.example.app_finances.core.designsystem.component.RowText
import com.example.app_finances.core.designsystem.theme.FinanceTheme
import com.example.app_finances.core.format.Dates
import com.example.app_finances.core.ui.labelRes
import com.example.app_finances.core.ui.tone
import com.example.app_finances.data.local.entity.RecurringEntity

data class RecurringRowActions(
    val onEdit: (RecurringEntity) -> Unit,
    val onSetActive: (RecurringEntity, Boolean) -> Unit,
    val onDelete: (RecurringEntity) -> Unit,
)

/** Linha de uma recorrência; pausada fica esmaecida. */
@Composable
fun RecurringRowItem(row: RecurringRow, actions: RecurringRowActions) {
    val recurring = row.recurring
    ListRow(
        text = RowText(recurring.description, subtitle(row)),
        onClick = { actions.onEdit(recurring) },
        modifier = Modifier
            .clip(MaterialTheme.shapes.large)
            .alpha(if (recurring.active) 1f else 0.55f),
        leading = {
            val color = row.category?.let { Color(it.colorArgb) } ?: MaterialTheme.colorScheme.onSurfaceVariant
            CategoryIcon(categoryIcon(row.category?.iconKey), color)
        },
        trailing = {
            Column(horizontalAlignment = Alignment.End) {
                MoneyText(recurring.amountCents, tone = recurring.type.tone)
                if (!recurring.active) {
                    Text(stringResource(R.string.recurring_paused), style = MaterialTheme.typography.labelSmall, color = FinanceTheme.colors.warning)
                }
            }
            RowMenu(recurring, actions)
        },
    )
}

/** "Mensal · próxima 30/11/2026 · Carteira". */
@Composable
private fun subtitle(row: RecurringRow): String {
    val next = row.nextDate?.let { stringResource(R.string.recurring_next, Dates.short(it)) }
        ?: stringResource(R.string.recurring_ended)
    return listOf(stringResource(row.recurring.frequency.labelRes), next, row.originName)
        .filter { it.isNotEmpty() }
        .joinToString(" · ")
}

@Composable
private fun RowMenu(recurring: RecurringEntity, actions: RecurringRowActions) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) { Icon(Icons.Filled.MoreVert, stringResource(R.string.recurring_more_actions)) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            val pauseLabel = if (recurring.active) R.string.recurring_pause else R.string.recurring_resume
            DropdownMenuItem(
                text = { Text(stringResource(pauseLabel)) },
                onClick = { expanded = false; actions.onSetActive(recurring, !recurring.active) },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_delete)) },
                onClick = { expanded = false; actions.onDelete(recurring) },
            )
        }
    }
}
