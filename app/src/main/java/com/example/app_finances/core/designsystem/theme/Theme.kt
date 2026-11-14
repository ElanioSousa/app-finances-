package com.example.app_finances.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
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

private val FinanceLightColorScheme = lightColorScheme(
    primary = EmeraldDark,
    onPrimary = Paper100,
    primaryContainer = EmeraldLight,
    onPrimaryContainer = EmeraldDark,
    secondary = SkyDark,
    onSecondary = Paper100,
    secondaryContainer = SkyLight,
    onSecondaryContainer = SkyDark,
    tertiary = AmberDark,
    onTertiary = Paper100,
    tertiaryContainer = AmberLight,
    onTertiaryContainer = AmberDark,
    error = CoralDark,
    onError = Paper100,
    errorContainer = CoralLight,
    onErrorContainer = CoralDark,
    background = Paper50,
    onBackground = InkText,
    surface = Paper50,
    onSurface = InkText,
    surfaceVariant = Paper200,
    onSurfaceVariant = InkTextSecondary,
    surfaceContainerLowest = Paper100,
    surfaceContainerLow = Paper100,
    surfaceContainer = Paper100,
    surfaceContainerHigh = Paper200,
    surfaceContainerHighest = Paper300,
    outline = Paper400,
    outlineVariant = Paper300,
    inverseSurface = InkText,
    inverseOnSurface = Paper50,
    scrim = Color.Black,
)

/** Cores com significado financeiro, fora do esquema do Material. */
@Immutable
data class FinanceColors(
    val income: Color,
    val incomeContainer: Color,
    val expense: Color,
    val expenseContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val transfer: Color,
    val transferContainer: Color,
    val textMuted: Color,
)

private val DarkFinanceColors = FinanceColors(
    income = Emerald, incomeContainer = EmeraldDeep, expense = Coral, expenseContainer = CoralDeep,
    warning = Amber, warningContainer = AmberDeep, transfer = Sky, transferContainer = SkyDeep, textMuted = TextMuted,
)

private val LightFinanceColors = FinanceColors(
    income = EmeraldDark, incomeContainer = EmeraldLight, expense = CoralDark, expenseContainer = CoralLight,
    warning = AmberDark, warningContainer = AmberLight, transfer = SkyDark, transferContainer = SkyLight, textMuted = InkTextMuted,
)

private val LocalFinanceColors = staticCompositionLocalOf { DarkFinanceColors }

/** As cores extras do tema atual (claro ou escuro). */
object FinanceTheme {
    val colors: FinanceColors
        @Composable @ReadOnlyComposable get() = LocalFinanceColors.current
}

/** Tema do app: escuro ou claro (escolha nas Configurações), sem cores dinâmicas do sistema. */
@Composable
fun AppfinancesTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalFinanceColors provides if (darkTheme) DarkFinanceColors else LightFinanceColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) FinanceDarkColorScheme else FinanceLightColorScheme,
            typography = FinanceTypography,
            shapes = FinanceShapes,
            content = content,
        )
    }
}

/** Degradê diagonal de [from] para [to], refeito só quando as cores mudam (ex.: ao trocar o tema). */
@Composable
fun rememberGradient(from: Color, to: Color): Brush = remember(from, to) { Brush.linearGradient(listOf(from, to)) }
