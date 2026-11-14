package com.example.app_finances.domain

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/** O dia de hoje ([today]) e, com o app aberto, de novo a cada meia-noite. */
fun currentDay(today: () -> LocalDate): Flow<LocalDate> = flow {
    while (true) {
        emit(today())
        val now = LocalDateTime.now()
        delay(Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay()).toMillis() + 1_000)
    }
}.distinctUntilChanged()
