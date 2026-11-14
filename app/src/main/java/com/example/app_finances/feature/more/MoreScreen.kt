package com.example.app_finances.feature.more

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CompareArrows
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.example.app_finances.R
import com.example.app_finances.core.designsystem.component.CategoryIcon
import com.example.app_finances.core.designsystem.component.ScreenColumn
import com.example.app_finances.core.designsystem.theme.FinanceTheme
import com.example.app_finances.core.designsystem.theme.Purple
import com.example.app_finances.core.designsystem.theme.Teal
import com.example.app_finances.navigation.Routes

/** O que os itens da tela "Mais" abrem: uma tela interna ou o formulário de transferência. */
data class MoreActions(val onOpen: (String) -> Unit, val onNewTransfer: () -> Unit)

private class MoreEntry(
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
    val color: @Composable () -> Color,
    val open: (MoreActions) -> Unit,
)

private fun screen(@StringRes labelRes: Int, icon: ImageVector, color: @Composable () -> Color, route: String) =
    MoreEntry(labelRes, icon, color) { it.onOpen(route) }

private val entries = listOf(
    screen(R.string.more_recurring, Icons.Outlined.EventRepeat, { FinanceTheme.colors.warning }, Routes.RECURRING),
    MoreEntry(R.string.more_transfer, Icons.AutoMirrored.Outlined.CompareArrows, { FinanceTheme.colors.transfer }) { it.onNewTransfer() },
    screen(R.string.more_accounts, Icons.Outlined.AccountBalanceWallet, { MaterialTheme.colorScheme.primary }, Routes.ACCOUNTS),
    screen(R.string.more_categories, Icons.Outlined.Category, { Purple }, Routes.CATEGORIES),
    screen(R.string.more_reports, Icons.AutoMirrored.Outlined.ShowChart, { Teal }, Routes.REPORTS),
    screen(R.string.more_settings, Icons.Outlined.Settings, { MaterialTheme.colorScheme.onSurfaceVariant }, Routes.SETTINGS),
)

@Composable
fun MoreScreen(scaffoldPadding: PaddingValues, actions: MoreActions) {
    ScreenColumn(title = stringResource(R.string.nav_more), scaffoldPadding = scaffoldPadding) {
        entries.forEach { entry ->
            item(key = entry.labelRes) { MoreItem(entry, actions) }
        }
    }
}

@Composable
private fun MoreItem(entry: MoreEntry, actions: MoreActions) {
    ListItem(
        headlineContent = { Text(stringResource(entry.labelRes)) },
        leadingContent = { CategoryIcon(icon = entry.icon, color = entry.color()) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .clip(MaterialTheme.shapes.large)
            .clickable { entry.open(actions) },
    )
}
