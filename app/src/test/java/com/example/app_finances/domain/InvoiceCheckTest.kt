package com.example.app_finances.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class InvoiceCheckTest {

    private val mercado = transaction(amount = 4_590).copy(description = "Mercado")
    private val uber = transaction(amount = 1_200).copy(description = "Uber")
    private val invoice = Invoice(YearMonth.of(2026, 12), LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 7), listOf(mercado, uber), paidCents = 0)

    @Test
    fun confere() = assertEquals(InvoiceCheck.Match, InvoiceCheck.of(invoice, 5_790))

    @Test
    fun bancoCobraMaisFaltaLancar() = assertEquals(InvoiceCheck.Missing(1_000), InvoiceCheck.of(invoice, 6_790))

    @Test
    fun appTemAMaisEApontaACompraComEsseValor() =
        assertEquals(InvoiceCheck.Extra(1_200, listOf(uber)), InvoiceCheck.of(invoice, 4_590))
}
