package com.example.app_finances.feature.transactions

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.app_finances.R
import com.example.app_finances.core.designsystem.component.Option
import com.example.app_finances.core.model.Catalog
import com.example.app_finances.core.ui.TransactionTypesInDisplayOrder
import com.example.app_finances.core.ui.pluralLabelRes
import com.example.app_finances.data.local.entity.TransactionType

@Composable
fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(R.string.search_hint)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Close, stringResource(R.string.action_clear))
                }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.large,
    )
}

/** Chips de tipo, categoria e conta numa faixa rolável. */
@Composable
fun FilterRow(filter: TransactionFilter, catalog: Catalog, onFilterChange: (TransactionFilter) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        typeOptions().forEach { option ->
            FilterChip(
                selected = filter.type == option.value,
                onClick = { onFilterChange(filter.copy(type = option.value)) },
                label = { Text(option.label) },
            )
        }
        val categoryOptions = remember(catalog.categories) { catalog.categories.map { Option(it.id, it.name) } }
        val accountOptions = remember(catalog.accounts) { catalog.accounts.map { Option(it.id, it.name) } }
        DropdownChip(stringResource(R.string.filter_category), categoryOptions, filter.categoryId) {
            onFilterChange(filter.copy(categoryId = it))
        }
        DropdownChip(stringResource(R.string.filter_account), accountOptions, filter.accountId) {
            onFilterChange(filter.copy(accountId = it))
        }
    }
}

@Composable
private fun typeOptions(): List<Option<TransactionType?>> =
    listOf<Option<TransactionType?>>(Option(null, stringResource(R.string.filter_all))) +
        TransactionTypesInDisplayOrder.map { Option(it, stringResource(it.pluralLabelRes)) }

/** Chip que abre um menu; mostra a opção escolhida no lugar do rótulo. */
@Composable
private fun DropdownChip(
    label: String,
    options: List<Option<Long>>,
    selected: Long?,
    onSelect: (Long?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = selected != null,
            onClick = { expanded = true },
            label = { Text(options.firstOrNull { it.value == selected }?.label ?: label) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.filter_any)) }, onClick = { onSelect(null); expanded = false })
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option.label) }, onClick = { onSelect(option.value); expanded = false })
            }
        }
    }
}
