package com.example.app_finances.feature.cards

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.app_finances.R
import com.example.app_finances.core.designsystem.component.EmptyState
import com.example.app_finances.core.designsystem.component.ScreenColumn

@Composable
fun CardsScreen(scaffoldPadding: PaddingValues) {
    ScreenColumn(title = stringResource(R.string.nav_cards), scaffoldPadding = scaffoldPadding) {
        item {
            EmptyState(
                icon = Icons.Outlined.CreditCard,
                title = stringResource(R.string.cards_empty_title),
                body = stringResource(R.string.cards_empty_body),
            )
        }
    }
}
