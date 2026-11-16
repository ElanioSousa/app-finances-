package com.example.app_finances.domain

import com.example.app_finances.data.local.entity.CategoryEntity
import com.example.app_finances.data.local.entity.CategoryKind
import com.example.app_finances.data.local.entity.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class MoneyAlertsTest {

    private val october = YearMonth.of(2026, 10)
    private fun spent(category: Long, amount: Long) = transaction(amount = amount).copy(categoryId = category)

    @Test
    fun resumoApontaACategoriaQueMaisMudouEmReais() {
        val september = listOf(spent(1, 50_000), spent(2, 10_000), spent(3, 20_000))
        // Alimentação +18% (R$ 90), Transporte +50% (R$ 50), Lazer −5% (pouco, ignorado).
        val octoberList = listOf(spent(1, 59_000), spent(2, 15_000), spent(3, 19_000), transaction(TransactionType.INCOME, amount = 276_014))
        val summary = MonthSummaries.of(october, octoberList, september)!!
        assertEquals(MonthTotals(incomeCents = 276_014, expenseCents = 93_000), summary.totals)
        assertEquals(CategoryChange(1, 18, 9_000), summary.change)
    }

    @Test
    fun mesVazioNaoTemResumo() = assertNull(MonthSummaries.of(october, emptyList(), listOf(spent(1, 100))))

    @Test
    fun alertaDeOrcamentoECartaoSoUmaVezPorNivel() {
        val food = CategoryEntity(id = 1, name = "Alimentação", kind = CategoryKind.EXPENSE, iconKey = "food", colorArgb = 0, monthlyBudgetCents = 100_000)
        val nov = YearMonth.of(2026, 11)
        val warning = Budgets.statuses(listOf(food), listOf(spent(1, 85_000)))
        val nubank = card(id = 7).copy(limitCents = 100_000)
        val full = CardSummary.of(nubank, listOf(transaction(amount = 95_000, accountId = null, date = LocalDate.of(2026, 11, 10)).copy(cardId = 7)), emptyList(), LocalDate.of(2026, 11, 14))

        val first = MoneyAlert.due(warning, listOf(full), nov, emptySet())
        assertEquals(2, first.size)
        // Já avisados: nada de novo. Estourar o orçamento é outro nível: avisa de novo.
        assertEquals(emptyList<MoneyAlert>(), MoneyAlert.due(warning, listOf(full), nov, first.map { it.key }.toSet()))
        val over = Budgets.statuses(listOf(food), listOf(spent(1, 120_000)))
        assertEquals(listOf("2026-11|budget|1|OVER"), MoneyAlert.due(over, listOf(full), nov, first.map { it.key }.toSet()).map { it.key })
        // Ao lembrar os enviados, os de antes de outubro somem.
        assertEquals(setOf("2026-11|budget|1|OVER"), MoneyAlert.remember(setOf("2026-09|card|7"), listOf("2026-11|budget|1|OVER"), keepFrom = october))
    }
}
