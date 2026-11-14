package com.example.app_finances.feature.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.app_finances.R
import com.example.app_finances.core.designsystem.component.EmptyState
import com.example.app_finances.core.designsystem.component.MoneyText
import com.example.app_finances.core.designsystem.component.ScreenColumn
import com.example.app_finances.core.designsystem.component.SectionCard
import com.example.app_finances.core.designsystem.component.ThinBar
import com.example.app_finances.core.designsystem.component.formatMoney
import com.example.app_finances.core.designsystem.theme.FinanceTheme
import com.example.app_finances.core.format.Dates
import com.example.app_finances.core.ui.ShowMessages
import com.example.app_finances.core.ui.containerViewModel
import com.example.app_finances.data.local.entity.GoalEntity
import com.example.app_finances.domain.GoalPlan
import java.time.YearMonth

@Composable
fun GoalsScreen(scaffoldPadding: PaddingValues, onBack: () -> Unit) {
    val viewModel = containerViewModel { GoalsViewModel(it.goalRepository, it.today) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    ShowMessages(viewModel.errorFlow)
    val actions = GoalActions(viewModel::openMove, viewModel::openEdit, viewModel::askDelete)

    ScreenColumn(title = stringResource(R.string.goals_title), scaffoldPadding = scaffoldPadding, onBack = onBack) {
        if (state.isLoading) return@ScreenColumn
        if (state.rows.isEmpty()) {
            item { EmptyState(Icons.Outlined.Savings, stringResource(R.string.goals_empty_title), stringResource(R.string.goals_empty_body)) }
        } else {
            item { SavedTotal(state.savedTotalCents) }
        }
        items(state.rows, key = { it.goal.id }) { row -> GoalCard(row, actions) }
        item {
            FilledTonalButton(onClick = viewModel::openNew, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(stringResource(R.string.goals_new), Modifier.padding(start = 8.dp))
            }
        }
    }
    GoalsDialogs(state.dialogs, viewModel)
}

private data class GoalActions(
    val onMove: (GoalEntity, Boolean) -> Unit,
    val onEdit: (GoalEntity) -> Unit,
    val onDelete: (GoalEntity) -> Unit,
)

@Composable
private fun SavedTotal(cents: Long) {
    SectionCard {
        Text(stringResource(R.string.goals_saved_total), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        MoneyText(cents, style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
private fun GoalCard(row: GoalRow, actions: GoalActions) {
    val goal = row.goal
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(goal.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = { actions.onEdit(goal) }) { Icon(Icons.Outlined.Edit, stringResource(R.string.goals_edit)) }
            IconButton(onClick = { actions.onDelete(goal) }) { Icon(Icons.Outlined.Delete, stringResource(R.string.action_delete)) }
        }
        ThinBar(row.plan.fraction, Color(goal.colorArgb), Modifier.fillMaxWidth())
        Text(stringResource(R.string.goals_progress, formatMoney(goal.savedCents), formatMoney(goal.targetCents)), style = MaterialTheme.typography.bodyMedium)
        PlanLine(row.plan, goal)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { actions.onMove(goal, false) }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.goals_deposit)) }
            OutlinedButton(onClick = { actions.onMove(goal, true) }, enabled = goal.savedCents > 0, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.goals_withdraw))
            }
        }
    }
}

/** "Guarde R$ 500,00 por mês até junho de 2027", "Meta alcançada!"… */
@Composable
private fun PlanLine(plan: GoalPlan, goal: GoalEntity) {
    val deadline = goal.deadline?.let { Dates.month(YearMonth.from(it)) }.orEmpty()
    val (text, color) = when (plan) {
        is GoalPlan.Reached -> stringResource(R.string.goals_reached) to FinanceTheme.colors.income
        is GoalPlan.Open -> stringResource(R.string.goals_remaining, formatMoney(plan.remainingCents)) to MaterialTheme.colorScheme.onSurfaceVariant
        is GoalPlan.OnTrack -> stringResource(R.string.goals_monthly, formatMoney(plan.monthlyCents), deadline) to MaterialTheme.colorScheme.onSurfaceVariant
        is GoalPlan.Late -> stringResource(R.string.goals_late, deadline, formatMoney(plan.remainingCents)) to FinanceTheme.colors.warning
    }
    Text(text, style = MaterialTheme.typography.bodySmall, color = color)
}
