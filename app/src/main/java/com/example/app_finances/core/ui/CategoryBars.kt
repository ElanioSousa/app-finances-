package com.example.app_finances.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.example.app_finances.R
import com.example.app_finances.core.designsystem.categoryIcon
import com.example.app_finances.core.designsystem.chart.RankedBar
import com.example.app_finances.core.designsystem.component.LocalMoneyFormatter
import com.example.app_finances.data.local.entity.CategoryEntity
import com.example.app_finances.domain.CategorySpend

/** Totais por categoria prontos para o ranking (nome, valor, ícone e cor; "Outras"/"Sem categoria"). */
@Composable
fun rememberCategoryBars(totals: List<CategorySpend>, categoryById: Map<Long, CategoryEntity>): List<RankedBar> {
    val other = stringResource(R.string.home_other_categories)
    val none = stringResource(R.string.category_none)
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val money = LocalMoneyFormatter.current
    return remember(totals, categoryById, other, none, muted, money) {
        totals.map { total ->
            val category = total.categoryId?.takeUnless { total.isOther }?.let(categoryById::get)
            RankedBar(
                label = category?.name ?: if (total.isOther) other else none,
                value = money.format(total.cents, false),
                share = total.share,
                icon = categoryIcon(category?.iconKey),
                iconColor = category?.let { Color(it.colorArgb) } ?: muted,
            )
        }
    }
}
