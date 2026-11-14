package com.example.app_finances.domain

import com.example.app_finances.data.local.entity.TransactionEntity

/** Resultado de conferir a fatura do app com o total da fatura do banco. */
sealed interface InvoiceCheck {
    data object Match : InvoiceCheck

    /** O banco cobra mais: falta lançar [cents] no app (ou uma compra caiu em outra fatura). */
    data class Missing(val cents: Long) : InvoiceCheck

    /** O app tem [cents] a mais; [suspects] são as compras com exatamente esse valor (duplicada? fatura errada?). */
    data class Extra(val cents: Long, val suspects: List<TransactionEntity>) : InvoiceCheck

    companion object {
        fun of(invoice: Invoice, bankCents: Long): InvoiceCheck {
            val diff = invoice.totalCents - bankCents
            return when {
                diff == 0L -> Match
                diff < 0 -> Missing(-diff)
                else -> Extra(diff, invoice.purchases.filter { it.amountCents == diff })
            }
        }
    }
}
