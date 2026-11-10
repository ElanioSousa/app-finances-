package com.example.app_finances.domain

import com.example.app_finances.data.local.entity.TransactionType.EXPENSE
import com.example.app_finances.data.local.entity.TransactionType.INCOME
import com.example.app_finances.data.local.entity.TransactionType.TRANSFER
import org.junit.Assert.assertEquals
import org.junit.Test

class MonthTotalsTest {

    @Test
    fun `soma receitas e despesas, pagas ou pendentes, e ignora transferências`() {
        val totals = MonthTotals.of(
            listOf(
                transaction(INCOME, 500_000),
                transaction(EXPENSE, 120_000),
                transaction(EXPENSE, 30_000).copy(paid = false),
                transaction(EXPENSE, 9_990, accountId = null).copy(cardId = 1),
                transaction(TRANSFER, 50_000).copy(destinationAccountId = 2),
            ),
        )
        assertEquals(500_000, totals.incomeCents)
        assertEquals(159_990, totals.expenseCents)
        assertEquals(340_010, totals.resultCents)
    }

    @Test
    fun `lista vazia dá zero`() {
        assertEquals(MonthTotals(), MonthTotals.of(emptyList()))
    }
}
