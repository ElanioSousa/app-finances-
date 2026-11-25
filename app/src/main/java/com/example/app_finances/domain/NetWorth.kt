package com.example.app_finances.domain

import com.example.app_finances.data.local.entity.RecurringEntity
import com.example.app_finances.data.local.entity.TransactionEntity
import com.example.app_finances.data.local.entity.TransactionType
import java.time.LocalDate

/** Um item do patrimônio: "Nubank R$ 2.174,57", "Empréstimo R$ 1.200,00". */
data class NamedAmount(val name: String, val cents: Long)

/**
 * O que você tem (saldo das contas e o que tem a receber) menos o que deve (cartões, com
 * as parcelas futuras; empréstimos com prazo, pelas parcelas que faltam; contas a pagar).
 */
data class NetWorth(
    val accountsCents: Long,
    val receivableCents: Long,
    val cards: List<NamedAmount>,
    val loans: List<NamedAmount>,
    val payableCents: Long,
) {
    val assetsCents: Long get() = accountsCents + receivableCents
    val debtsCents: Long get() = cards.sumOf { it.cents } + loans.sumOf { it.cents } + payableCents
    val netCents: Long get() = assetsCents - debtsCents

    companion object {
        fun of(balancesCents: Long, pending: List<TransactionEntity>, cards: List<CardSummary>, recurrings: List<RecurringEntity>, today: LocalDate): NetWorth {
            val open = pending.filter { !it.paid && it.cardId == null }
            return NetWorth(
                accountsCents = balancesCents,
                receivableCents = open.filter { it.type == TransactionType.INCOME }.sumOf { it.amountCents },
                cards = cards.map { NamedAmount(it.card.name, it.usedCents) }.filter { it.cents > 0 },
                loans = recurrings.mapNotNull { remainingLoan(it, today, open) },
                payableCents = open.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountCents },
            )
        }

        /** Despesa com prazo (empréstimo, financiamento): o que falta das parcelas ainda não lançadas. */
        private fun remainingLoan(recurring: RecurringEntity, today: LocalDate, open: List<TransactionEntity>): NamedAmount? {
            val end = recurring.endDate ?: return null
            if (recurring.type != TransactionType.EXPENSE || !recurring.active) return null
            // Parcela já lançada (pendente) conta em "contas a pagar", não aqui de novo.
            val remaining = RecurrenceScheduler.projected(listOf(recurring), today, end, open).sumOf { it.amountCents }
            return NamedAmount(recurring.description, remaining).takeIf { remaining > 0 }
        }
    }
}
