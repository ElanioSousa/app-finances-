package com.example.app_finances.domain

import com.example.app_finances.data.local.entity.CreditCardEntity
import com.example.app_finances.data.local.entity.InvoicePaymentEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class CardSummaryTest {
    private val card = CreditCardEntity(id = 1, name = "Cartão", limitCents = 100_000, closingDay = 5, dueDay = 15, colorArgb = 0)
    private val today = LocalDate.of(2026, 11, 10)

    private fun purchase(cents: Long, date: LocalDate) = transaction(amount = cents, accountId = null, date = date).copy(cardId = 1)

    @Test
    fun `fatura atual, anterior a pagar e limite`() {
        val purchases = listOf(
            purchase(20_000, LocalDate.of(2026, 11, 1)),
            purchase(30_000, LocalDate.of(2026, 11, 8)),
        )
        val summary = CardSummary.of(card, purchases, emptyList(), today)
        assertEquals(YearMonth.of(2026, 12), summary.current.month)
        assertEquals(30_000, summary.current.totalCents)
        assertEquals(20_000L, summary.pending?.remainingCents)
        assertEquals(50_000, summary.usedCents)
        assertEquals(50_000, summary.availableCents)
        assertEquals(0.5f, summary.usedFraction)
    }

    @Test
    fun `fatura anterior paga não aparece como pendente`() {
        val purchases = listOf(purchase(20_000, LocalDate.of(2026, 11, 1)))
        val payments = listOf(InvoicePaymentEntity(cardId = 1, referenceMonth = YearMonth.of(2026, 11), amountCents = 20_000, date = today, accountId = 1))
        val summary = CardSummary.of(card, purchases, payments, today)
        assertNull(summary.pending)
        assertEquals(0, summary.usedCents)
    }
}
