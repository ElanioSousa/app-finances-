package com.example.app_finances.domain

import com.example.app_finances.data.local.entity.Frequency
import com.example.app_finances.data.local.entity.RecurringEntity
import com.example.app_finances.data.local.entity.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class MonthForecastTest {

    private val today = LocalDate.of(2026, 11, 14)
    private fun nov(day: Int) = LocalDate.of(2026, 11, day)

    private val salary = RecurringEntity(
        id = 1, description = "Salário", amountCents = 10_000, type = TransactionType.INCOME, accountId = 1,
        frequency = Frequency.MONTHLY, startDate = LocalDate.of(2026, 1, 28), generatedUntil = LocalDate.of(2026, 10, 28),
    )

    // Fecha dia 5 e vence dia 25: a compra de 01/11 vence em 25/11. Fecha dia 1 e vence dia 7: a de 10/11 vence em 07/12.
    private val dueThisMonthCard = card(id = 1, closing = 5, due = 25)
    private val dueNextMonthCard = card(id = 2, closing = 1, due = 7)

    private fun cardPurchase(cardId: Long, amount: Long, date: LocalDate) =
        transaction(amount = amount, accountId = null, date = date).copy(cardId = cardId)

    private fun inputs(pending: List<com.example.app_finances.data.local.entity.TransactionEntity> = emptyList(), recurrings: List<RecurringEntity> = listOf(salary)) = ForecastInputs(
        balanceCents = 100_000,
        pending = pending,
        recurrings = recurrings,
        cards = listOf(dueThisMonthCard, dueNextMonthCard),
        cardPurchases = listOf(cardPurchase(1, 4_000, nov(1)), cardPurchase(2, 7_000, nov(10))),
        invoicePayments = emptyList(),
    )

    @Test
    fun fimDoMesSomaOQueEntraETiraOQueSai() {
        val pending = listOf(
            transaction(amount = 30_000, date = nov(20)).copy(paid = false),
            transaction(TransactionType.INCOME, amount = 50_000, date = nov(25)).copy(paid = false),
            // Atrasada ainda vai sair; dezembro e o que já foi pago não contam neste mês.
            transaction(amount = 2_000, date = nov(1)).copy(paid = false),
            transaction(amount = 5_000, date = LocalDate.of(2026, 12, 2)).copy(paid = false),
            transaction(amount = 9_999, date = nov(20)).copy(paid = true),
        )
        assertEquals(100_000 - 30_000 + 50_000 - 2_000 + 10_000 - 4_000, MonthForecast.endOfMonth(inputs(pending), today))
    }

    @Test
    fun proximosMesesLevamFaturasPendenciasERecorrencias() {
        val pending = listOf(transaction(amount = 5_000, date = LocalDate.of(2026, 12, 2)).copy(paid = false))
        val months = MonthForecast.months(inputs(pending), today, count = 3)
        assertEquals(listOf(YearMonth.of(2026, 11), YearMonth.of(2026, 12), YearMonth.of(2027, 1)), months.map { it.month })
        // Nov: +salário −fatura de 25/11. Dez: +salário −pendência −fatura de 07/12. Jan: +salário.
        assertEquals(listOf(106_000L, 104_000L, 114_000L), months.map { it.endCents })
    }

    @Test
    fun recorrenciaJaLancadaNaoContaDuasVezes() {
        val generated = salary.copy(generatedUntil = nov(28))
        val noCards = inputs(recurrings = listOf(generated)).copy(cards = emptyList())
        assertEquals(100_000L, MonthForecast.endOfMonth(noCards, today))
    }
}
