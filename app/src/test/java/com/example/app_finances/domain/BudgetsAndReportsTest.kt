package com.example.app_finances.domain

import com.example.app_finances.data.local.entity.CategoryEntity
import com.example.app_finances.data.local.entity.CategoryKind
import com.example.app_finances.data.local.entity.TransactionType.EXPENSE
import com.example.app_finances.data.local.entity.TransactionType.INCOME
import com.example.app_finances.data.local.entity.TransactionType.TRANSFER
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class BudgetsAndReportsTest {

    private fun category(id: Long, budget: Long?, kind: CategoryKind = CategoryKind.EXPENSE) =
        CategoryEntity(id = id, name = "C$id", kind = kind, iconKey = "food", colorArgb = 0, monthlyBudgetCents = budget)

    private fun expense(cents: Long, category: Long?) = transaction(EXPENSE, cents).copy(categoryId = category)

    @Test
    fun `níveis do orçamento - ok, atenção a partir de 80 e estourado acima de 100`() {
        val categories = listOf(category(1, 10_000), category(2, 10_000), category(3, 10_000), category(4, null))
        val month = listOf(expense(5_000, 1), expense(8_000, 2), expense(10_001, 3), expense(99_999, 4), transaction(INCOME, 50_000).copy(categoryId = 1))
        val statuses = Budgets.statuses(categories, month)
        assertEquals(listOf(3L, 2L, 1L), statuses.map { it.category.id })
        assertEquals(listOf(BudgetLevel.OVER, BudgetLevel.WARNING, BudgetLevel.OK), statuses.map { it.level })
        assertEquals(-1L, statuses.first().remainingCents)
    }

    @Test
    fun `categoria com orçamento e sem gasto aparece zerada`() {
        val status = Budgets.statuses(listOf(category(1, 10_000)), emptyList()).single()
        assertEquals(0L, status.spentCents)
        assertEquals(BudgetLevel.OK, status.level)
    }

    @Test
    fun `regras da categoria`() {
        assertEquals(emptyList<String>(), CategoryRules.violations(category(1, 10_000)))
        assertTrue(CategoryRules.violations(category(1, 0)).isNotEmpty())
        assertTrue(CategoryRules.violations(category(1, 5_000, CategoryKind.INCOME)).isNotEmpty())
        assertTrue(CategoryRules.violations(category(1, null).copy(name = " ")).isNotEmpty())
    }

    @Test
    fun `período de mês e de ano`() {
        val month = ReportRange(ReportPeriod.MONTH, YearMonth.of(2026, 2))
        assertEquals(LocalDate.of(2026, 2, 1) to LocalDate.of(2026, 2, 28), month.start to month.end)
        val year = month.copy(period = ReportPeriod.YEAR)
        assertEquals(LocalDate.of(2026, 1, 1) to LocalDate.of(2026, 12, 31), year.start to year.end)
        assertEquals(YearMonth.of(2025, 2), year.previous().anchor)
        assertEquals(YearMonth.of(2026, 3), month.next().anchor)
    }

    @Test
    fun `relatório por categoria e por conta ou cartão, sem transferências`() {
        val list = listOf(
            expense(3_000, 1), expense(1_000, 2),
            transaction(EXPENSE, 2_000, accountId = null).copy(cardId = 9, categoryId = 1),
            transaction(INCOME, 10_000).copy(categoryId = 5),
            transaction(TRANSFER, 7_000).copy(destinationAccountId = 2),
        )
        val report = Reports.build(list)
        assertEquals(MonthTotals(10_000, 6_000), report.totals)
        assertEquals(listOf(1L to 5_000L, 2L to 1_000L), report.expensesByCategory.map { it.categoryId to it.cents })
        assertEquals(listOf(5L), report.incomesByCategory.map { it.categoryId })
        val wallet = report.byOrigin.first { it.accountId == 1L }
        assertEquals(10_000L to 4_000L, wallet.incomeCents to wallet.expenseCents)
        assertEquals(2_000L, report.byOrigin.first { it.cardId == 9L }.expenseCents)
        assertEquals(2, report.byOrigin.size)
    }
}
