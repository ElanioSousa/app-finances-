package com.example.app_finances.domain

import com.example.app_finances.data.local.entity.CreditCardEntity
import com.example.app_finances.data.local.entity.InvoicePaymentEntity
import com.example.app_finances.data.local.entity.RecurringEntity
import com.example.app_finances.data.local.entity.TransactionEntity
import com.example.app_finances.data.local.entity.TransactionType
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/** O que a previsão usa: o saldo de hoje, as pendências, as recorrências e os cartões. */
data class ForecastInputs(
    val balanceCents: Long,
    val pending: List<TransactionEntity>,
    val recurrings: List<RecurringEntity>,
    val cards: List<CreditCardEntity>,
    val cardPurchases: List<TransactionEntity>,
    val invoicePayments: List<InvoicePaymentEntity>,
)

/** Quanto deve haver nas contas no último dia de [month]. */
data class MonthBalance(val month: YearMonth, val endCents: Long)

/**
 * Saldo previsto no fim de cada mês, a partir de hoje: o do mês anterior, mais o que entra e
 * menos o que sai das contas no mês (pendências e recorrências ainda não lançadas), menos as
 * faturas de cartão que vencem nele (com parcelas e assinaturas futuras). O mês atual também
 * leva o que está atrasado (pendências e faturas vencidas e não pagas).
 */
object MonthForecast {
    const val HORIZON_MONTHS = 6

    /** Faturas de até 3 meses antes ainda podem estar em aberto (ou vencer depois). */
    private const val INVOICE_LOOKBACK = 3L

    fun endOfMonth(inputs: ForecastInputs, today: LocalDate): Long = months(inputs, today, count = 1).first().endCents

    fun months(inputs: ForecastInputs, today: LocalDate, count: Int = HORIZON_MONTHS): List<MonthBalance> {
        val first = YearMonth.from(today)
        val last = first.plusMonths(count - 1L)
        // O que venceu antes (atrasado) cai no mês atual; depois do horizonte, fica de fora.
        val slotOf = { month: YearMonth -> ChronoUnit.MONTHS.between(first, maxOf(month, first)).toInt().takeIf { it < count } }
        val projected = RecurrenceScheduler.projected(inputs.recurrings, today, last.atEndOfMonth(), inputs.pending + inputs.cardPurchases)
        val deltas = LongArray(count)
        (inputs.pending.filter { !it.paid } + projected).filter { it.cardId == null }.forEach { item ->
            slotOf(YearMonth.from(item.date))?.let { deltas[it] += signedAmount(item) }
        }
        invoices(inputs, inputs.cardPurchases + projected.filter { it.cardId != null }, first, last).forEach { invoice ->
            slotOf(YearMonth.from(invoice.dueDate))?.let { deltas[it] -= invoice.remainingCents }
        }
        return deltas.toList().runningFold(inputs.balanceCents, Long::plus).drop(1)
            .mapIndexed { index, end -> MonthBalance(first.plusMonths(index.toLong()), end) }
    }

    /** Cada fatura de cada cartão uma vez, de alguns meses atrás (ainda em aberto) até o fim do horizonte. */
    private fun invoices(inputs: ForecastInputs, purchases: List<TransactionEntity>, first: YearMonth, last: YearMonth): List<Invoice> {
        val byCard = purchases.groupBy { it.cardId }
        return inputs.cards.flatMap { card ->
            val ofCard = byCard[card.id].orEmpty()
            generateSequence(first.minusMonths(INVOICE_LOOKBACK)) { it.plusMonths(1) }.takeWhile { it <= last }
                .map { InvoiceCalculator.invoice(card, it, ofCard, inputs.invoicePayments) }
                .toList()
        }
    }

    /** Receita soma, despesa subtrai; transferência só muda o dinheiro de conta. */
    private fun signedAmount(t: TransactionEntity): Long = when (t.type) {
        TransactionType.INCOME -> t.amountCents
        TransactionType.EXPENSE -> -t.amountCents
        TransactionType.TRANSFER -> 0
    }
}
