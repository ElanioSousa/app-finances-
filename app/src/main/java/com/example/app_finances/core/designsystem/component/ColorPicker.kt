package com.example.app_finances.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.app_finances.core.designsystem.theme.PickerPalette

/** Bolinha de escolha (cor, ícone…): a escolhida ganha borda e é anunciada como selecionada. */
@Composable
fun ChoiceCircle(
    selected: Boolean,
    background: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {},
) {
    Box(
        modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(background)
            .border(if (selected) 3.dp else 0.dp, if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { this.selected = selected },
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/** Grade de escolhas, com o mesmo espaçamento em todos os seletores. */
@Composable
fun ChoiceGrid(content: @Composable () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
}

/** Bolinhas da paleta do app para escolher a cor de contas, cartões e categorias. */
@Composable
fun ColorPicker(selectedArgb: Int, onSelect: (Int) -> Unit) {
    ChoiceGrid {
        PickerPalette.forEach { color ->
            val argb = color.toArgb()
            ChoiceCircle(selected = argb == selectedArgb, background = color, onClick = { onSelect(argb) })
        }
    }
}
