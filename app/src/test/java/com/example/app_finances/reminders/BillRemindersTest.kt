package com.example.app_finances.reminders

import com.example.app_finances.data.local.entity.TransactionType
import com.example.app_finances.domain.Insights
import com.example.app_finances.domain.UpcomingBill
import com.example.app_finances.domain.transaction
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class BillRemindersTest {

    @Test
    fun `agenda para hoje às 9h ou, se já passou, para amanhã`() {
        val nine = LocalTime.of(9, 0)
        assertEquals(Duration.ofHours(2), BillReminders.delayUntilNext(nine, LocalDateTime.of(2026, 11, 10, 7, 0)))
        assertEquals(Duration.ofHours(23), BillReminders.delayUntilNext(nine, LocalDateTime.of(2026, 11, 10, 10, 0)))
        assertEquals(Duration.ofDays(1), BillReminders.delayUntilNext(nine, LocalDateTime.of(2026, 11, 10, 9, 0)))
    }

    @Test
    fun `lembrete só de contas a pagar até amanhã, inclusive atrasadas`() {
        val today = LocalDate.of(2026, 11, 10)
        fun pending(type: TransactionType, date: LocalDate) = UpcomingBill.Pending(transaction(type, date = date).copy(paid = false))
        val bills = listOf(
            pending(TransactionType.EXPENSE, today.minusDays(3)),
            pending(TransactionType.EXPENSE, today.plusDays(1)),
            pending(TransactionType.EXPENSE, today.plusDays(2)),
            pending(TransactionType.INCOME, today),
        )
        assertEquals(listOf(today.minusDays(3), today.plusDays(1)), Insights.billsDueByTomorrow(bills, today).map { it.date })
    }
}
