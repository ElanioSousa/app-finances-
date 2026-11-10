package com.example.app_finances.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.app_finances.R
import com.example.app_finances.core.designsystem.component.AmountTone
import com.example.app_finances.core.designsystem.component.MoneyText
import com.example.app_finances.domain.MonthTotals

/** Receitas, despesas e resultado de um período, lado a lado. */
@Composable
fun MonthTotalsRow(totals: MonthTotals) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        LabeledAmount(stringResource(R.string.home_income), totals.incomeCents, AmountTone.Income)
        LabeledAmount(stringResource(R.string.home_expenses), totals.expenseCents, AmountTone.Expense)
        LabeledAmount(stringResource(R.string.month_result), totals.resultCents, AmountTone.BySign)
    }
}

@Composable
private fun LabeledAmount(label: String, cents: Long, tone: AmountTone) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        MoneyText(cents = cents, tone = tone)
    }
}
