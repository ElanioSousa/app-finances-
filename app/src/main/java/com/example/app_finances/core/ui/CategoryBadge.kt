package com.example.app_finances.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.app_finances.core.designsystem.categoryIcon
import com.example.app_finances.core.designsystem.component.CategoryIcon
import com.example.app_finances.data.local.entity.CategoryEntity

/** Ícone e cor de uma categoria; sem categoria, o ícone genérico em cinza. */
@Composable
fun CategoryBadge(category: CategoryEntity?, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val color = category?.let { Color(it.colorArgb) } ?: MaterialTheme.colorScheme.onSurfaceVariant
    CategoryIcon(categoryIcon(category?.iconKey), color, modifier, size)
}
