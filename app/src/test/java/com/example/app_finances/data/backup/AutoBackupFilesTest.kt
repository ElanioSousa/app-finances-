package com.example.app_finances.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AutoBackupFilesTest {

    private fun day(d: Int) = LocalDate.of(2026, 11, d)

    @Test
    fun nomeTemADataParaOrdenar() {
        assertEquals("financas-backup-auto-2026-11-07.json", AutoBackupFiles.nameFor(day(7)))
    }

    @Test
    fun ficamSoOsQuatroMaisRecentesENaoMexeEmOutrosArquivos() {
        val auto = (1..6).map { AutoBackupFiles.nameFor(day(it)) }
        val others = listOf("financas-backup-2026-11-01.json", "fotos.zip")
        val deleted = AutoBackupFiles.toDelete((others + auto).shuffled())
        assertEquals(listOf(AutoBackupFiles.nameFor(day(2)), AutoBackupFiles.nameFor(day(1))), deleted)
    }

    @Test
    fun avisaSoDepoisDeDuasSemanasSemBackup() {
        assertFalse(AutoBackupFiles.isLate(day(1), day(15)))
        assertTrue(AutoBackupFiles.isLate(day(1), day(16)))
    }
}
