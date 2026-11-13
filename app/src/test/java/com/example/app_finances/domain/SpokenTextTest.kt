package com.example.app_finances.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class SpokenTextTest {

    private fun n(text: String) = SpokenText.toWritten(text)

    @Test
    fun `reais e centavos viram valor`() {
        assertEquals("mercado 45,90 no nubank", n("mercado 45 reais e 90 centavos no nubank"))
        assertEquals("uber 23,50", n("uber 23 reais e 50"))
        assertEquals("pão 8,00", n("pão 8 reais"))
        assertEquals("bala 0,50", n("bala 50 centavos"))
        assertEquals("gás 1.234,00", n("gás 1.234 reais"))
        assertEquals("gás 120,00", n("gás 120,00 reais"))
    }

    @Test
    fun `números por extenso`() {
        assertEquals("mercado 45,90 no nubank", n("mercado quarenta e cinco reais e noventa centavos no nubank"))
        assertEquals("mercado 45,90", n("mercado quarenta e cinco e noventa"))
        assertEquals("geladeira 3600 no nubank em 10 vezes", n("geladeira três mil e seiscentos no nubank em dez vezes"))
        assertEquals("tv 1500 em 2 vezes", n("tv mil e quinhentos em duas vezes"))
        assertEquals("almoço 125,00", n("almoço cento e vinte e cinco reais"))
        assertEquals("sofá 3600", n("sofá 3 mil e 600"))
    }

    @Test
    fun `artigo e texto digitado ficam como estão`() {
        assertEquals("comprei um tênis 200,00", n("comprei um tênis 200 reais"))
        assertEquals("1,00", n("um real"))
        assertEquals("mercado 45,90 nubank 13/11 5x", n("mercado 45,90 nubank 13/11 5x"))
        assertEquals("uber 2,50", n("uber 2 e 50"))
        assertEquals("camisa 80,00", n("camisa 80 reais à vista"))
        assertEquals("coca 0 8", n("coca zero 8"))
    }
}
