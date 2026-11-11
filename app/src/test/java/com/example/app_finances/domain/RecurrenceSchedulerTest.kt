package com.example.app_finances.domain

import com.example.app_finances.data.local.entity.Frequency
import com.example.app_finances.data.local.entity.RecurringEntity
import com.example.app_finances.data.local.entity.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RecurrenceSchedulerTest {

    private fun d(y: Int, m: Int, day: Int) = LocalDate.of(y, m, day)

    private fun recurring(frequency: Frequency = Frequency.MONTHLY, start: LocalDate = d(2026, 1, 31)) = RecurringEntity(
        id = 7, description = "Aluguel", amountCents = 150_000, type = TransactionType.EXPENSE,
        accountId = 1, frequency = frequency, startDate = start,
    )

    @Test
    fun `mensal no dia 31 volta ao 31 depois de fevereiro`() {
        assertEquals(
            listOf(d(2026, 1, 31), d(2026, 2, 28), d(2026, 3, 31), d(2026, 4, 30)),
            RecurrenceScheduler.occurrences(recurring(), d(2026, 1, 1), d(2026, 4, 30)),
        )
    }

    @Test
    fun `anual em 29 de fevereiro e semanal`() {
        assertEquals(
            listOf(d(2028, 2, 29), d(2029, 2, 28), d(2030, 2, 28), d(2031, 2, 28), d(2032, 2, 29)),
            RecurrenceScheduler.occurrences(recurring(Frequency.YEARLY, d(2028, 2, 29)), d(2028, 1, 1), d(2032, 12, 31)),
        )
        assertEquals(
            listOf(d(2026, 11, 2), d(2026, 11, 9), d(2026, 11, 16)),
            RecurrenceScheduler.occurrences(recurring(Frequency.WEEKLY, d(2026, 11, 2)), d(2026, 11, 1), d(2026, 11, 20)),
        )
    }

    @Test
    fun `respeita a data de fim`() {
        val withEnd = recurring().copy(endDate = d(2026, 3, 15))
        assertEquals(listOf(d(2026, 1, 31), d(2026, 2, 28)), RecurrenceScheduler.occurrences(withEnd, d(2026, 1, 1), d(2026, 12, 31)))
        assertNull(RecurrenceScheduler.nextOccurrence(withEnd, d(2026, 3, 1)))
    }

    @Test
    fun `próxima ocorrência`() {
        assertEquals(d(2026, 11, 30), RecurrenceScheduler.nextOccurrence(recurring(), d(2026, 11, 10)))
        assertEquals(d(2026, 1, 31), RecurrenceScheduler.nextOccurrence(recurring(), d(2025, 6, 1)))
    }

    @Test
    fun `gera só depois do que já foi gerado e nunca para pausadas`() {
        val r = recurring(start = d(2026, 9, 10)).copy(generatedUntil = d(2026, 10, 31))
        val pending = RecurrenceScheduler.pending(r, RecurrenceScheduler.horizon(d(2026, 11, 5)))
        assertEquals(listOf(d(2026, 11, 10)), pending.map { it.date })
        assertTrue(pending.all { it.recurringId == 7L && !it.paid })
        assertEquals(emptyList<Any>(), RecurrenceScheduler.pending(r.copy(active = false), d(2026, 12, 31)))
    }

    @Test
    fun `no cartão a ocorrência já conta como compra`() {
        val onCard = recurring().copy(accountId = null, cardId = 3)
        assertTrue(RecurrenceScheduler.toTransaction(onCard, d(2026, 1, 31)).paid)
    }

    @Test
    fun `custo mensal equivalente`() {
        assertEquals(150_000, RecurrenceScheduler.monthlyEquivalent(recurring()))
        assertEquals(10_000, RecurrenceScheduler.monthlyEquivalent(recurring(Frequency.YEARLY).copy(amountCents = 120_000)))
        assertEquals(43_333, RecurrenceScheduler.monthlyEquivalent(recurring(Frequency.WEEKLY).copy(amountCents = 10_000)))
    }

    @Test
    fun `regras da recorrência reaproveitam as do lançamento`() {
        assertEquals(emptyList<String>(), RecurringRules.violations(recurring()))
        assertFalse(RecurringRules.violations(recurring().copy(amountCents = 0)).isEmpty())
        assertFalse(RecurringRules.violations(recurring().copy(type = TransactionType.TRANSFER)).isEmpty())
        assertFalse(RecurringRules.violations(recurring().copy(cardId = 3)).isEmpty())
        assertFalse(RecurringRules.violations(recurring().copy(endDate = d(2025, 1, 1))).isEmpty())
    }

    @Test
    fun `ao criar no passado começa no mês atual, ao retomar a partir de hoje`() {
        val today = d(2026, 11, 10)
        assertEquals(d(2026, 10, 31), RecurrenceScheduler.initialCursor(d(2026, 9, 5), today))
        assertNull(RecurrenceScheduler.initialCursor(d(2026, 11, 1), today))
        assertNull(RecurrenceScheduler.initialCursor(d(2026, 12, 1), today))
        assertEquals(d(2026, 11, 9), RecurrenceScheduler.restartCursor(d(2026, 9, 5), today))
        assertNull(RecurrenceScheduler.restartCursor(d(2026, 11, 10), today))
    }

    @Test
    fun `semanal antiga pula direto para o período pedido`() {
        val old = recurring(Frequency.WEEKLY, d(2010, 1, 4))
        assertEquals(listOf(d(2026, 11, 2), d(2026, 11, 9)), RecurrenceScheduler.occurrences(old, d(2026, 11, 1), d(2026, 11, 10)))
        assertEquals(d(2026, 11, 16), RecurrenceScheduler.nextOccurrence(old, d(2026, 11, 10)))
    }

    @Test
    fun `com prazo termina na última parcela e numera cada lançamento`() {
        val loan = recurring().let { it.copy(endDate = RecurrenceScheduler.endDateFor(it, 3)) }
        assertEquals(d(2026, 3, 31), loan.endDate)
        assertEquals(3, RecurrenceScheduler.totalOccurrences(loan))
        assertEquals(
            listOf("Aluguel (1/3)", "Aluguel (2/3)", "Aluguel (3/3)"),
            RecurrenceScheduler.pending(loan, d(2026, 12, 31)).map { it.description },
        )
        assertEquals("Aluguel", RecurrenceScheduler.toTransaction(recurring(), d(2026, 2, 28)).description)
        assertEquals(null, RecurrenceScheduler.totalOccurrences(recurring()))
    }

    @Test
    fun `prazo precisa de ao menos uma parcela`() {
        val error = runCatching { RecurrenceScheduler.endDateFor(recurring(), 0) }.exceptionOrNull()
        assertEquals("Informe de 1 a ${RecurrenceScheduler.MAX_OCCURRENCES} parcelas.", error?.message)
    }
}
