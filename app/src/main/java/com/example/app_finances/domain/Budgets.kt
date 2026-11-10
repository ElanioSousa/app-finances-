package com.example.app_finances.domain

import com.example.app_finances.data.local.entity.CategoryEntity
import com.example.app_finances.data.local.entity.TransactionEntity
import com.example.app_finances.data.local.entity.TransactionType

/** Situação de um orçamento no mês. */
enum class BudgetLevel { OK, WARNING, OVER }

/** Quanto já foi gasto numa categoria com orçamento. */
data class BudgetStatus(
    val category: CategoryEntity,
    val spentCents: Long,
    val budgetCents: Long,
) {
    val fraction: Float get() = spentCents.toFloat() / budgetCents
    val remainingCents: Long get() = budgetCents - spentCents

    /** Atenção a partir de 80% do orçamento; estourado quando passa de 100%. */
    val level: BudgetLevel
        get() = when {
            spentCents > budgetCents -> BudgetLevel.OVER
            fraction >= Budgets.WARNING_FRACTION -> BudgetLevel.WARNING
            else -> BudgetLevel.OK
        }
}

/** Orçamento mensal por categoria de despesa (compras no cartão contam pela data da compra). */
object Budgets {
    const val WARNING_FRACTION = 0.8f

    /** Categorias com orçamento e o gasto delas em [monthTransactions]; as mais apertadas primeiro. */
    fun statuses(categories: List<CategoryEntity>, monthTransactions: List<TransactionEntity>): List<BudgetStatus> {
        val spentByCategory = monthTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.categoryId }
            .mapValues { (_, list) -> list.sumOf { it.amountCents } }
        return categories
            .filterNot { it.archived }
            .mapNotNull { category ->
                val budget = category.monthlyBudgetCents?.takeIf { it > 0 } ?: return@mapNotNull null
                BudgetStatus(category, spentByCategory[category.id] ?: 0, budget)
            }
            .sortedByDescending { it.fraction }
    }
}
