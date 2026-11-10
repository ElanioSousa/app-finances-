package com.example.app_finances.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Botões segmentados para escolher uma opção (ex.: Despesa | Receita | Transferência). */
@Composable
fun <T> OptionSegmentedRow(options: List<Option<T>>, selected: T, onSelect: (T) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option.value == selected,
                onClick = { onSelect(option.value) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
            ) { Text(option.label) }
        }
    }
}
