package com.example.app_finances.domain

import com.example.app_finances.data.local.entity.TransactionEntity
import com.example.app_finances.data.local.entity.TransactionType
import java.time.LocalDate
import java.time.YearMonth

/** Período de um relatório: um mês ou um ano inteiro. */
enum class ReportPeriod { MONTH, YEAR }

/** Mês/ano de referência + período, com as datas que ele cobre. */
data class ReportRange(val period: ReportPeriod, val anchor: YearMonth) {
    private val months: Long get() = if (period == ReportPeriod.MONTH) 1 else 12
    private val firstMonth: YearMonth get() = if (period == ReportPeriod.MONTH) anchor else anchor.withMonth(1)

    val start: LocalDate get() = firstMonth.atDay(1)
    val end: LocalDate get() = firstMonth.plusMonths(months - 1).atEndOfMonth()

    fun previous() = copy(anchor = anchor.minusMonths(months))
    fun next() = copy(anchor = anchor.plusMonths(months))
}

/** Movimento de uma conta ou de um cartão no período (transferências não entram). */
data class OriginTotal(
    val accountId: Long?,
    val cardId: Long?,
    val incomeCents: Long,
    val expenseCents: Long,
)

data class Report(
    val totals: MonthTotals,
    val expensesByCategory: List<CategorySpend>,
    val incomesByCategory: List<CategorySpend>,
    val byOrigin: List<OriginTotal>,
)

/** Relatórios por categoria e por conta/cartão, por competência (pagos ou não). */
object Reports {

    fun build(transactions: List<TransactionEntity>): Report = Report(
        totals = MonthTotals.of(transactions),
        expensesByCategory = Insights.totalsByCategory(transactions, TransactionType.EXPENSE),
        incomesByCategory = Insights.totalsByCategory(transactions, TransactionType.INCOME),
        byOrigin = byOrigin(transactions),
    )

    private fun byOrigin(transactions: List<TransactionEntity>): List<OriginTotal> =
        transactions
            .filter { it.type != TransactionType.TRANSFER }
            .groupBy { it.accountId to it.cardId }
            .map { (origin, list) ->
                val (incomes, expenses) = list.partition { it.type == TransactionType.INCOME }
                OriginTotal(origin.first, origin.second, incomes.sumOf { it.amountCents }, expenses.sumOf { it.amountCents })
            }
            .sortedByDescending { it.expenseCents + it.incomeCents }
}
