package com.example.app_finances.domain

import com.example.app_finances.data.local.entity.GoalEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class SpendingRoomTest {

    private val today = LocalDate.of(2026, 11, 14) // faltam 17 dias contando hoje

    @Test
    fun divideOQueSobraPelosDiasDepoisDeReservarAsMetas() {
        // Meta de R$ 4.000 até junho: R$ 500 por mês.
        val trip = GoalEntity(name = "Viagem", targetCents = 500_000, savedCents = 100_000, deadline = LocalDate.of(2027, 6, 30), colorArgb = 0)
        val room = SpendingRoom.of(endOfMonthCents = 220_000, goals = listOf(trip), today = today)
        assertEquals(SpendingRoom(totalCents = 170_000, perDayCents = 10_000, reservedForGoalsCents = 50_000), room)
    }

    @Test
    fun semFolgaNaoTemValorPorDia() {
        assertEquals(SpendingRoom(-3_000, 0, 0), SpendingRoom.of(-3_000, emptyList(), today))
    }
}
