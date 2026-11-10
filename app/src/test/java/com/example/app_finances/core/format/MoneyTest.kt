package com.example.app_finances.core.format

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyTest {

    private fun brl(text: String) = text.replace(' ', ' ')

    @Test
    fun `formata centavos no padrão brasileiro`() {
        assertEquals(brl("R$ 0,00"), Money.format(0))
        assertEquals(brl("R$ 0,05"), Money.format(5))
        assertEquals(brl("R$ 12,30"), Money.format(1_230))
        assertEquals(brl("R$ 1.234,56"), Money.format(123_456))
        assertEquals(brl("R$ 1.000.000,00"), Money.format(100_000_000))
    }

    @Test
    fun `formata negativos e sinal de mais opcional`() {
        assertEquals(brl("-R$ 10,00"), Money.format(-1_000))
        assertEquals(brl("+R$ 10,00"), Money.format(1_000, showPlusSign = true))
        assertEquals(brl("R$ 0,00"), Money.format(0, showPlusSign = true))
        assertEquals(brl("-R$ 10,00"), Money.format(-1_000, showPlusSign = true))
    }

    @Test
    fun `digitação estilo caixa eletrônico considera só os dígitos`() {
        assertEquals(0, Money.parseTypedDigits(""))
        assertEquals(0, Money.parseTypedDigits(brl("R$ 0,00")))
        assertEquals(1, Money.parseTypedDigits(brl("R$ 0,001")))
        assertEquals(123, Money.parseTypedDigits(brl("R$ 0,0123")))
        // Apagar o último dígito de R$ 0,12 deixa R$ 0,01.
        assertEquals(1, Money.parseTypedDigits(brl("R$ 0,1")))
    }

    @Test
    fun `digitação respeita o valor máximo`() {
        assertEquals(Money.MAX_INPUT_CENTS, Money.parseTypedDigits("99999999999999999999999"))
    }
}
