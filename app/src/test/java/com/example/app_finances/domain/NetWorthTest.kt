package com.example.app_finances.domain

import com.example.app_finances.data.local.entity.Frequency
import com.example.app_finances.data.local.entity.RecurringEntity
import com.example.app_finances.data.local.entity.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class NetWorthTest {

    private val today = LocalDate.of(2026, 11, 14)

    @Test
    fun temMenosDeveComCartaoEmprestimoEPendencias() {
        val pending = listOf(
            transaction(TransactionType.INCOME, amount = 80_000, date = LocalDate.of(2026, 11, 19)).copy(paid = false),
            transaction(amount = 15_000, date = LocalDate.of(2026, 11, 20)).copy(paid = false),
        )
        val nubank = card(id = 1, closing = 1, due = 7).copy(name = "Nubank")
        // Compra de R$ 300 em 3x: as três parcelas ocupam o limite.
        val purchases = (0..2).map { transaction(amount = 10_000, accountId = null, date = LocalDate.of(2026, 11, 10).plusMonths(it.toLong())).copy(cardId = 1) }
        val cards = listOf(CardSummary.of(nubank, purchases, emptyList(), today))
        // Empréstimo de 12x R$ 110 que já lançou até novembro: faltam dezembro a abril (5).
        val loan = RecurringEntity(
            description = "Empréstimo", amountCents = 11_000, type = TransactionType.EXPENSE, accountId = 1, frequency = Frequency.MONTHLY,
            startDate = LocalDate.of(2026, 5, 30), endDate = LocalDate.of(2027, 4, 30), generatedUntil = LocalDate.of(2026, 11, 30),
        )

        val worth = NetWorth.of(balancesCents = 50_000, pending = pending, cards = cards, recurrings = listOf(loan), today = today)

        assertEquals(130_000L, worth.assetsCents)
        assertEquals(listOf(NamedAmount("Nubank", 30_000)), worth.cards)
        assertEquals(listOf(NamedAmount("Empréstimo", 55_000)), worth.loans)
        assertEquals(15_000L, worth.payableCents)
        assertEquals(130_000L - 30_000 - 55_000 - 15_000, worth.netCents)
    }
}
