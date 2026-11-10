package com.example.app_finances.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class InstallmentsTest {

    @Test
    fun `divide e põe os centavos que sobram na primeira`() {
        assertEquals(listOf(334L, 333L, 333L), Installments.split(1_000, 3))
        assertEquals(listOf(1_000L), Installments.split(1_000, 1))
        assertEquals(1_000L, Installments.split(1_000, 7).sum())
    }

    @Test
    fun `parcelas mensais ligadas pelo mesmo grupo`() {
        val purchase = transaction(amount = 30_000, accountId = null, date = LocalDate.of(2027, 1, 31)).copy(cardId = 1)
        val parts = Installments.schedule(purchase, 3) { "g1" }
        assertEquals(listOf(10_000L, 10_000L, 10_000L), parts.map { it.amountCents })
        assertEquals(listOf(LocalDate.of(2027, 1, 31), LocalDate.of(2027, 2, 28), LocalDate.of(2027, 3, 31)), parts.map { it.date })
        assertEquals(listOf(1, 2, 3), parts.map { it.installmentNumber })
        assertTrue(parts.all { it.installmentGroupId == "g1" && it.installmentCount == 3 })
        parts.forEach { assertEquals(emptyList<String>(), TransactionRules.violations(it)) }
    }

    @Test
    fun `à vista não vira parcelamento`() {
        val purchase = transaction(accountId = null).copy(cardId = 1)
        assertEquals(listOf(purchase), Installments.schedule(purchase, 1) { "g" })
    }

    @Test
    fun `parcelas caem em faturas seguidas`() {
        val purchase = transaction(amount = 30_000, accountId = null, date = LocalDate.of(2027, 1, 31)).copy(cardId = 1)
        val months = Installments.schedule(purchase, 3) { "g" }.map { InvoiceCalculator.invoiceMonthOf(it.date, closingDay = 31) }
        assertEquals(3, months.distinct().size)
    }
}
