package com.example.app_finances.domain

import com.example.app_finances.data.local.entity.CreditCardEntity
import com.example.app_finances.data.local.entity.InvoicePaymentEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class CardRulesTest {

    private val valid = CreditCardEntity(name = "Nubank", limitCents = 100_000, closingDay = 5, dueDay = 12, colorArgb = 0)

    @Test
    fun `cartão válido e inválidos`() {
        assertEquals(emptyList<String>(), CardRules.cardViolations(valid))
        assertTrue(CardRules.cardViolations(valid.copy(name = " ")).isNotEmpty())
        assertTrue(CardRules.cardViolations(valid.copy(limitCents = 0)).isNotEmpty())
        assertTrue(CardRules.cardViolations(valid.copy(closingDay = 32)).isNotEmpty())
        assertTrue(CardRules.cardViolations(valid.copy(dueDay = 5)).isNotEmpty())
    }

    @Test
    fun `pagamento não passa do restante`() {
        val payment = InvoicePaymentEntity(cardId = 1, referenceMonth = YearMonth.of(2026, 11), amountCents = 5_000, date = LocalDate.of(2026, 11, 10), accountId = 1)
        assertEquals(emptyList<String>(), CardRules.paymentViolations(payment, remainingCents = 5_000))
        assertTrue(CardRules.paymentViolations(payment, remainingCents = 4_999).isNotEmpty())
        assertTrue(CardRules.paymentViolations(payment.copy(amountCents = 0), remainingCents = 5_000).isNotEmpty())
    }
}
