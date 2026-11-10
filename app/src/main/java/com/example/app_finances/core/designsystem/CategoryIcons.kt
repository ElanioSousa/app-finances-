package com.example.app_finances.core.designsystem

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.ChildCare
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LaptopMac
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Ícones que uma categoria pode usar. A chave é o que fica no banco ("food");
 * a ordem é a do seletor de ícones.
 */
val CategoryIcons: Map<String, ImageVector> = linkedMapOf(
    "food" to Icons.Outlined.Restaurant,
    "home" to Icons.Outlined.Home,
    "transport" to Icons.Outlined.DirectionsCar,
    "health" to Icons.Outlined.LocalHospital,
    "education" to Icons.Outlined.School,
    "leisure" to Icons.Outlined.SportsEsports,
    "shopping" to Icons.Outlined.ShoppingBag,
    "subscriptions" to Icons.Outlined.Subscriptions,
    "bills" to Icons.AutoMirrored.Outlined.ReceiptLong,
    "coffee" to Icons.Outlined.LocalCafe,
    "travel" to Icons.Outlined.Flight,
    "pets" to Icons.Outlined.Pets,
    "beauty" to Icons.Outlined.Spa,
    "kids" to Icons.Outlined.ChildCare,
    "fitness" to Icons.Outlined.FitnessCenter,
    "phone" to Icons.Outlined.Smartphone,
    "salary" to Icons.Outlined.Payments,
    "freelance" to Icons.Outlined.LaptopMac,
    "investments" to Icons.AutoMirrored.Outlined.TrendingUp,
    "gift" to Icons.Outlined.CardGiftcard,
    "other" to Icons.Outlined.Category,
)

/** Traduz o `iconKey` guardado no banco para um ícone. Chave desconhecida → ícone genérico. */
fun categoryIcon(iconKey: String?): ImageVector = CategoryIcons[iconKey] ?: Icons.Outlined.Category
