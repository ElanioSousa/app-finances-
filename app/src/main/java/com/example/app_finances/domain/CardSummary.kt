package com.example.app_finances.domain

import com.example.app_finances.data.local.entity.CreditCardEntity
import com.example.app_finances.data.local.entity.InvoicePaymentEntity
import com.example.app_finances.data.local.entity.TransactionEntity
import java.time.LocalDate

/**
 * Visão rápida de um cartão: a fatura em aberto, a anterior se ainda falta pagar,
 * e quanto do limite está comprometido.
 */
data class CardSummary(
    val card: CreditCardEntity,
    val current: Invoice,
    val pending: Invoice?,
    val usedCents: Long,
    val currentStatus: InvoiceStatus,
) {
    val availableCents: Long get() = card.limitCents - usedCents

    /** Fração do limite em uso, de 0 a 1 (passa de 1 se estourou). */
    val usedFraction: Float get() = if (card.limitCents <= 0) 0f else usedCents.toFloat() / card.limitCents

    companion object {
        fun of(
            card: CreditCardEntity,
            purchases: List<TransactionEntity>,
            payments: List<InvoicePaymentEntity>,
            today: LocalDate,
        ): CardSummary {
            val currentMonth = InvoiceCalculator.invoiceMonthOf(today, card.closingDay)
            val current = InvoiceCalculator.invoice(card, currentMonth, purchases, payments)
            val previous = InvoiceCalculator.invoice(card, currentMonth.minusMonths(1), purchases, payments)
            return CardSummary(
                card = card,
                current = current,
                pending = previous.takeIf { it.remainingCents > 0 },
                usedCents = InvoiceCalculator.usedLimit(card, purchases, payments),
                currentStatus = current.status(today),
            )
        }

        /** Resumo de todos os cartões, separando compras e pagamentos por cartão uma vez só. */
        fun ofAll(
            cards: List<CreditCardEntity>,
            purchases: List<TransactionEntity>,
            payments: List<InvoicePaymentEntity>,
            today: LocalDate,
        ): List<CardSummary> {
            val purchasesByCard = purchases.groupBy { it.cardId }
            val paymentsByCard = payments.groupBy { it.cardId }
            return cards.map { of(it, purchasesByCard[it.id].orEmpty(), paymentsByCard[it.id].orEmpty(), today) }
        }
    }
}
