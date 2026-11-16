package com.example.app_finances.core.work

import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

/** Tempo até o próximo [time] (hoje, se ainda não passou; senão amanhã). Para tarefas diárias. */
fun delayUntilNext(time: LocalTime, now: LocalDateTime): Duration {
    val todayAt = now.toLocalDate().atTime(time)
    val next = if (todayAt.isAfter(now)) todayAt else todayAt.plusDays(1)
    return Duration.between(now, next)
}
