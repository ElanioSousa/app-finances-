package com.example.app_finances.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Base = Typography()

/** Algarismos de largura fixa: valores alinham em listas e não "pulam" ao mudar. */
private fun TextStyle.tabular() = copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum")

val FinanceTypography = Typography(
    displaySmall = Base.displaySmall.tabular(),
    headlineLarge = Base.headlineLarge.tabular(),
    headlineMedium = Base.headlineMedium.tabular(),
    headlineSmall = Base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = Base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = Base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = Base.titleSmall.copy(fontWeight = FontWeight.Medium),
    bodyLarge = Base.bodyLarge,
    bodyMedium = Base.bodyMedium,
    bodySmall = Base.bodySmall,
    labelLarge = Base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    labelMedium = Base.labelMedium,
    labelSmall = Base.labelSmall.copy(letterSpacing = 0.4.sp),
)

/** Estilo para valores em listas: corpo do texto com algarismos tabulares. */
val AmountTextStyle = Base.bodyLarge.tabular()
