package com.example.app_finances.domain

import com.example.app_finances.data.local.entity.CreditCardEntity
import com.example.app_finances.data.local.entity.InvoicePaymentEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class InvoiceCalculatorTest {

    private fun card(closing: Int, due: Int) =
        CreditCardEntity(id = 1, name = "Cartão", limitCents = 500_000, closingDay = closing, dueDay = due, colorArgb = 0)

    private fun date(y: Int, m: Int, d: Int) = LocalDate.of(y, m, d)

    @Test
    fun `compra antes do fechamento fica na fatura do mês, a partir dele vai para a próxima`() {
        assertEquals(YearMonth.of(2026, 11), InvoiceCalculator.invoiceMonthOf(date(2026, 11, 4), closingDay = 5))
        assertEquals(YearMonth.of(2026, 12), InvoiceCalculator.invoiceMonthOf(date(2026, 11, 5), closingDay = 5))
        assertEquals(YearMonth.of(2026, 12), InvoiceCalculator.invoiceMonthOf(date(2026, 11, 30), closingDay = 5))
    }

    @Test
    fun `virada de ano`() {
        assertEquals(YearMonth.of(2027, 1), InvoiceCalculator.invoiceMonthOf(date(2026, 12, 20), closingDay = 10))
    }

    @Test
    fun `fechamento no dia 31 vira o último dia de meses curtos`() {
        assertEquals(date(2027, 2, 28), InvoiceCalculator.closingDate(YearMonth.of(2027, 2), 31))
        assertEquals(date(2028, 2, 29), InvoiceCalculator.closingDate(YearMonth.of(2028, 2), 31))
        assertEquals(YearMonth.of(2027, 3), InvoiceCalculator.invoiceMonthOf(date(2027, 2, 28), closingDay = 31))
        assertEquals(YearMonth.of(2027, 1), InvoiceCalculator.invoiceMonthOf(date(2027, 1, 30), closingDay = 31))
    }

    @Test
    fun `vencimento no mesmo mês ou no seguinte`() {
        assertEquals(date(2026, 11, 15), InvoiceCalculator.dueDate(YearMonth.of(2026, 11), card(closing = 5, due = 15)))
        assertEquals(date(2026, 12, 5), InvoiceCalculator.dueDate(YearMonth.of(2026, 11), card(closing = 25, due = 5)))
        assertEquals(date(2027, 1, 5), InvoiceCalculator.dueDate(YearMonth.of(2026, 12), card(closing = 25, due = 5)))
    }

    @Test
    fun `fatura junta só as compras e pagamentos do mês e do cartão`() {
        val card = card(closing = 5, due = 15)
        val purchases = listOf(
            transaction(amount = 10_000, accountId = null, date = date(2026, 10, 10)).copy(cardId = 1),
            transaction(amount = 5_000, accountId = null, date = date(2026, 11, 4)).copy(cardId = 1),
            transaction(amount = 7_000, accountId = null, date = date(2026, 11, 5)).copy(cardId = 1),
            transaction(amount = 9_000, accountId = null, date = date(2026, 11, 1)).copy(cardId = 2),
        )
        val payments = listOf(payment(YearMonth.of(2026, 11), 4_000), payment(YearMonth.of(2026, 10), 99_999))
        val invoice = InvoiceCalculator.invoice(card, YearMonth.of(2026, 11), purchases, payments)
        assertEquals(15_000, invoice.totalCents)
        assertEquals(11_000, invoice.remainingCents)
    }

    @Test
    fun `situação da fatura ao longo do tempo`() {
        val card = card(closing = 5, due = 15)
        val purchases = listOf(transaction(amount = 10_000, accountId = null, date = date(2026, 11, 1)).copy(cardId = 1))
        val invoice = InvoiceCalculator.invoice(card, YearMonth.of(2026, 11), purchases, emptyList())
        assertEquals(InvoiceStatus.OPEN, invoice.status(date(2026, 11, 4)))
        assertEquals(InvoiceStatus.CLOSED, invoice.status(date(2026, 11, 5)))
        assertEquals(InvoiceStatus.CLOSED, invoice.status(date(2026, 11, 15)))
        assertEquals(InvoiceStatus.OVERDUE, invoice.status(date(2026, 11, 16)))

        val paid = InvoiceCalculator.invoice(card, YearMonth.of(2026, 11), purchases, listOf(payment(YearMonth.of(2026, 11), 10_000)))
        assertEquals(InvoiceStatus.PAID, paid.status(date(2026, 11, 20)))
    }

    @Test
    fun `limite usado considera tudo o que falta pagar`() {
        val card = card(closing = 5, due = 15)
        val purchases = listOf(
            transaction(amount = 30_000, accountId = null).copy(cardId = 1),
            transaction(amount = 20_000, accountId = null).copy(cardId = 1),
        )
        assertEquals(40_000, InvoiceCalculator.usedLimit(card, purchases, listOf(payment(YearMonth.of(2026, 11), 10_000))))
        assertEquals(0, InvoiceCalculator.usedLimit(card, emptyList(), listOf(payment(YearMonth.of(2026, 11), 10_000))))
    }

    private fun payment(month: YearMonth, cents: Long) =
        InvoicePaymentEntity(cardId = 1, referenceMonth = month, amountCents = cents, date = date(2026, 11, 10), accountId = 1)
}
