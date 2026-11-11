package com.example.app_finances.feature.transactions

import com.example.app_finances.data.local.entity.CreditCardEntity
import com.example.app_finances.data.local.entity.InvoicePaymentEntity
import com.example.app_finances.core.model.TransactionItem
import com.example.app_finances.data.local.entity.TransactionType
import java.time.LocalDate

/** Lançamentos de um mesmo dia. */
data class DayGroup(
    val date: LocalDate,
    val items: List<TransactionItem>,
    val payments: List<PaymentItem> = emptyList(),
)

/**
 * Pagamento de fatura: o dinheiro sai da conta, mas não é uma despesa nova
 * (as compras do cartão já contaram). Aparece na lista e não entra nos totais.
 */
data class PaymentItem(val payment: InvoicePaymentEntity, val card: CreditCardEntity?, val accountName: String)

/** Agrupa por dia, mais recentes primeiro; no mesmo dia, lançamentos antes dos pagamentos. */
fun groupByDay(items: List<TransactionItem>, payments: List<PaymentItem> = emptyList()): List<DayGroup> {
    val itemsByDay = items.groupBy { it.transaction.date }
    val paymentsByDay = payments.groupBy { it.payment.date }
    return (itemsByDay.keys + paymentsByDay.keys).sortedDescending().map { date ->
        DayGroup(date, itemsByDay[date].orEmpty(), paymentsByDay[date].orEmpty())
    }
}

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

    /** Pagamentos só aparecem em "Todos" (não são receita nem despesa) e sem filtro de categoria. */
    fun matches(item: PaymentItem): Boolean {
        val q = query.trim()
        return type == null && categoryId == null &&
            (accountId == null || item.payment.accountId == accountId) &&
            (q.isEmpty() || listOfNotNull(item.card?.name, PAYMENT_KEYWORD).any { it.contains(q, ignoreCase = true) })
    }

    private fun matchesQuery(item: TransactionItem): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return true
        return listOfNotNull(item.transaction.description, item.transaction.notes, item.category?.name)
            .any { it.contains(q, ignoreCase = true) }
    }
}

/** Buscar por "fatura" encontra os pagamentos. */
private const val PAYMENT_KEYWORD = "Fatura"
