package com.example.app_finances.feature.cards.detail

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.app_finances.R
import com.example.app_finances.core.designsystem.component.AmountInput
import com.example.app_finances.core.designsystem.component.DateField
import com.example.app_finances.core.designsystem.component.DialogActions
import com.example.app_finances.core.designsystem.component.FormDialog
import com.example.app_finances.core.designsystem.component.Option
import com.example.app_finances.core.designsystem.component.OptionDropdown
import com.example.app_finances.core.format.Dates
import com.example.app_finances.core.ui.Editing
import java.time.YearMonth

data class PayActions(
    val onEdit: ((PayEditor) -> PayEditor) -> Unit,
    val dialog: DialogActions,
)

/** Pagamento total ou parcial da fatura, saindo de uma conta. */
@Composable
fun PayInvoiceDialog(editing: Editing<PayEditor>, month: YearMonth, accountOptions: List<Option<Long>>, actions: PayActions) {
    val editor = editing.value
    val onEdit = actions.onEdit
    FormDialog(
        title = stringResource(R.string.card_pay_title, Dates.month(month)),
        editing = editing,
        actions = actions.dialog,
        confirmLabel = stringResource(R.string.card_pay_invoice),
    ) {
        AmountInput(
            cents = editor.amountCents,
            onCentsChange = { cents -> onEdit { it.copy(amountCents = cents) } },
            label = stringResource(R.string.card_pay_amount),
            modifier = Modifier.fillMaxWidth(),
        )
        OptionDropdown(stringResource(R.string.card_pay_from), accountOptions, editor.accountId, { id -> onEdit { it.copy(accountId = id) } })
        DateField(editor.date, { date -> onEdit { it.copy(date = date) } })
    }
}
