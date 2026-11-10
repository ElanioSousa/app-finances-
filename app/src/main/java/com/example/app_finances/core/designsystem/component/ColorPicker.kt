package com.example.app_finances.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.example.app_finances.core.designsystem.theme.PickerPalette

/** Bolinhas da paleta do app para escolher a cor de contas, cartões e categorias. */
@Composable
fun ColorPicker(selectedArgb: Int, onSelect: (Int) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PickerPalette.forEach { color ->
            val argb = color.toArgb()
            val selected = argb == selectedArgb
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(if (selected) 3.dp else 0.dp, if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape)
                    .clickable { onSelect(argb) },
            )
        }
    }
}
