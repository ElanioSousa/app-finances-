package com.example.app_finances.domain

import com.example.app_finances.data.local.entity.CategoryEntity
import com.example.app_finances.data.local.entity.CategoryKind
import com.example.app_finances.data.local.entity.CreditCardEntity
import com.example.app_finances.data.local.entity.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class QuickEntryParserTest {

    private val today = LocalDate.of(2026, 11, 13)
    private fun category(id: Long, name: String, kind: CategoryKind, icon: String) =
        CategoryEntity(id = id, name = name, kind = kind, iconKey = icon, colorArgb = 0)
    private fun card(id: Long, name: String) = CreditCardEntity(id = id, name = name, limitCents = 100_000, closingDay = 1, dueDay = 7, colorArgb = 0)

    private val catalog = QuickEntryCatalog(
        categories = listOf(
            category(1, "Alimentação", CategoryKind.EXPENSE, "food"),
            category(2, "Transporte", CategoryKind.EXPENSE, "transport"),
            category(3, "Compras", CategoryKind.EXPENSE, "shopping"),
            category(4, "Saúde", CategoryKind.EXPENSE, "health"),
            category(10, "Salário", CategoryKind.INCOME, "salary"),
        ),
        accounts = listOf(account(1).copy(name = "Carteira"), account(2).copy(name = "Banco Inter")),
        cards = listOf(card(7, "Nubank"), card(8, "Mercado Pago")),
    )

    private fun parse(text: String) = QuickEntryParser.parse(text, catalog, today)

    @Test
    fun `gasto simples com categoria pela palavra`() {
        val entry = parse("mercado 45,90")
        assertEquals(4_590L, entry.amountCents)
        assertEquals("Mercado", entry.description)
        assertEquals(1L, entry.categoryId)
        assertEquals(TransactionType.EXPENSE, entry.type)
        assertFalse(entry.onCard)
    }

    @Test
    fun `cartão pelo nome e parcelas`() {
        val entry = parse("geladeira 3.600 no Nubank em 10x")
        assertEquals(360_000L, entry.amountCents)
        assertEquals(7L, entry.cardId)
        assertTrue(entry.onCard)
        assertEquals(10, entry.installments)
        assertEquals("Geladeira", entry.description)
    }

    @Test
    fun `nome de cartão com duas palavras não vira categoria`() {
        val entry = parse("tênis 299,90 mercado pago 3 vezes")
        assertEquals(8L, entry.cardId)
        assertEquals(3, entry.installments)
        assertNull(entry.categoryId)
        assertEquals("Tênis", entry.description)
    }

    @Test
    fun `receita pelo verbo ou pela categoria`() {
        val salary = parse("recebi salário R$ 2.760,14")
        assertEquals(TransactionType.INCOME, salary.type)
        assertEquals(276_014L, salary.amountCents)
        assertEquals(10L, salary.categoryId)
        assertNull(salary.cardId)
        assertEquals(TransactionType.INCOME, parse("salário 2760,14").type)
    }

    @Test
    fun `datas, conta e número que não é o valor`() {
        assertEquals(today.minusDays(1), parse("farmácia 35 ontem").date)
        assertEquals(LocalDate.of(2026, 11, 2), parse("uber 18 02/11").date)
        assertEquals(2L, parse("almoço 32 banco inter").accountId)
        val uber = parse("uber 99 23,50")
        assertEquals(2_350L, uber.amountCents)
        assertEquals(2L, uber.categoryId)
    }

    @Test
    fun `parcelas sem cartão não valem e texto sem valor deixa o valor em branco`() {
        assertEquals(1, parse("curso 600 3x").installments)
        assertNull(parse("mercado").amountCents)
        assertEquals("Compras", catalog.categories.first { it.id == parse("mercado livre 80").categoryId }.name)
    }

    @Test
    fun `frase falada, depois de escrita, e texto digitado com número por extenso`() {
        val spoken = parse(SpokenText.toWritten("geladeira três mil e seiscentos reais no Nubank em dez vezes"))
        assertEquals(360_000L, spoken.amountCents)
        assertEquals(7L, spoken.cardId)
        assertEquals(10, spoken.installments)
        assertEquals("Geladeira", spoken.description)
        assertEquals("Coca zero", parse("coca zero 8").description)
    }

    @Test
    fun `valores em vários formatos`() {
        assertEquals(123_456L, parseCents("1.234,56"))
        assertEquals(4_590L, parseCents("45.90"))
        assertEquals(4_590L, parseCents("45,9"))
        assertEquals(100_000L, parseCents("1.000"))
        assertNull(parseCents("0"))
    }
}
