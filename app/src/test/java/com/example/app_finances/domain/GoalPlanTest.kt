package com.example.app_finances.domain

import com.example.app_finances.data.local.entity.GoalEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class GoalPlanTest {

    private val today = LocalDate.of(2026, 11, 14)
    private fun goal(saved: Long, deadline: LocalDate? = null) =
        GoalEntity(name = "Reserva", targetCents = 500_000, savedCents = saved, deadline = deadline, colorArgb = 0)

    @Test
    fun divideOQueFaltaPelosMesesAteOPrazoContandoOAtual() {
        // De novembro a junho são 8 meses para guardar R$ 4.000: R$ 500 por mês.
        assertEquals(GoalPlan.OnTrack(0.2f, 400_000, 50_000), GoalPlan.of(goal(100_000, LocalDate.of(2027, 6, 30)), today))
    }

    @Test
    fun arredondaParaCimaParaNaoFicarFaltandoCentavos() {
        val plan = GoalPlan.of(goal(0, LocalDate.of(2027, 1, 10)), today) as GoalPlan.OnTrack
        assertEquals(166_667, plan.monthlyCents)
    }

    @Test
    fun semPrazoAlcancadaEAtrasada() {
        assertEquals(GoalPlan.Open(0.5f, 250_000), GoalPlan.of(goal(250_000), today))
        assertEquals(GoalPlan.Reached(1f), GoalPlan.of(goal(600_000), today))
        assertEquals(GoalPlan.Late(0.5f, 250_000), GoalPlan.of(goal(250_000, LocalDate.of(2026, 10, 31)), today))
    }
}
