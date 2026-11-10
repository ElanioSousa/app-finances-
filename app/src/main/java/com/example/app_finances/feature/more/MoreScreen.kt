package com.example.app_finances.feature.more

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
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
import com.example.app_finances.core.designsystem.theme.Amber
import com.example.app_finances.core.designsystem.theme.Emerald
import com.example.app_finances.core.designsystem.theme.Sky
import com.example.app_finances.core.designsystem.theme.TextSecondary
import com.example.app_finances.core.designsystem.theme.Violet
import com.example.app_finances.navigation.Routes

/** [route] nulo = função ainda não disponível. */
private data class MoreEntry(
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
    val color: Color,
    val route: String? = null,
)

private val entries = listOf(
    MoreEntry(R.string.more_recurring, Icons.Outlined.EventRepeat, Amber, Routes.RECURRING),
    MoreEntry(R.string.more_accounts, Icons.Outlined.AccountBalanceWallet, Emerald, Routes.ACCOUNTS),
    MoreEntry(R.string.more_categories, Icons.Outlined.Category, Violet),
    MoreEntry(R.string.more_reports, Icons.AutoMirrored.Outlined.ShowChart, Sky),
    MoreEntry(R.string.more_settings, Icons.Outlined.Settings, TextSecondary),
)

@Composable
fun MoreScreen(scaffoldPadding: PaddingValues, onOpen: (String) -> Unit) {
    ScreenColumn(title = stringResource(R.string.nav_more), scaffoldPadding = scaffoldPadding) {
        entries.forEach { entry ->
            item(key = entry.labelRes) { MoreItem(entry, onOpen) }
        }
    }
}

@Composable
private fun MoreItem(entry: MoreEntry, onOpen: (String) -> Unit) {
    val comingSoon: (@Composable () -> Unit)? = if (entry.route == null) {
        { Text(stringResource(R.string.coming_soon)) }
    } else {
        null
    }
    ListItem(
        headlineContent = { Text(stringResource(entry.labelRes)) },
        supportingContent = comingSoon,
        leadingContent = { CategoryIcon(icon = entry.icon, color = entry.color) },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            supportingColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        modifier = Modifier
            .clip(MaterialTheme.shapes.large)
            .clickable(enabled = entry.route != null) { entry.route?.let(onOpen) },
    )
}
