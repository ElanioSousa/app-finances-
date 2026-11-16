package com.example.app_finances.domain

import com.example.app_finances.data.local.entity.CategoryEntity
import java.time.LocalDate

/** Quantas vezes uma descrição foi lançada com uma categoria (e a última vez). Vem do banco. */
data class CategoryUse(val description: String, val categoryId: Long, val uses: Int, val lastDate: LocalDate)

/**
 * A categoria que o usuário costuma dar a cada descrição ("Lifestyleacai" → Alimentação):
 * a mais usada; no empate, a mais recente. "Mercado (2/3)" conta como "Mercado".
 */
class CategoryMemory(uses: List<CategoryUse>) {

    private val byDescription: Map<String, Long> = uses
        .groupBy { keyOf(it.description) }
        .filterKeys { it.isNotEmpty() }
        .mapValues { (_, list) ->
            list.groupBy { it.categoryId }
                .maxWith(compareBy<Map.Entry<Long, List<CategoryUse>>>({ e -> e.value.sumOf { it.uses } }, { e -> e.value.maxOf { it.lastDate } }))
                .key
        }

    /** Categoria lembrada para [description], se for uma das [eligible] (ativas e do tipo certo). */
    fun categoryFor(description: String, eligible: List<CategoryEntity>): CategoryEntity? {
        val id = byDescription[keyOf(description)] ?: return null
        return eligible.firstOrNull { it.id == id && !it.archived }
    }

    companion object {
        val EMPTY = CategoryMemory(emptyList())

        private val INSTALLMENT_SUFFIX = Regex("""\s*\(\d+/\d+\)\s*$""")
        private val SPACES = Regex("\\s+")

        private fun keyOf(description: String): String =
            normalize(description.replace(INSTALLMENT_SUFFIX, "")).replace(SPACES, " ").trim()
    }
}
