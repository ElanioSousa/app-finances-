package com.example.app_finances.feature.cards.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.app_finances.R
import com.example.app_finances.core.designsystem.component.MoneyText
import com.example.app_finances.core.designsystem.component.MonthSelector
import com.example.app_finances.core.designsystem.component.ScreenColumn
import com.example.app_finances.core.designsystem.component.SectionCard
import com.example.app_finances.core.format.Dates
import com.example.app_finances.core.format.Money
import com.example.app_finances.core.ui.ShowMessages
import com.example.app_finances.core.ui.TransactionRow
import com.example.app_finances.core.ui.containerViewModel
import com.example.app_finances.core.designsystem.component.ConfirmDialog
import com.example.app_finances.core.designsystem.component.ConfirmTexts
import com.example.app_finances.core.designsystem.component.DialogActions
import com.example.app_finances.data.local.entity.InvoicePaymentEntity
import com.example.app_finances.core.model.TransactionItem
import com.example.app_finances.domain.InvoiceStatus
import com.example.app_finances.domain.Invoice
import com.example.app_finances.feature.cards.InvoiceStatusChip
import com.example.app_finances.feature.cards.LimitSummary

data class CardDetailActions(val onBack: () -> Unit, val onOpenTransaction: (Long) -> Unit)

@Composable
fun CardDetailScreen(cardId: Long, scaffoldPadding: PaddingValues, actions: CardDetailActions) {
    val viewModel = containerViewModel { CardDetailViewModel(cardId, it.cardRepository, it.catalogSources, it.today) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val detail = state.detail
    ShowMessages(viewModel.errorFlow)
    var paymentToDelete by remember { mutableStateOf<InvoicePaymentEntity?>(null) }

    ScreenColumn(title = detail?.summary?.card?.name.orEmpty(), scaffoldPadding = scaffoldPadding, onBack = actions.onBack) {
        if (detail == null) return@ScreenColumn
        item { LimitSummary(detail.summary) }
        item { MonthSelector(detail.invoice.month, viewModel::setMonth) }
        item { InvoiceCard(detail.invoice, detail.invoiceStatus, onPay = viewModel::openPay) }
        purchases(detail.purchases, actions.onOpenTransaction)
        payments(detail.payments) { paymentToDelete = it }
    }
    val pay = state.pay
    if (detail != null && pay != null) {
        PayInvoiceDialog(pay, detail.invoice.month, detail.accountOptions, PayActions(viewModel::editPay, DialogActions(viewModel::confirmPay, viewModel::closePay)))
    }
    paymentToDelete?.let { payment ->
        DeletePaymentDialog(payment, onConfirm = { viewModel.deletePayment(payment); paymentToDelete = null }, onDismiss = { paymentToDelete = null })
    }
}

/** Excluir um pagamento devolve o valor à conta e reabre o que faltava da fatura. */
@Composable
private fun DeletePaymentDialog(payment: InvoicePaymentEntity, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val texts = ConfirmTexts(
        stringResource(R.string.card_payment_delete_title),
        stringResource(R.string.card_payment_delete_body, Money.format(payment.amountCents)),
        stringResource(R.string.action_delete),
    )
    ConfirmDialog(texts, DialogActions(onConfirm, onDismiss))
}

@Composable
private fun InvoiceCard(invoice: Invoice, status: InvoiceStatus, onPay: () -> Unit) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MoneyText(invoice.totalCents, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            InvoiceStatusChip(status)
        }
        Text(
            "${stringResource(R.string.card_closes_on, Dates.short(invoice.closingDate))} · ${stringResource(R.string.card_due_on, Dates.short(invoice.dueDate))}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (invoice.paidCents > 0) {
            AmountLine(stringResource(R.string.card_paid_amount), invoice.paidCents)
            AmountLine(stringResource(R.string.card_remaining), invoice.remainingCents)
        }
        if (invoice.remainingCents > 0) {
            Button(onClick = onPay, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.card_pay_invoice)) }
        }
    }
}

@Composable
private fun AmountLine(label: String, cents: Long) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        MoneyText(cents)
    }
}

private fun LazyListScope.purchases(purchases: List<TransactionItem>, onOpen: (Long) -> Unit) {
    item { SectionTitle(stringResource(R.string.card_purchases)) }
    if (purchases.isEmpty()) {
        item { Text(stringResource(R.string.card_no_purchases), color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
    items(purchases, key = { "purchase-${it.id}" }) { item ->
        TransactionRow(item, onClick = { onOpen(item.id) }, modifier = Modifier.clip(MaterialTheme.shapes.large))
    }
}

private fun LazyListScope.payments(payments: List<PaymentLine>, onDelete: (InvoicePaymentEntity) -> Unit) {
    if (payments.isEmpty()) return
    item { SectionTitle(stringResource(R.string.card_payments)) }
    items(payments, key = { "payment-${it.payment.id}" }) { line ->
        Row(Modifier.fillMaxWidth().padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.card_payment_line, Dates.short(line.payment.date), line.accountName),
                Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            MoneyText(line.payment.amountCents)
            IconButton(onClick = { onDelete(line.payment) }) {
                Icon(Icons.Outlined.Delete, stringResource(R.string.card_payment_delete), tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
}
