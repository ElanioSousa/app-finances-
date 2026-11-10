package com.example.app_finances.feature.cards

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.app_finances.R
import com.example.app_finances.core.designsystem.component.DialogActions
import com.example.app_finances.core.designsystem.component.EmptyState
import com.example.app_finances.core.designsystem.component.ScreenColumn
import com.example.app_finances.core.ui.containerViewModel

@Composable
fun CardsScreen(scaffoldPadding: PaddingValues, onOpenCard: (Long) -> Unit) {
    val viewModel = containerViewModel { CardsViewModel(it.cardRepository, it.accountRepository, it.today) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    ScreenColumn(title = stringResource(R.string.nav_cards), scaffoldPadding = scaffoldPadding) {
        if (!state.isLoading && state.summaries.isEmpty()) {
            item {
                EmptyState(Icons.Outlined.CreditCard, stringResource(R.string.cards_empty_title), stringResource(R.string.cards_empty_body))
            }
        }
        items(state.summaries, key = { it.card.id }) { summary ->
            CardTile(summary, CardTileActions(onOpen = { onOpenCard(summary.card.id) }, onEdit = { viewModel.openEdit(summary.card) }))
        }
        item {
            FilledTonalButton(onClick = viewModel::openNew, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(stringResource(R.string.cards_new), Modifier.padding(start = 8.dp))
            }
        }
    }
    state.editor?.let { editor ->
        CardFormDialog(editor, state.accountOptions, CardFormActions(viewModel::edit, DialogActions(viewModel::save, viewModel::close)))
    }
}
