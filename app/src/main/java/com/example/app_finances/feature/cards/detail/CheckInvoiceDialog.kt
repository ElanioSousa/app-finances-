package com.example.app_finances.feature.cards.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.app_finances.R
import com.example.app_finances.core.designsystem.component.AmountInput
import com.example.app_finances.core.designsystem.component.formatMoney
import com.example.app_finances.core.designsystem.theme.FinanceTheme
import com.example.app_finances.domain.Invoice
import com.example.app_finances.domain.InvoiceCheck

/** Digita o total da fatura do banco e vê na hora se confere com a do app (nada é gravado). */
@Composable
fun CheckInvoiceDialog(invoice: Invoice, onDismiss: () -> Unit) {
    var bankCents by rememberSaveable { mutableLongStateOf(0L) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.card_check_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.card_check_app_total, formatMoney(invoice.totalCents)))
                AmountInput(bankCents, { bankCents = it }, stringResource(R.string.card_check_bank_total), Modifier.fillMaxWidth())
                if (bankCents > 0) CheckResult(InvoiceCheck.of(invoice, bankCents))
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) } },
    )
}

@Composable
private fun CheckResult(check: InvoiceCheck) {
    val colors = FinanceTheme.colors
    val (text, color) = when (check) {
        InvoiceCheck.Match -> stringResource(R.string.card_check_match) to colors.income
        is InvoiceCheck.Missing -> stringResource(R.string.card_check_missing, formatMoney(check.cents)) to colors.warning
        is InvoiceCheck.Extra -> extraText(check) to colors.warning
    }
    Text(text, color = color, style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun extraText(check: InvoiceCheck.Extra): String {
    val base = stringResource(R.string.card_check_extra, formatMoney(check.cents))
    if (check.suspects.isEmpty()) return base
    return base + "\n" + stringResource(R.string.card_check_suspects, check.suspects.joinToString { it.description })
}

