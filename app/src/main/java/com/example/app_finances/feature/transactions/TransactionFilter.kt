package com.example.app_finances.feature.transactions

import com.example.app_finances.core.model.TransactionItem
import com.example.app_finances.data.local.entity.TransactionType
import java.time.LocalDate

/** Lançamentos de um mesmo dia. */
data class DayGroup(val date: LocalDate, val items: List<TransactionItem>)

/** Agrupa por dia mantendo a ordem recebida (mais recentes primeiro). */
fun List<TransactionItem>.groupByDay(): List<DayGroup> =
    groupBy { it.transaction.date }.map { (date, items) -> DayGroup(date, items) }

/** Filtros da tela de lançamentos; campos nulos/vazios não filtram. */
data class TransactionFilter(
    val type: TransactionType? = null,
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val query: String = "",
) {
    val isActive: Boolean get() = this != TransactionFilter()

    fun matches(item: TransactionItem): Boolean {
        val t = item.transaction
        return (type == null || t.type == type) &&
            (categoryId == null || t.categoryId == categoryId) &&
            (accountId == null || t.accountId == accountId || t.destinationAccountId == accountId) &&
            matchesQuery(item)
    }

    private fun matchesQuery(item: TransactionItem): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return true
        return listOfNotNull(item.transaction.description, item.transaction.notes, item.category?.name)
            .any { it.contains(q, ignoreCase = true) }
    }
}
