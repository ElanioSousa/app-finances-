package com.example.app_finances.data.backup

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Regras dos arquivos do backup automático (puras, sem Android). */
object AutoBackupFiles {
    private const val PREFIX = "financas-backup-auto-"
    private const val EXTENSION = ".json"

    /** Quantos backups automáticos ficam na pasta (um mês, semanal). */
    const val KEEP = 4

    /** Sem backup novo há mais que isso, a tela avisa (a pasta pode ter sumido). */
    const val LATE_AFTER_DAYS = 14L

    fun nameFor(date: LocalDate): String = "$PREFIX$date$EXTENSION"

    /** Backups automáticos além dos [KEEP] mais recentes. A data no nome faz a ordem alfabética ser a do tempo. */
    fun toDelete(names: Collection<String>): List<String> =
        names.filter { it.startsWith(PREFIX) && it.endsWith(EXTENSION) }.sortedDescending().drop(KEEP)

    fun isLate(lastDone: LocalDate, today: LocalDate): Boolean = ChronoUnit.DAYS.between(lastDone, today) > LATE_AFTER_DAYS
}
