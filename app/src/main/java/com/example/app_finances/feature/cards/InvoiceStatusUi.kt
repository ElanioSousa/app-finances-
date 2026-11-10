package com.example.app_finances.feature.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.app_finances.R
import com.example.app_finances.core.designsystem.theme.FinanceTheme
import com.example.app_finances.core.format.Money
import com.example.app_finances.domain.CardSummary
import com.example.app_finances.domain.InvoiceStatus

/** Etiqueta colorida com a situação da fatura. */
@Composable
fun InvoiceStatusChip(status: InvoiceStatus, modifier: Modifier = Modifier) {
    val colors = FinanceTheme.colors
    val (label, color) = when (status) {
        InvoiceStatus.OPEN -> R.string.invoice_open to colors.transfer
        InvoiceStatus.CLOSED -> R.string.invoice_closed to colors.warning
        InvoiceStatus.PAID -> R.string.invoice_paid to colors.income
        InvoiceStatus.OVERDUE -> R.string.invoice_overdue to colors.expense
    }
    Text(
        text = stringResource(label),
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = modifier
            .background(color.copy(alpha = 0.16f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** Barra fina do limite usado; fica vermelha acima de 90%. */
@Composable
fun LimitBar(fraction: Float, modifier: Modifier = Modifier) {
    val color: Color = if (fraction > 0.9f) FinanceTheme.colors.expense else MaterialTheme.colorScheme.primary
    LinearProgressIndicator(
        progress = { fraction.coerceIn(0f, 1f) },
        modifier = modifier,
        color = color,
        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        drawStopIndicator = {},
    )
}

/** Barra do limite com "Disponível X de Y" logo abaixo. */
@Composable
fun LimitSummary(summary: CardSummary, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LimitBar(summary.usedFraction, Modifier.fillMaxWidth())
        Text(
            stringResource(R.string.card_available, Money.format(summary.availableCents), Money.format(summary.card.limitCents)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
