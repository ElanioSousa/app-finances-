package com.example.app_finances.domain

import com.example.app_finances.data.local.entity.CreditCardEntity
import com.example.app_finances.data.local.entity.TransactionEntity
import com.example.app_finances.data.local.entity.TransactionType
import java.time.LocalDate
import java.time.YearMonth

/** Gasto de uma categoria no período. [categoryId] nulo = sem categoria; [isOther] = soma das menores. */
data class CategorySpend(
    val categoryId: Long?,
    val cents: Long,
    val share: Float,
    val isOther: Boolean = false,
)

/** Receitas e despesas de um mês, para o histórico. */
data class MonthBar(val month: YearMonth, val totals: MonthTotals)

/** Algo a pagar/receber em breve: um lançamento pendente ou uma fatura de cartão. */
sealed interface UpcomingBill {
    val date: LocalDate
    val amountCents: Long

    data class Pending(val transaction: TransactionEntity) : UpcomingBill {
        override val date: LocalDate get() = transaction.date
        override val amountCents: Long get() = transaction.amountCents
    }

    data class CardInvoice(val card: CreditCardEntity, val invoice: Invoice) : UpcomingBill {
        override val date: LocalDate get() = invoice.dueDate
        override val amountCents: Long get() = invoice.remainingCents
    }

    fun isOverdue(today: LocalDate): Boolean = date < today
}

/** Números do Início, calculados a partir dos lançamentos (regras puras, testáveis). */
object Insights {
    /** Janela de "contas a vencer" (as atrasadas sempre entram). */
    const val UPCOMING_DAYS = 7

    /** Quantos meses o histórico mostra, terminando no mês atual. */
    const val HISTORY_MONTHS = 6

    /** Despesas por categoria, maiores primeiro; além de [maxRows], o resto vira "Outras". */
    fun spendingByCategory(transactions: List<TransactionEntity>, maxRows: Int = 5): List<CategorySpend> =
        totalsByCategory(transactions, TransactionType.EXPENSE, maxRows)

    /** Totais de [type] por categoria, maiores primeiro; além de [maxRows], o resto vira "Outras". */
    fun totalsByCategory(transactions: List<TransactionEntity>, type: TransactionType, maxRows: Int = Int.MAX_VALUE): List<CategorySpend> {
        val ofType = transactions.filter { it.type == type }
        val total = ofType.sumOf { it.amountCents }
        if (total == 0L) return emptyList()
        val ranked = ofType.groupBy { it.categoryId }
            .map { (id, list) -> id to list.sumOf { it.amountCents } }
            .sortedByDescending { it.second }
        val shown = if (ranked.size > maxRows) ranked.take(maxRows - 1) else ranked
        val otherCents = total - shown.sumOf { it.second }
        return shown.map { (id, cents) -> CategorySpend(id, cents, cents.toFloat() / total) } +
            listOfNotNull(CategorySpend(null, otherCents, otherCents.toFloat() / total, isOther = true).takeIf { otherCents > 0 })
    }

    /**
     * Receitas × despesas de cada um dos [count] meses que terminam em [last].
     * [monthOf] diz em que mês cada lançamento conta (ver [LedgerMonths]).
     */
    fun monthlyHistory(
        transactions: List<TransactionEntity>,
        last: YearMonth,
        monthOf: (TransactionEntity) -> YearMonth,
        count: Int = HISTORY_MONTHS,
    ): List<MonthBar> {
        val byMonth = transactions.groupBy(monthOf)
        return (count - 1 downTo 0).map { back ->
            val month = last.minusMonths(back.toLong())
            MonthBar(month, MonthTotals.of(byMonth[month].orEmpty()))
        }
    }

    /**
     * Contas do lembrete diário: o que há para **pagar** (despesas pendentes e faturas)
     * que já venceu ou vence até amanhã. Receitas a receber não entram.
     */
    fun billsDueByTomorrow(bills: List<UpcomingBill>, today: LocalDate): List<UpcomingBill> =
        bills.filter { bill ->
            val isIncome = bill is UpcomingBill.Pending && bill.transaction.type == TransactionType.INCOME
            !isIncome && bill.date <= today.plusDays(1)
        }

    /** Último dia da janela de contas a vencer. */
    fun upcomingLimit(today: LocalDate): LocalDate = today.plusDays(UPCOMING_DAYS.toLong())

    /**
     * Pendências até [UPCOMING_DAYS] dias à frente (as atrasadas também) e faturas de cartão
     * a pagar que vencem nesse prazo ou já venceram. Mais antigas primeiro.
     * (Filtra de novo o que o banco já filtra: a regra vale mesmo sem a consulta.)
     */
    fun upcomingBills(pending: List<TransactionEntity>, cards: List<CardSummary>, today: LocalDate): List<UpcomingBill> {
        val limit = upcomingLimit(today)
        val transactions = pending.filter { !it.paid && it.date <= limit }.map { UpcomingBill.Pending(it) }
        val invoices = cards.flatMap { summary ->
            listOfNotNull(summary.pending, summary.current)
                .filter { it.remainingCents > 0 && it.closingDate <= today && it.dueDate <= limit }
                .map { UpcomingBill.CardInvoice(summary.card, it) }
        }
        return (transactions + invoices).sortedBy { it.date }
    }
}
