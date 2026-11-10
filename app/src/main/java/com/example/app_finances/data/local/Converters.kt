package com.example.app_finances.data.local

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.YearMonth

/** Datas viram números/textos ordenáveis no SQLite. */
class Converters {
    @TypeConverter
    fun localDateToEpochDay(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun epochDayToLocalDate(epochDay: Long?): LocalDate? = epochDay?.let(LocalDate::ofEpochDay)

    /** "2026-11": ordena corretamente como texto. */
    @TypeConverter
    fun yearMonthToText(month: YearMonth?): String? = month?.toString()

    @TypeConverter
    fun textToYearMonth(text: String?): YearMonth? = text?.let(YearMonth::parse)
}
