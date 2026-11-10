package com.example.app_finances.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

private val FinanceDarkColorScheme = darkColorScheme(
    primary = Emerald,
    onPrimary = Ink950,
    primaryContainer = EmeraldDeep,
    onPrimaryContainer = Emerald,
    secondary = Sky,
    onSecondary = Ink950,
    secondaryContainer = SkyDeep,
    onSecondaryContainer = Sky,
    tertiary = Amber,
    onTertiary = Ink950,
    tertiaryContainer = AmberDeep,
    onTertiaryContainer = Amber,
    error = Coral,
    onError = Ink950,
    errorContainer = CoralDeep,
    onErrorContainer = Coral,
    background = Ink950,
    onBackground = TextPrimary,
    surface = Ink950,
    onSurface = TextPrimary,
    surfaceVariant = Ink850,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = Ink950,
    surfaceContainerLow = Ink900,
    surfaceContainer = Ink900,
    surfaceContainerHigh = Ink850,
    surfaceContainerHighest = Ink800,
    outline = Ink700,
    outlineVariant = Ink800,
    inverseSurface = TextPrimary,
    inverseOnSurface = Ink950,
    scrim = Color.Black,
)

/** Cores com significado financeiro, fora do esquema do Material. */
@Immutable
data class FinanceColors(
    val income: Color = Emerald,
    val incomeContainer: Color = EmeraldDeep,
    val expense: Color = Coral,
    val expenseContainer: Color = CoralDeep,
    val warning: Color = Amber,
    val warningContainer: Color = AmberDeep,
    val transfer: Color = Sky,
    val transferContainer: Color = SkyDeep,
    val textMuted: Color = TextMuted,
)

/** O app tem um único tema (escuro), então as cores extras são uma constante. */
object FinanceTheme {
    val colors = FinanceColors()
}

/** Tema do app: sempre escuro, sem cores dinâmicas do sistema. */
@Composable
fun AppfinancesTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FinanceDarkColorScheme,
        typography = FinanceTypography,
        shapes = FinanceShapes,
        content = content,
    )
}
