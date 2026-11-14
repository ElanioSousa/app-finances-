package com.example.app_finances.domain

import com.example.app_finances.data.local.entity.Frequency
import com.example.app_finances.data.local.entity.RecurringEntity
import com.example.app_finances.data.local.entity.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class MonthForecastTest {

    private val today = LocalDate.of(2026, 11, 14)
    private fun nov(day: Int) = LocalDate.of(2026, 11, day)

    private val salary = RecurringEntity(
        id = 1, description = "Salário", amountCents = 10_000, type = TransactionType.INCOME, accountId = 1,
        frequency = Frequency.MONTHLY, startDate = LocalDate.of(2026, 1, 28), generatedUntil = LocalDate.of(2026, 10, 28),
    )

    @Test
    fun somaOQueEntraETiraOQueSaiAteOFimDoMes() {
        val pending = listOf(
            transaction(amount = 30_000, date = nov(20)).copy(paid = false),
            transaction(TransactionType.INCOME, amount = 50_000, date = nov(25)).copy(paid = false),
            // Atrasada ainda vai sair; dezembro e o que já foi pago não contam.
            transaction(amount = 2_000, date = nov(1)).copy(paid = false),
            transaction(amount = 5_000, date = LocalDate.of(2026, 12, 2)).copy(paid = false),
            transaction(amount = 9_999, date = nov(20)).copy(paid = true),
        )
        // Fecha dia 5 e vence dia 25: a compra de 01/11 está na fatura que vence em 25/11.
        val dueThisMonth = CardSummary.of(card(id = 1, closing = 5, due = 25), listOf(cardPurchase(1, 4_000, nov(1))), emptyList(), today)
        // Fecha dia 1 e vence dia 7: a compra de 10/11 só vence em dezembro.
        val dueNextMonth = CardSummary.of(card(id = 2, closing = 1, due = 7), listOf(cardPurchase(2, 7_000, nov(10))), emptyList(), today)

        val forecast = MonthForecast.endOfMonth(100_000, pending, listOf(salary), listOf(dueThisMonth, dueNextMonth), today)

        assertEquals(100_000 - 30_000 + 50_000 - 2_000 + 10_000 - 4_000, forecast)
    }

    @Test
    fun recorrenciaJaLancadaNaoContaDuasVezes() {
        val generated = salary.copy(generatedUntil = nov(28))
        assertEquals(100_000L, MonthForecast.endOfMonth(100_000, emptyList(), listOf(generated), emptyList(), today))
    }

    private fun cardPurchase(cardId: Long, amount: Long, date: LocalDate) =
        transaction(amount = amount, accountId = null, date = date).copy(cardId = cardId)
}
