package com.example.app_finances.core.designsystem.component

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.example.app_finances.core.designsystem.theme.AmountTextStyle
import com.example.app_finances.core.designsystem.theme.FinanceTheme
import com.example.app_finances.core.format.Money

/** Como colorir um valor. */
enum class AmountTone { Neutral, Income, Expense, BySign }

@Composable
fun MoneyText(
    cents: Long,
    modifier: Modifier = Modifier,
    tone: AmountTone = AmountTone.Neutral,
    style: TextStyle = AmountTextStyle,
) {
    Text(
        text = Money.format(cents, showPlusSign = tone == AmountTone.Income),
        modifier = modifier,
        style = style,
        color = tone.color(cents),
        maxLines = 1,
    )
}

/** Cor de um valor conforme o tom; [cents] só importa para [AmountTone.BySign]. */
@Composable
fun AmountTone.color(cents: Long = 0): Color {
    val colors = FinanceTheme.colors
    return when (this) {
        AmountTone.Neutral -> LocalContentColor.current
        AmountTone.Income -> colors.income
        AmountTone.Expense -> colors.expense
        AmountTone.BySign -> if (cents < 0) colors.expense else colors.income
    }
}
