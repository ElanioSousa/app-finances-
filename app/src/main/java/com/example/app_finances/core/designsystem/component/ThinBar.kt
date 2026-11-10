package com.example.app_finances.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Barra fina de progresso (trilho + preenchimento, cantos de 4 dp): limite do cartão, ranking de gastos. */
@Composable
fun ThinBar(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Canvas(modifier.fillMaxWidth().height(8.dp)) {
        val radius = CornerRadius(4.dp.toPx())
        drawRoundRect(track, cornerRadius = radius)
        val width = size.width * fraction.coerceIn(0f, 1f)
        if (width > 0f) drawRoundRect(color, size = Size(width.coerceAtLeast(radius.x * 2), size.height), cornerRadius = radius)
    }
}
