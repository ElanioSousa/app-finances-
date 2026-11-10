package com.example.app_finances.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.app_finances.R
import com.example.app_finances.core.format.Dates
import java.time.YearMonth

/** "‹ rótulo ›": navega para o período anterior/seguinte (mês, ano…). */
@Composable
fun PeriodSelector(label: String, onPrevious: () -> Unit, onNext: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.month_previous))
        }
        Text(label, style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.month_next))
        }
    }
}

/** "‹ Novembro de 2026 ›" para navegar entre meses. */
@Composable
fun MonthSelector(month: YearMonth, onMonthChange: (YearMonth) -> Unit, modifier: Modifier = Modifier) =
    PeriodSelector(Dates.month(month), { onMonthChange(month.minusMonths(1)) }, { onMonthChange(month.plusMonths(1)) }, modifier)
