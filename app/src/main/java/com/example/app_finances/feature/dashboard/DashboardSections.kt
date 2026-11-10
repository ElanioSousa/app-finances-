package com.example.app_finances.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.app_finances.R
import com.example.app_finances.core.designsystem.chart.ChartLegend
import com.example.app_finances.core.designsystem.chart.MonthBarGroup
import com.example.app_finances.core.designsystem.chart.MonthBars
import com.example.app_finances.core.designsystem.chart.MonthBarsState
import com.example.app_finances.core.designsystem.chart.RankedBars
import com.example.app_finances.core.designsystem.component.AmountTone
import com.example.app_finances.core.designsystem.component.CategoryIcon
import com.example.app_finances.core.designsystem.component.ListRow
import com.example.app_finances.core.designsystem.component.MoneyText
import com.example.app_finances.core.designsystem.component.RowText
import com.example.app_finances.core.designsystem.component.SectionCard
import com.example.app_finances.core.designsystem.theme.ChartExpense
import com.example.app_finances.core.designsystem.theme.ChartIncome
import com.example.app_finances.core.designsystem.theme.FinanceTheme
import com.example.app_finances.core.format.Dates
import com.example.app_finances.core.format.Money
import com.example.app_finances.data.local.entity.TransactionEntity
import com.example.app_finances.core.model.TransactionItem
import com.example.app_finances.core.ui.BudgetRow
import com.example.app_finances.core.ui.rememberCategoryBars
import com.example.app_finances.data.local.entity.CategoryEntity
import com.example.app_finances.domain.CategorySpend
import com.example.app_finances.core.ui.TransactionAmount
import com.example.app_finances.core.ui.TransactionIcon
import com.example.app_finances.domain.BudgetStatus
import com.example.app_finances.domain.CardSummary
import com.example.app_finances.domain.Insights
import com.example.app_finances.domain.MonthBar
import com.example.app_finances.domain.UpcomingBill
import com.example.app_finances.feature.cards.InvoiceStatusChip
import java.time.LocalDate

data class UpcomingActions(
    val onOpenTransaction: (Long) -> Unit,
    val onOpenCard: (Long) -> Unit,
    val onMarkPaid: (TransactionEntity) -> Unit,
)

/** Pendências e faturas que vencem em breve (e as atrasadas), com "marcar como pago". */
@Composable
fun UpcomingCard(entries: List<UpcomingEntry>, today: LocalDate, actions: UpcomingActions) {
    SectionCard(title = stringResource(R.string.home_upcoming, Insights.UPCOMING_DAYS)) {
        if (entries.isEmpty()) {
            Text(stringResource(R.string.home_upcoming_empty, Insights.UPCOMING_DAYS), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        entries.forEach { entry ->
            val bill = entry.bill
            when {
                bill is UpcomingBill.CardInvoice -> InvoiceRow(bill, today, actions.onOpenCard)
                entry.item != null -> PendingRow(entry.item, today, actions)
            }
        }
    }
}

@Composable
private fun PendingRow(item: TransactionItem, today: LocalDate, actions: UpcomingActions) {
    ListRow(
        text = RowText(item.transaction.description, dueText(item.transaction.date, today)),
        onClick = { actions.onOpenTransaction(item.id) },
        modifier = Modifier.clip(MaterialTheme.shapes.medium),
        leading = { TransactionIcon(item) },
        trailing = {
            TransactionAmount(item)
            IconButton(onClick = { actions.onMarkPaid(item.transaction) }) {
                Icon(Icons.Outlined.CheckCircle, stringResource(R.string.home_mark_paid), tint = FinanceTheme.colors.income)
            }
        },
    )
}

@Composable
private fun InvoiceRow(bill: UpcomingBill.CardInvoice, today: LocalDate, onOpenCard: (Long) -> Unit) {
    ListRow(
        text = RowText(stringResource(R.string.home_card_invoice, bill.card.name), dueText(bill.date, today)),
        onClick = { onOpenCard(bill.card.id) },
        modifier = Modifier.clip(MaterialTheme.shapes.medium),
        leading = { CategoryIcon(Icons.Outlined.CreditCard, FinanceTheme.colors.warning) },
        trailing = { MoneyText(bill.amountCents, tone = AmountTone.Expense) },
    )
}

/** "Venceu 08/11", "Vence hoje", "Vence 15/11". */
@Composable
private fun dueText(date: LocalDate, today: LocalDate): String = when {
    date < today -> stringResource(R.string.home_overdue, Dates.dayMonth(date, today))
    date == today -> stringResource(R.string.home_due_today)
    else -> stringResource(R.string.home_due_on, Dates.dayMonth(date, today))
}

/** Fatura atual de cada cartão, com situação e vencimento. */
@Composable
fun CardsCard(cards: List<CardSummary>, today: LocalDate, onOpenCard: (Long) -> Unit) {
    SectionCard(title = stringResource(R.string.home_cards)) {
        cards.forEach { summary ->
            ListRow(
                text = RowText(summary.card.name, dueText(summary.current.dueDate, today)),
                onClick = { onOpenCard(summary.card.id) },
                modifier = Modifier.clip(MaterialTheme.shapes.medium),
                leading = { CategoryIcon(Icons.Outlined.CreditCard, Color(summary.card.colorArgb)) },
                trailing = {
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        MoneyText(summary.current.totalCents)
                        InvoiceStatusChip(summary.currentStatus)
                    }
                },
            )
        }
    }
}

/** Orçamentos do mês, os mais apertados primeiro. */
@Composable
fun BudgetsCard(budgets: List<BudgetStatus>) {
    SectionCard(title = stringResource(R.string.home_budgets)) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            budgets.forEach { BudgetRow(it) }
        }
    }
}

/** Ranking das categorias com mais gasto no mês. */
@Composable
fun SpendingCard(spending: List<CategorySpend>, categoryById: Map<Long, CategoryEntity>) {
    SectionCard(title = stringResource(R.string.home_spending)) {
        if (spending.isEmpty()) {
            Text(stringResource(R.string.home_spending_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@SectionCard
        }
        RankedBars(rememberCategoryBars(spending, categoryById))
    }
}

/** Receitas × despesas dos últimos meses; tocar num mês mostra os valores dele. */
@Composable
fun HistoryCard(history: List<MonthBar>, selected: Int, onSelect: (Int) -> Unit) {
    if (history.isEmpty()) return
    val chosen = history[selected.coerceIn(history.indices)]
    val summary = stringResource(
        R.string.home_history_selected,
        Dates.month(chosen.month),
        Money.format(chosen.totals.incomeCents),
        Money.format(chosen.totals.expenseCents),
    )
    val groups = remember(history) {
        history.map { MonthBarGroup(Dates.shortMonth(it.month), it.totals.incomeCents, it.totals.expenseCents) }
    }
    SectionCard(title = stringResource(R.string.home_history, Insights.HISTORY_MONTHS)) {
        ChartLegend(listOf(stringResource(R.string.home_income) to ChartIncome, stringResource(R.string.home_expenses) to ChartExpense))
        MonthBars(
            state = MonthBarsState(
                groups = groups,
                selected = selected,
                description = stringResource(R.string.home_history_description, summary),
            ),
            onSelect = onSelect,
        )
        Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
