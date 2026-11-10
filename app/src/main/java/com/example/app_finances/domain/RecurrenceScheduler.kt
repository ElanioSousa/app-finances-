package com.example.app_finances.domain

import com.example.app_finances.data.local.entity.Frequency
import com.example.app_finances.data.local.entity.RecurringEntity
import com.example.app_finances.data.local.entity.TransactionEntity
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * Datas e lançamentos de recorrências (salário, aluguel, assinaturas).
 * Cada ocorrência é contada a partir do início — assim um dia 31 volta a ser 31
 * depois de fevereiro (31/01 → 28/02 → 31/03).
 */
object RecurrenceScheduler {

    /** Até onde gerar: o fim do mês atual, para as contas do mês já aparecerem como pendentes. */
    fun horizon(today: LocalDate): LocalDate = YearMonth.from(today).atEndOfMonth()

    /** Ocorrências entre [from] e [until] (inclusive), respeitando início e fim da recorrência. */
    fun occurrences(recurring: RecurringEntity, from: LocalDate, until: LocalDate): List<LocalDate> =
        recurring.datesFrom(from).takeWhile { it <= until }.toList()

    /** Próxima ocorrência a partir de [onOrAfter], ou nula se a recorrência já terminou. */
    fun nextOccurrence(recurring: RecurringEntity, onOrAfter: LocalDate): LocalDate? =
        recurring.datesFrom(onOrAfter).firstOrNull()

    /**
     * De onde começar a gerar ao **criar**: uma recorrência que começou no passado
     * não lança meses antigos — começa no mês atual. Nulo = desde o início.
     */
    fun initialCursor(startDate: LocalDate, today: LocalDate): LocalDate? {
        val monthStart = today.withDayOfMonth(1)
        return monthStart.minusDays(1).takeIf { startDate < monthStart }
    }

    /** De onde voltar a gerar depois de **editar ou retomar**: a partir de hoje (inclusive). */
    fun restartCursor(startDate: LocalDate, today: LocalDate): LocalDate? {
        val yesterday = today.minusDays(1)
        return yesterday.takeIf { startDate <= yesterday }
    }

    /**
     * Lançamentos que faltam gerar até [horizon] — só depois de `generatedUntil`,
     * para não recriar o que o usuário já apagou.
     */
    fun pending(recurring: RecurringEntity, horizon: LocalDate): List<TransactionEntity> {
        if (!recurring.active) return emptyList()
        val from = recurring.generatedUntil?.plusDays(1) ?: recurring.startDate
        return occurrences(recurring, from, horizon).map { toTransaction(recurring, it) }
    }

    /**
     * O lançamento de uma ocorrência. Na conta nasce pendente (o usuário confirma
     * quando pagar/receber); no cartão já conta como compra da fatura.
     * (`RecurringDao.deleteFutureOccurrences` depende desta regra para saber o que ainda não aconteceu.)
     */
    fun toTransaction(recurring: RecurringEntity, date: LocalDate) = TransactionEntity(
        type = recurring.type,
        amountCents = recurring.amountCents,
        date = date,
        description = recurring.description,
        categoryId = recurring.categoryId,
        accountId = recurring.accountId,
        cardId = recurring.cardId,
        paid = recurring.cardId != null,
        recurringId = recurring.id,
    )

    /** Custo mensal equivalente: semanal × 52 / 12, anual / 12. */
    fun monthlyEquivalent(recurring: RecurringEntity): Long = when (recurring.frequency) {
        Frequency.WEEKLY -> recurring.amountCents * WEEKS_PER_YEAR / MONTHS_PER_YEAR
        Frequency.MONTHLY -> recurring.amountCents
        Frequency.YEARLY -> recurring.amountCents / MONTHS_PER_YEAR
    }

    /**
     * Datas a partir de [from] (inclusive) até o fim da recorrência. Pula direto para
     * perto de [from] em vez de andar desde o início (uma semanal de anos atrás seria lenta);
     * o passo estimado fica um antes para cobrir o ajuste de fim de mês.
     */
    private fun RecurringEntity.datesFrom(from: LocalDate): Sequence<LocalDate> {
        val firstStep = (stepsBetween(startDate, from) - 1).coerceAtLeast(0)
        return generateSequence(firstStep) { it + 1 }
            .map { dateAt(it) }
            .takeWhile { endDate == null || it <= endDate }
            .filter { it >= from }
    }

    private fun RecurringEntity.stepsBetween(start: LocalDate, end: LocalDate): Long = when (frequency) {
        Frequency.WEEKLY -> ChronoUnit.WEEKS.between(start, end)
        Frequency.MONTHLY -> ChronoUnit.MONTHS.between(start, end)
        Frequency.YEARLY -> ChronoUnit.YEARS.between(start, end)
    }

    private fun RecurringEntity.dateAt(step: Long): LocalDate = when (frequency) {
        Frequency.WEEKLY -> startDate.plusWeeks(step)
        Frequency.MONTHLY -> startDate.plusMonths(step)
        Frequency.YEARLY -> startDate.plusYears(step)
    }

    private const val WEEKS_PER_YEAR = 52L
    private const val MONTHS_PER_YEAR = 12L
}
