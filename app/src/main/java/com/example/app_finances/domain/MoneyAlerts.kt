package com.example.app_finances.domain

import com.example.app_finances.data.local.entity.TransactionEntity
import com.example.app_finances.data.local.entity.TransactionType
import java.time.YearMonth
import kotlin.math.abs

/** Como uma categoria de despesa mudou de um mês para o outro ([percent] negativo = gastou menos). */
data class CategoryChange(val categoryId: Long, val percent: Int, val diffCents: Long)

/** Resumo de um mês fechado, comparado com o anterior. */
data class MonthSummary(val month: YearMonth, val totals: MonthTotals, val change: CategoryChange?)

object MonthSummaries {
    /** Só aponta uma categoria se ela mudou pelo menos isso (e tinha gasto no mês anterior). */
    private const val MIN_CHANGE_PERCENT = 10

    /** Nulo se o mês não teve nenhum lançamento. A categoria é a que mais mudou em reais. */
    fun of(month: YearMonth, current: List<TransactionEntity>, previous: List<TransactionEntity>): MonthSummary? {
        val totals = MonthTotals.of(current)
        if (totals.incomeCents == 0L && totals.expenseCents == 0L) return null
        val before = expenseByCategory(previous)
        val now = expenseByCategory(current)
        val change = before.filterValues { it > 0 }.map { (id, old) ->
            val diff = (now[id] ?: 0) - old
            CategoryChange(id, (diff * 100 / old).toInt(), diff)
        }.filter { abs(it.percent) >= MIN_CHANGE_PERCENT }.maxByOrNull { abs(it.diffCents) }
        return MonthSummary(month, totals, change)
    }

    private fun expenseByCategory(list: List<TransactionEntity>): Map<Long, Long> =
        Insights.totalsByCategory(list, TransactionType.EXPENSE).mapNotNull { spend -> spend.categoryId?.let { it to spend.cents } }.toMap()
}

/** Aviso que vale uma vez: [key] diz se já foi enviado (o mês vem primeiro, para limpar os antigos). */
sealed interface MoneyAlert {
    val key: String

    /** Orçamento passou de 80% ([BudgetLevel.WARNING]) ou estourou ([BudgetLevel.OVER]). */
    data class Budget(val status: BudgetStatus, val month: YearMonth) : MoneyAlert {
        override val key: String get() = "$month|budget|${status.category.id}|${status.level}"
    }

    /** Cartão com 90% ou mais do limite em uso (uma vez por fatura). */
    data class CardLimit(val summary: CardSummary) : MoneyAlert {
        override val key: String get() = "${summary.current.month}|card|${summary.card.id}"
    }

    companion object {
        /** O que ainda não foi avisado, entre orçamentos do [month] e cartões. */
        fun due(budgets: List<BudgetStatus>, cards: List<CardSummary>, month: YearMonth, sent: Set<String>): List<MoneyAlert> {
            val budgetAlerts = budgets.filter { it.level != BudgetLevel.OK }.map { Budget(it, month) }
            val cardAlerts = cards.filter { it.isNearLimit }.map(::CardLimit)
            return (budgetAlerts + cardAlerts).filter { it.key !in sent }
        }

        /** Os avisos a lembrar depois de enviar [newKeys]: os de [keepFrom] em diante (os antigos não voltam a valer). */
        fun remember(sent: Set<String>, newKeys: Collection<String>, keepFrom: YearMonth): Set<String> =
            (sent + newKeys).filter { key -> monthOf(key)?.let { it >= keepFrom } ?: false }.toSet()

        private fun monthOf(key: String): YearMonth? = runCatching { YearMonth.parse(key.substringBefore('|')) }.getOrNull()
    }
}
