package com.example.app_finances.domain

import com.example.app_finances.data.local.entity.CreditCardEntity
import com.example.app_finances.data.local.entity.TransactionType.EXPENSE
import com.example.app_finances.data.local.entity.TransactionType.INCOME
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class InsightsTest {
    private val today = LocalDate.of(2026, 11, 10)

    private fun expense(cents: Long, category: Long?, date: LocalDate = today) =
        transaction(EXPENSE, cents, date = date).copy(categoryId = category)

    @Test
    fun `gastos por categoria ordenados e o resto em Outras`() {
        val list = listOf(
            expense(5_000, 1), expense(5_000, 1), expense(8_000, 2), expense(1_000, 3), expense(500, null),
            transaction(INCOME, 99_999),
        )
        val spend = Insights.spendingByCategory(list, maxRows = 3)
        assertEquals(listOf(1L, 2L, null), spend.map { it.categoryId })
        assertEquals(listOf(10_000L, 8_000L, 1_500L), spend.map { it.cents })
        assertTrue(spend.last().isOther)
        assertEquals(1f, spend.sumOf { it.share.toDouble() }.toFloat(), 0.001f)
        assertEquals(emptyList<CategorySpend>(), Insights.spendingByCategory(listOf(transaction(INCOME, 1_000))))
    }

    @Test
    fun `sem precisar de Outras quando cabe tudo`() {
        val spend = Insights.spendingByCategory(listOf(expense(100, 1), expense(300, null)), maxRows = 5)
        assertEquals(listOf(null, 1L), spend.map { it.categoryId })
        assertTrue(spend.none { it.isOther })
    }

    @Test
    fun `histórico de seis meses inclui meses vazios`() {
        val list = listOf(
            transaction(INCOME, 10_000, date = LocalDate.of(2026, 11, 1)),
            expense(4_000, 1, LocalDate.of(2026, 9, 15)),
            expense(9_999, 1, LocalDate.of(2026, 4, 30)),
        )
        val history = Insights.monthlyHistory(list, YearMonth.of(2026, 11))
        assertEquals((6..11).map { YearMonth.of(2026, it) }, history.map { it.month })
        assertEquals(listOf(0L, 0L, 0L, 4_000L, 0L, 0L), history.map { it.totals.expenseCents })
        assertEquals(10_000L, history.last().totals.incomeCents)
    }

    @Test
    fun `próximas contas juntam pendências e faturas a vencer`() {
        val card = CreditCardEntity(id = 1, name = "Cartão", limitCents = 100_000, closingDay = 5, dueDay = 15, colorArgb = 0)
        val purchases = listOf(transaction(amount = 20_000, accountId = null, date = LocalDate.of(2026, 11, 1)).copy(cardId = 1))
        val summary = CardSummary.of(card, purchases, emptyList(), today)
        val pending = listOf(
            expense(1_000, 1, LocalDate.of(2026, 11, 8)).copy(paid = false),
            expense(2_000, 1, LocalDate.of(2026, 11, 17)).copy(paid = false),
            expense(3_000, 1, LocalDate.of(2026, 11, 18)).copy(paid = false),
        )
        val bills = Insights.upcomingBills(pending, listOf(summary), today)
        assertEquals(listOf(LocalDate.of(2026, 11, 8), LocalDate.of(2026, 11, 15), LocalDate.of(2026, 11, 17)), bills.map { it.date })
        assertTrue(bills.first().isOverdue(today))
        assertTrue(bills[1] is UpcomingBill.CardInvoice)
        assertEquals(20_000L, bills[1].amountCents)
    }
}
