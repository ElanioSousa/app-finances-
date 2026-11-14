package com.example.app_finances.domain

import com.example.app_finances.data.local.entity.RecurringEntity
import com.example.app_finances.data.local.entity.TransactionEntity
import com.example.app_finances.data.local.entity.TransactionType
import java.time.LocalDate
import java.time.YearMonth

/**
 * Quanto deve haver nas contas no último dia do mês: o saldo de hoje, mais o que ainda
 * entra e menos o que ainda sai das contas até lá (pendências, inclusive atrasadas, e
 * recorrências ainda não lançadas), menos as faturas de cartão que vencem até o fim do mês.
 */
object MonthForecast {

    fun endOfMonth(balanceCents: Long, pending: List<TransactionEntity>, recurrings: List<RecurringEntity>, cards: List<CardSummary>, today: LocalDate): Long {
        val end = YearMonth.from(today).atEndOfMonth()
        val toCome = pending.filter { !it.paid } + RecurrenceScheduler.projected(recurrings, today, end)
        val accountFlow = toCome.filter { it.cardId == null && it.date <= end }.sumOf(::signedAmount)
        val invoices = cards.flatMap { listOfNotNull(it.pending, it.current) }.filter { it.dueDate <= end }.sumOf { it.remainingCents }
        return balanceCents + accountFlow - invoices
    }

    /** Receita soma, despesa subtrai; transferência só muda o dinheiro de conta. */
    private fun signedAmount(t: TransactionEntity): Long = when (t.type) {
        TransactionType.INCOME -> t.amountCents
        TransactionType.EXPENSE -> -t.amountCents
        TransactionType.TRANSFER -> 0
    }
}
