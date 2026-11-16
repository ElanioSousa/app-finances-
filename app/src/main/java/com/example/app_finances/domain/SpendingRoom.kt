package com.example.app_finances.domain

import com.example.app_finances.data.local.entity.GoalEntity
import java.time.LocalDate

/**
 * Quanto ainda dá para gastar até o fim do mês: o saldo previsto para o último dia, menos o
 * que as metas pedem por mês ([reservedForGoalsCents]), dividido pelos dias que faltam (com hoje).
 */
data class SpendingRoom(val totalCents: Long, val perDayCents: Long, val reservedForGoalsCents: Long) {
    companion object {
        fun of(endOfMonthCents: Long, goals: List<GoalEntity>, today: LocalDate): SpendingRoom {
            val reserved = goals.map { GoalPlan.of(it, today) }.filterIsInstance<GoalPlan.OnTrack>().sumOf { it.monthlyCents }
            val total = endOfMonthCents - reserved
            val daysLeft = today.lengthOfMonth() - today.dayOfMonth + 1
            return SpendingRoom(total, if (total > 0) total / daysLeft else 0, reserved)
        }
    }
}
