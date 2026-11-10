package com.example.app_finances.feature.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.app_finances.R
import com.example.app_finances.core.designsystem.component.MoneyText
import com.example.app_finances.core.designsystem.theme.FinanceTheme
import com.example.app_finances.core.designsystem.theme.Ink850
import com.example.app_finances.core.format.Dates
import com.example.app_finances.core.format.Money
import com.example.app_finances.domain.CardSummary

data class CardTileActions(val onOpen: () -> Unit, val onEdit: () -> Unit)

/** Cartão "de plástico": cor do cartão, fatura atual, vencimento e limite. */
@Composable
fun CardTile(summary: CardSummary, actions: CardTileActions) {
    val cardColor = Color(summary.card.colorArgb)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(Brush.linearGradient(listOf(cardColor.copy(alpha = 0.55f), Ink850)))
            .clickable(onClick = actions.onOpen)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CardTitleRow(summary, actions.onEdit)
        CurrentInvoice(summary)
        summary.pending?.let {
            Text(
                stringResource(R.string.card_pending_invoice, Money.format(it.remainingCents)),
                style = MaterialTheme.typography.bodySmall,
                color = FinanceTheme.colors.warning,
            )
        }
        LimitSummary(summary)
    }
}

@Composable
private fun CardTitleRow(summary: CardSummary, onEdit: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(summary.card.name, style = MaterialTheme.typography.titleLarge)
            summary.card.brand?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, stringResource(R.string.cards_edit)) }
    }
}

@Composable
private fun CurrentInvoice(summary: CardSummary) {
    val invoice = summary.current
    Row(verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.card_current_invoice), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            MoneyText(invoice.totalCents, style = MaterialTheme.typography.headlineSmall)
        }
        Column(horizontalAlignment = Alignment.End) {
            InvoiceStatusChip(summary.currentStatus)
            Text(
                stringResource(R.string.card_due_on, Dates.short(invoice.dueDate)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
