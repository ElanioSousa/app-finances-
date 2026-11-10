package com.example.app_finances.core.format

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class DatesTest {
    private val today = LocalDate.of(2026, 11, 10)

    @Test
    fun `hoje e ontem por extenso`() {
        assertEquals("Hoje", Dates.dayHeader(today, today))
        assertEquals("Ontem", Dates.dayHeader(today.minusDays(1), today))
        assertEquals("Domingo, 8 de novembro", Dates.dayHeader(LocalDate.of(2026, 11, 8), today))
        assertEquals("08/11/2026", Dates.short(LocalDate.of(2026, 11, 8), today))
    }

    @Test
    fun `mês com inicial maiúscula`() {
        assertEquals("Novembro de 2026", Dates.month(YearMonth.of(2026, 11)))
    }

    @Test
    fun `datas do calendário ida e volta sem mudar o dia`() {
        assertEquals(today, Dates.fromPickerMillis(Dates.toPickerMillis(today)))
    }
}
