package com.example.app_finances.domain

import com.example.app_finances.data.local.entity.CreditCardEntity
import com.example.app_finances.data.local.entity.InvoicePaymentEntity
import com.example.app_finances.data.local.entity.TransactionEntity
import java.time.LocalDate
import java.time.YearMonth

/** Situação de uma fatura numa data. */
enum class InvoiceStatus { OPEN, CLOSED, PAID, OVERDUE }

/**
 * Fatura de [month] de um cartão: o mês é aquele em que ela **fecha**.
 * Compras feitas a partir do dia do fechamento entram na fatura seguinte.
 */
data class Invoice(
    val month: YearMonth,
    val closingDate: LocalDate,
    val dueDate: LocalDate,
    val purchases: List<TransactionEntity>,
    val paidCents: Long,
) {
    val totalCents: Long = purchases.sumOf { it.amountCents }
    val remainingCents: Long get() = (totalCents - paidCents).coerceAtLeast(0)

    fun status(today: LocalDate): InvoiceStatus = when {
        today < closingDate -> InvoiceStatus.OPEN
        remainingCents == 0L -> InvoiceStatus.PAID
        today <= dueDate -> InvoiceStatus.CLOSED
        else -> InvoiceStatus.OVERDUE
    }
}

/** Datas de fatura de um cartão. Dias 29–31 viram o último dia nos meses mais curtos. */
object InvoiceCalculator {

    fun closingDate(month: YearMonth, closingDay: Int): LocalDate = month.atClampedDay(closingDay)

    /** Vencimento: no mesmo mês se vence depois do fechamento; senão, no mês seguinte. */
    fun dueDate(month: YearMonth, card: CreditCardEntity): LocalDate {
        val dueMonth = if (card.dueDay > card.closingDay) month else month.plusMonths(1)
        return dueMonth.atClampedDay(card.dueDay)
    }

    /** Fatura em que cai uma compra feita em [date]. */
    fun invoiceMonthOf(date: LocalDate, closingDay: Int): YearMonth {
        val month = YearMonth.from(date)
        return if (date < closingDate(month, closingDay)) month else month.plusMonths(1)
    }

    fun invoice(
        card: CreditCardEntity,
        month: YearMonth,
        purchases: List<TransactionEntity>,
        payments: List<InvoicePaymentEntity>,
    ): Invoice = Invoice(
        month = month,
        closingDate = closingDate(month, card.closingDay),
        dueDate = dueDate(month, card),
        purchases = purchases.filter { it.cardId == card.id && invoiceMonthOf(it.date, card.closingDay) == month },
        paidCents = payments.filter { it.cardId == card.id && it.referenceMonth == month }.sumOf { it.amountCents },
    )

    /** Limite em uso: tudo o que foi comprado e ainda não foi pago (inclui parcelas futuras). */
    fun usedLimit(card: CreditCardEntity, purchases: List<TransactionEntity>, payments: List<InvoicePaymentEntity>): Long {
        val bought = purchases.filter { it.cardId == card.id }.sumOf { it.amountCents }
        val paid = payments.filter { it.cardId == card.id }.sumOf { it.amountCents }
        return (bought - paid).coerceAtLeast(0)
    }

    private fun YearMonth.atClampedDay(day: Int): LocalDate = atDay(day.coerceIn(1, lengthOfMonth()))
}
