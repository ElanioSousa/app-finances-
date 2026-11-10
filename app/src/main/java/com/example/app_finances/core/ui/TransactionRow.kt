package com.example.app_finances.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CompareArrows
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.app_finances.R
import com.example.app_finances.core.designsystem.component.CategoryIcon
import com.example.app_finances.core.designsystem.component.ListRow
import com.example.app_finances.core.designsystem.component.RowText
import com.example.app_finances.core.designsystem.component.MoneyText
import com.example.app_finances.core.designsystem.theme.FinanceTheme
import com.example.app_finances.core.model.TransactionItem
import com.example.app_finances.data.local.entity.TransactionType

/** Uma linha de lançamento: ícone da categoria, descrição, categoria/conta e valor. */
@Composable
fun TransactionRow(item: TransactionItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ListRow(
        text = RowText(item.transaction.description, item.subtitle),
        onClick = onClick,
        modifier = modifier,
        leading = { TransactionIcon(item) },
        trailing = { TransactionAmount(item) },
    )
}

/** Ícone da categoria (ou de transferência) de um lançamento. */
@Composable
fun TransactionIcon(item: TransactionItem) {
    if (item.transaction.type == TransactionType.TRANSFER) {
        CategoryIcon(Icons.AutoMirrored.Outlined.CompareArrows, FinanceTheme.colors.transfer)
    } else {
        CategoryBadge(item.category)
    }
}

/** Valor colorido pelo tipo, com "Pendente" embaixo quando ainda não foi pago. */
@Composable
fun TransactionAmount(item: TransactionItem) {
    Column(horizontalAlignment = Alignment.End) {
        MoneyText(cents = item.transaction.amountCents, tone = item.transaction.type.tone)
        if (!item.transaction.paid) {
            Text(
                stringResource(R.string.transaction_pending),
                style = MaterialTheme.typography.labelSmall,
                color = FinanceTheme.colors.warning,
            )
        }
    }
}
