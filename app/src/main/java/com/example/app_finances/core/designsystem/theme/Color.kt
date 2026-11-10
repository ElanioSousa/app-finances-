package com.example.app_finances.core.designsystem.theme

import androidx.compose.ui.graphics.Color

// Camadas do fundo (mais escuro → mais claro)
val Ink950 = Color(0xFF0F1115)
val Ink900 = Color(0xFF171A21)
val Ink850 = Color(0xFF1F232C)
val Ink800 = Color(0xFF272C37)
val Ink700 = Color(0xFF2A2F3A)

// Texto
val TextPrimary = Color(0xFFE6E8EE)
val TextSecondary = Color(0xFF9AA3B2)
val TextMuted = Color(0xFF6B7385)

// Marca e semântica financeira
val Emerald = Color(0xFF34D399)
val EmeraldDeep = Color(0xFF064E3B)
val Coral = Color(0xFFF87171)
val CoralDeep = Color(0xFF4C1D1D)
val Amber = Color(0xFFFBBF24)
val AmberDeep = Color(0xFF4A3A0E)
val Sky = Color(0xFF60A5FA)
val SkyDeep = Color(0xFF15304F)
val Violet = Color(0xFFA78BFA)

/** Cores que o usuário pode escolher para contas, cartões e categorias. */
val PickerPalette = listOf(
    Emerald, Sky, Violet, Color(0xFFF472B6),
    Coral, Color(0xFFFB923C), Amber, Color(0xFF2DD4BF),
    Color(0xFF94A3B8), Color(0xFF8B5CF6), Color(0xFFE11D48), Color(0xFF0EA5E9),
)

// Gráficos: tons validados para o fundo escuro (faixa de luminosidade, croma, daltonismo e contraste).
val ChartIncome = Color(0xFF059669)
val ChartExpense = Color(0xFFDC2626)
val ChartMagnitude = Color(0xFF3B82F6)
