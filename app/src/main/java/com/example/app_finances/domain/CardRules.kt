package com.example.app_finances.domain

import com.example.app_finances.data.local.entity.CreditCardEntity
import com.example.app_finances.data.local.entity.InvoicePaymentEntity

/** Invariantes de cartões e pagamentos de fatura. */
object CardRules {

    fun cardViolations(card: CreditCardEntity): List<String> = buildList {
        if (card.name.isBlank()) add("Informe o nome do cartão.")
        if (card.limitCents <= 0) add("O limite deve ser maior que zero.")
        if (card.closingDay !in 1..31) add("Dia de fechamento inválido.")
        if (card.dueDay !in 1..31) add("Dia de vencimento inválido.")
        if (card.closingDay == card.dueDay) add("Fechamento e vencimento devem ser em dias diferentes.")
    }

    /** O pagamento precisa ser positivo e não pode passar do que falta pagar da fatura. */
    fun paymentViolations(payment: InvoicePaymentEntity, remainingCents: Long): List<String> = buildList {
        if (payment.amountCents <= 0) add("O valor deve ser maior que zero.")
        if (payment.amountCents > remainingCents) add("O valor passa do que falta pagar da fatura.")
    }

    fun requireNoViolations(violations: List<String>) {
        require(violations.isEmpty()) { violations.joinToString(" ") }
    }
}
