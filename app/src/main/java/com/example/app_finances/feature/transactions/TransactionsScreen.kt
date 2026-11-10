package com.example.app_finances.feature.transactions

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.app_finances.R
import com.example.app_finances.core.designsystem.component.EmptyState
import com.example.app_finances.core.designsystem.component.ScreenColumn

@Composable
fun TransactionsScreen(scaffoldPadding: PaddingValues) {
    ScreenColumn(title = stringResource(R.string.nav_transactions), scaffoldPadding = scaffoldPadding) {
        item {
            EmptyState(
                icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                title = stringResource(R.string.transactions_empty_title),
                body = stringResource(R.string.transactions_empty_body),
            )
        }
    }
}
