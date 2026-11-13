package com.example.app_finances.domain

import com.example.app_finances.data.local.entity.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class LedgerMonthsTest {

    // Fecha dia 6 e vence dia 13 (mesmo mês); o outro fecha dia 28 e vence dia 5 do mês seguinte.
    private val nubank = card(id = 7, closing = 6, due = 13)
    private val lateDue = card(id = 8, closing = 28, due = 5)
    private val months = LedgerMonths(listOf(nubank, lateDue))

    private fun onCard(cardId: Long, date: LocalDate): TransactionEntity =
        transaction(accountId = null, date = date).copy(cardId = cardId)

    private fun d(month: Int, day: Int) = LocalDate.of(2026, month, day)

    @Test
    fun `na conta conta o mês da data`() {
        assertEquals(YearMonth.of(2026, 11), months.of(transaction(date = d(11, 13))))
    }

    @Test
    fun `no cartão conta o mês em que a fatura vence`() {
        assertEquals(YearMonth.of(2026, 11), months.of(onCard(7, d(11, 2))))
        assertEquals(YearMonth.of(2026, 12), months.of(onCard(7, d(11, 13))))
        assertEquals(YearMonth.of(2026, 12), months.of(onCard(8, d(11, 13))))
        assertEquals(YearMonth.of(2027, 1), months.of(onCard(8, d(11, 29))))
    }

    @Test
    fun `parcelas de uma compra após o fechamento começam no mês seguinte`() {
        val purchase = onCard(7, d(11, 13)).copy(amountCents = 41_700)
        val parcels = Installments.schedule(purchase, 5) { "g" }
        val november = YearMonth.of(2026, 11)
        assertEquals(emptyList<TransactionEntity>(), months.within(parcels, november, november))
        assertEquals(listOf(1), months.within(parcels, november.plusMonths(1), november.plusMonths(1)).map { it.installmentNumber })
        assertEquals(5, months.within(parcels, november.plusMonths(1), november.plusMonths(5)).size)
    }

    @Test
    fun `cartão desconhecido usa a data`() {
        assertEquals(YearMonth.of(2026, 11), months.of(onCard(99, d(11, 29))))
    }

    @Test
    fun `atraso máximo cobre o pior caso`() {
        val worst = months.of(onCard(8, d(12, 31)))
        assertEquals(YearMonth.of(2026, 12).plusMonths(LedgerMonths.MAX_CARD_DELAY_MONTHS), worst)
    }
}
