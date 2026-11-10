package com.example.app_finances.domain

import com.example.app_finances.data.local.entity.TransactionEntity
import com.example.app_finances.data.local.entity.TransactionType

/**
 * Receitas e despesas de um mês, pagas ou não (visão de competência).
 * Compras no cartão contam pela data da compra; transferências não são
 * receita nem despesa, só mudam o dinheiro de lugar.
 */
data class MonthTotals(
    val incomeCents: Long = 0,
    val expenseCents: Long = 0,
) {
    val resultCents: Long get() = incomeCents - expenseCents

    companion object {
        fun of(transactions: List<TransactionEntity>): MonthTotals =
            transactions.fold(MonthTotals()) { totals, t ->
                when (t.type) {
                    TransactionType.INCOME -> totals.copy(incomeCents = totals.incomeCents + t.amountCents)
                    TransactionType.EXPENSE -> totals.copy(expenseCents = totals.expenseCents + t.amountCents)
                    TransactionType.TRANSFER -> totals
                }
            }
    }
}
