package com.example.app_finances.domain

import com.example.app_finances.data.local.entity.CreditCardEntity
import com.example.app_finances.data.local.entity.RecurringEntity
import com.example.app_finances.data.local.entity.TransactionEntity
import java.time.LocalDate
import java.time.YearMonth

/**
 * Mês em que cada lançamento conta nos totais, no gráfico, nos orçamentos e nos relatórios.
 * Na conta, é o mês da data. No cartão, é o mês em que vence a fatura da compra:
 * uma compra de 13/11 numa fatura que vence em 13/12 conta em dezembro.
 */
class LedgerMonths(cards: List<CreditCardEntity>) {
    private val cardById = cards.associateBy { it.id }

    fun of(transaction: TransactionEntity): YearMonth =
        transaction.cardId?.let(cardById::get)?.let { InvoiceCalculator.dueMonthOf(transaction.date, it) }
            ?: YearMonth.from(transaction.date)

    /** Só os lançamentos que contam entre [from] e [to]. */
    fun within(transactions: List<TransactionEntity>, from: YearMonth, to: YearMonth): List<TransactionEntity> =
        transactions.filter { of(it) in from..to }

    /** O que as recorrências ainda vão lançar e que conta entre [from] e [to]. */
    fun projected(recurrings: List<RecurringEntity>, from: YearMonth, to: YearMonth): List<TransactionEntity> =
        within(RecurrenceScheduler.projected(recurrings, firstDateFor(from), to.atEndOfMonth()), from, to)

    companion object {
        /**
         * Uma compra no cartão conta no máximo dois meses depois da data
         * (feita após o fechamento, numa fatura que vence no mês seguinte ao fechamento).
         */
        const val MAX_CARD_DELAY_MONTHS = 2L

        /** Primeira data a buscar para cobrir os lançamentos que contam a partir de [from]. */
        fun firstDateFor(from: YearMonth): LocalDate = from.minusMonths(MAX_CARD_DELAY_MONTHS).atDay(1)
    }
}

/** Lançamentos que contam de [from] até [to], com a regra de mês que os escolheu. */
data class Ledger(
    val from: YearMonth,
    val to: YearMonth,
    val transactions: List<TransactionEntity>,
    val months: LedgerMonths,
) {
    /** O que as recorrências ainda vão lançar no mesmo período. */
    fun projected(recurrings: List<RecurringEntity>): List<TransactionEntity> = months.projected(recurrings, from, to)
}
