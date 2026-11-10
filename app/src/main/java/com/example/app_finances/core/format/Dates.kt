package com.example.app_finances.core.format

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Datas no padrão brasileiro, com "Hoje"/"Ontem" quando ajuda a leitura. */
object Dates {
    private val ptBR: Locale = Locale.forLanguageTag("pt-BR")
    private val dayHeader = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", ptBR)
    private val shortDate = DateTimeFormatter.ofPattern("dd/MM/yyyy", ptBR)
    private val dayMonth = DateTimeFormatter.ofPattern("dd/MM", ptBR)
    private val monthLabel = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", ptBR)

    fun dayHeader(date: LocalDate, today: LocalDate = LocalDate.now()): String =
        relative(date, today) ?: date.format(dayHeader).capitalized()

    fun short(date: LocalDate, today: LocalDate = LocalDate.now()): String =
        relative(date, today) ?: date.format(shortDate)

    /** Datas próximas (dias/semanas): "Hoje", "Ontem" ou "12/11", sem o ano. */
    fun dayMonth(date: LocalDate, today: LocalDate = LocalDate.now()): String =
        relative(date, today) ?: date.format(dayMonth)

    /** "Hoje"/"Ontem", ou nulo para as demais datas. */
    fun relative(date: LocalDate, today: LocalDate = LocalDate.now()): String? = when (date) {
        today -> "Hoje"
        today.minusDays(1) -> "Ontem"
        else -> null
    }

    fun month(month: YearMonth): String = month.format(monthLabel).capitalized()

    /** "nov", "dez" — para eixos de gráfico. */
    fun shortMonth(month: YearMonth): String = month.month.getDisplayName(TextStyle.SHORT, ptBR).trimEnd('.')

    /** O DatePicker do Material trabalha com milissegundos em UTC. */
    fun toPickerMillis(date: LocalDate): Long = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    fun fromPickerMillis(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

    private fun String.capitalized() = replaceFirstChar { it.titlecase(ptBR) }
}
