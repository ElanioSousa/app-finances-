package com.example.app_finances.domain

import com.example.app_finances.data.local.entity.GoalEntity
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/** Como a meta está e quanto guardar por mês para chegar no prazo. */
sealed interface GoalPlan {
    val fraction: Float

    data class Reached(override val fraction: Float) : GoalPlan

    /** Sem prazo: só o que falta. */
    data class Open(override val fraction: Float, val remainingCents: Long) : GoalPlan

    /** Guardar [monthlyCents] por mês, do mês atual até o do prazo. */
    data class OnTrack(override val fraction: Float, val remainingCents: Long, val monthlyCents: Long) : GoalPlan

    /** O prazo passou e ainda falta [remainingCents]. */
    data class Late(override val fraction: Float, val remainingCents: Long) : GoalPlan

    companion object {
        fun of(goal: GoalEntity, today: LocalDate): GoalPlan {
            val fraction = if (goal.targetCents <= 0) 1f else (goal.savedCents.toFloat() / goal.targetCents).coerceIn(0f, 1f)
            val remaining = (goal.targetCents - goal.savedCents).coerceAtLeast(0)
            val deadline = goal.deadline
            return when {
                remaining == 0L -> Reached(fraction)
                deadline == null -> Open(fraction, remaining)
                deadline < today -> Late(fraction, remaining)
                else -> OnTrack(fraction, remaining, ceilDiv(remaining, monthsUntil(deadline, today)))
            }
        }

        /** Meses em que ainda dá para guardar, contando o atual: de nov a jun = 8. */
        private fun monthsUntil(deadline: LocalDate, today: LocalDate): Long =
            ChronoUnit.MONTHS.between(YearMonth.from(today), YearMonth.from(deadline)) + 1

        private fun ceilDiv(a: Long, b: Long): Long = (a + b - 1) / b
    }
}
