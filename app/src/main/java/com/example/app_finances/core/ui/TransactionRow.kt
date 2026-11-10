package com.example.app_finances.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CompareArrows
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.app_finances.R
import com.example.app_finances.core.designsystem.categoryIcon
import com.example.app_finances.core.designsystem.component.CategoryIcon
import com.example.app_finances.core.designsystem.component.MoneyText
import com.example.app_finances.core.designsystem.theme.FinanceTheme
import com.example.app_finances.core.model.TransactionItem
import com.example.app_finances.data.local.entity.TransactionType

/** Uma linha de lançamento: ícone da categoria, descrição, categoria/conta e valor. */
@Composable
fun TransactionRow(item: TransactionItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TransactionIcon(item)
        Column(Modifier.weight(1f)) {
            Text(item.transaction.description, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        TransactionAmount(item)
    }
}

@Composable
private fun TransactionIcon(item: TransactionItem) {
    if (item.transaction.type == TransactionType.TRANSFER) {
        CategoryIcon(Icons.AutoMirrored.Outlined.CompareArrows, FinanceTheme.colors.transfer)
    } else {
        val color = item.category?.let { Color(it.colorArgb) } ?: MaterialTheme.colorScheme.onSurfaceVariant
        CategoryIcon(categoryIcon(item.category?.iconKey), color)
    }
}

@Composable
private fun TransactionAmount(item: TransactionItem) {
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
