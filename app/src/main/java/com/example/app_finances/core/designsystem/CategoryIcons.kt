package com.example.app_finances.core.designsystem

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LaptopMac
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.ui.graphics.vector.ImageVector

/** Traduz o `iconKey` guardado no banco para um ícone. Chave desconhecida → ícone genérico. */
fun categoryIcon(iconKey: String?): ImageVector = when (iconKey) {
    "food" -> Icons.Outlined.Restaurant
    "home" -> Icons.Outlined.Home
    "transport" -> Icons.Outlined.DirectionsCar
    "health" -> Icons.Outlined.LocalHospital
    "education" -> Icons.Outlined.School
    "leisure" -> Icons.Outlined.SportsEsports
    "shopping" -> Icons.Outlined.ShoppingBag
    "subscriptions" -> Icons.Outlined.Subscriptions
    "bills" -> Icons.AutoMirrored.Outlined.ReceiptLong
    "salary" -> Icons.Outlined.Payments
    "freelance" -> Icons.Outlined.LaptopMac
    "investments" -> Icons.AutoMirrored.Outlined.TrendingUp
    "gift" -> Icons.Outlined.CardGiftcard
    else -> Icons.Outlined.Category
}
