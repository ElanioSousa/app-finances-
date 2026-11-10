package com.example.app_finances.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.app_finances.R

/** Abas da barra inferior. */
enum class TopLevelDestination(
    val route: String,
    @param:StringRes val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val showsAddButton: Boolean,
) {
    Home("home", R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home, showsAddButton = true),
    Transactions(
        "transactions", R.string.nav_transactions,
        Icons.AutoMirrored.Filled.ReceiptLong, Icons.AutoMirrored.Outlined.ReceiptLong, showsAddButton = true,
    ),
    Cards("cards", R.string.nav_cards, Icons.Filled.CreditCard, Icons.Outlined.CreditCard, showsAddButton = false),
    More("more", R.string.nav_more, Icons.Filled.GridView, Icons.Outlined.GridView, showsAddButton = false),
}
