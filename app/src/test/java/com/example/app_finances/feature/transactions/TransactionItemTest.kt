package com.example.app_finances.feature.transactions

import com.example.app_finances.core.model.Catalog
import com.example.app_finances.core.model.toItems
import com.example.app_finances.data.local.entity.CategoryEntity
import com.example.app_finances.data.local.entity.CategoryKind
import com.example.app_finances.data.local.entity.TransactionType
import com.example.app_finances.domain.account
import com.example.app_finances.domain.transaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TransactionItemTest {

    private val food = CategoryEntity(id = 1, name = "Alimentação", kind = CategoryKind.EXPENSE, iconKey = "food", colorArgb = 0)
    private val catalog = Catalog(listOf(food), listOf(account(1).copy(name = "Carteira"), account(2).copy(name = "Banco")))
    private val day1 = LocalDate.of(2026, 11, 10)
    private val day2 = LocalDate.of(2026, 11, 9)

    @Test
    fun `junta categoria e nomes das contas`() {
        val expense = listOf(transaction(date = day1).copy(categoryId = 1)).toItems(catalog).single()
        assertEquals("Alimentação · Carteira", expense.subtitle)

        val transfer = listOf(transaction(TransactionType.TRANSFER).copy(destinationAccountId = 2)).toItems(catalog).single()
        assertEquals("Carteira → Banco", transfer.subtitle)
    }

    @Test
    fun `agrupa por dia mantendo a ordem`() {
        val items = listOf(transaction(date = day1), transaction(date = day1), transaction(date = day2)).toItems(catalog)
        val groups = items.groupByDay()
        assertEquals(listOf(day1, day2), groups.map { it.date })
        assertEquals(listOf(2, 1), groups.map { it.items.size })
    }

    @Test
    fun `filtra por tipo, categoria, conta e busca`() {
        val item = listOf(transaction().copy(categoryId = 1, description = "Mercado", notes = "feira")).toItems(catalog).single()
        assertTrue(TransactionFilter().matches(item))
        assertTrue(TransactionFilter(type = TransactionType.EXPENSE, categoryId = 1, accountId = 1).matches(item))
        assertFalse(TransactionFilter(type = TransactionType.INCOME).matches(item))
        assertFalse(TransactionFilter(accountId = 2).matches(item))
        assertTrue(TransactionFilter(query = "merc").matches(item))
        assertTrue(TransactionFilter(query = "FEIRA").matches(item))
        assertTrue(TransactionFilter(query = "alimenta").matches(item))
        assertFalse(TransactionFilter(query = "farmácia").matches(item))
    }

    @Test
    fun `filtro de conta inclui transferências recebidas`() {
        val transfer = listOf(transaction(TransactionType.TRANSFER).copy(destinationAccountId = 2)).toItems(catalog).single()
        assertTrue(TransactionFilter(accountId = 2).matches(transfer))
    }

    @Test
    fun `filtro vazio não conta como ativo`() {
        assertFalse(TransactionFilter().isActive)
        assertTrue(TransactionFilter(query = "x").isActive)
    }
}
