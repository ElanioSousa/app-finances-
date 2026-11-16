package com.example.app_finances.domain

import com.example.app_finances.data.local.entity.CategoryEntity
import com.example.app_finances.data.local.entity.CategoryKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class CategoryMemoryTest {

    private fun use(description: String, category: Long, uses: Int, day: Int) =
        CategoryUse(description, category, uses, LocalDate.of(2026, 11, day))

    private fun category(id: Long, archived: Boolean = false) =
        CategoryEntity(id = id, name = "C$id", kind = CategoryKind.EXPENSE, iconKey = "other", colorArgb = 0, archived = archived)

    private val all = (1L..9L).map { category(it) }

    @Test
    fun aMaisUsadaVenceENoEmpateAMaisRecente() {
        val memory = CategoryMemory(listOf(use("Mercado", 1, 5, 1), use("Mercado", 2, 2, 14), use("Uber", 3, 1, 1), use("Uber", 4, 1, 10)))
        assertEquals(1L, memory.categoryFor("mercado", all)?.id)
        assertEquals(4L, memory.categoryFor("Uber", all)?.id)
    }

    @Test
    fun ignoraAcentosMaiusculasEParcela() {
        val memory = CategoryMemory(listOf(use("Farmácia São João (1/3)", 9, 1, 1), use("Farmácia São João (2/3)", 9, 1, 2)))
        assertEquals(9L, memory.categoryFor("farmacia sao joao", all)?.id)
        assertNull(memory.categoryFor("farmacia", all))
    }

    @Test
    fun soDevolveCategoriaQueAindaServe() {
        val memory = CategoryMemory(listOf(use("Mercado", 1, 5, 1)))
        assertNull(memory.categoryFor("Mercado", listOf(category(1, archived = true))))
        assertNull(memory.categoryFor("Mercado", listOf(category(2))))
    }
}
