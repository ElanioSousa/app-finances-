package com.example.app_finances.feature.categories

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.app_finances.R
import com.example.app_finances.core.ui.CategoryBadge
import com.example.app_finances.core.designsystem.categoryIcon
import com.example.app_finances.core.designsystem.component.CategoryIcon
import com.example.app_finances.core.designsystem.component.ConfirmDialog
import com.example.app_finances.core.designsystem.component.ConfirmTexts
import com.example.app_finances.core.designsystem.component.DialogActions
import com.example.app_finances.core.designsystem.component.ListRow
import com.example.app_finances.core.designsystem.component.Option
import com.example.app_finances.core.designsystem.component.OptionSegmentedRow
import com.example.app_finances.core.designsystem.component.RowText
import com.example.app_finances.core.designsystem.component.ScreenColumn
import com.example.app_finances.core.ui.BudgetRow
import com.example.app_finances.core.ui.LocalSnackbarHostState
import com.example.app_finances.core.ui.containerViewModel
import com.example.app_finances.data.local.entity.CategoryKind

@Composable
fun CategoriesScreen(scaffoldPadding: PaddingValues, onBack: () -> Unit) {
    val viewModel = containerViewModel { CategoriesViewModel(it.categoryRepository, it.transactionRepository, it.today) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarHostState.current
    LaunchedEffect(viewModel) { viewModel.errorFlow.collect { snackbar.showSnackbar(it) } }

    ScreenColumn(title = stringResource(R.string.categories_title), scaffoldPadding = scaffoldPadding, onBack = onBack) {
        item { OptionSegmentedRow(kindOptions(), state.kind, viewModel::setKind) }
        // No topo: com muitas categorias, o botão no fim da lista ficaria escondido.
        item {
            FilledTonalButton(onClick = viewModel::openNew, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(stringResource(R.string.categories_new), Modifier.padding(start = 8.dp))
            }
        }
        items(state.rows, key = { it.category.id }) { row -> CategoryRowItem(row, onClick = { viewModel.openEdit(row.category) }) }
    }
    state.editor?.let { editing ->
        val actions = CategoryFormActions(viewModel::edit, DialogActions(viewModel::save, viewModel::close), viewModel::askArchive)
        CategoryFormDialog(editing, actions)
    }
    state.confirmArchive?.let { ArchiveDialog(DialogActions(viewModel::confirmArchive, viewModel::cancelArchive)) }
}

@Composable
private fun kindOptions(): List<Option<CategoryKind>> = listOf(
    Option(CategoryKind.EXPENSE, stringResource(R.string.filter_expenses)),
    Option(CategoryKind.INCOME, stringResource(R.string.filter_incomes)),
)

/** Despesa com orçamento mostra o andamento do mês; as demais, uma linha simples. */
@Composable
private fun CategoryRowItem(row: CategoryRow, onClick: () -> Unit) {
    val budget = row.budget
    if (budget != null) {
        BudgetRow(budget, Modifier.clip(MaterialTheme.shapes.large).clickable(onClick = onClick).padding(16.dp))
        return
    }
    val category = row.category
    val subtitle = if (category.kind == CategoryKind.EXPENSE) stringResource(R.string.category_no_budget) else stringResource(R.string.type_income)
    ListRow(
        text = RowText(category.name, subtitle),
        onClick = onClick,
        modifier = Modifier.clip(MaterialTheme.shapes.large),
        leading = { CategoryBadge(category, size = 32.dp) },
    )
}

@Composable
private fun ArchiveDialog(actions: DialogActions) {
    val texts = ConfirmTexts(
        title = stringResource(R.string.category_archive_title),
        body = stringResource(R.string.category_archive_body),
        confirmLabel = stringResource(R.string.category_archive),
    )
    ConfirmDialog(texts, actions)
}
