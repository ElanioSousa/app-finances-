package com.example.app_finances.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.app_finances.data.local.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Atualizar o app não pode perder dados: cada versão do banco migra para a seguinte. */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test
    fun v1ParaV2MantemRecorrenciasESemDataDeGeracao() {
        helper.createDatabase(DB, 1).use { db ->
            db.execSQL("INSERT INTO accounts (id, name, type, initialBalanceCents, colorArgb, archived) VALUES (1, 'Carteira', 'CASH', 0, 0, 0)")
            db.execSQL(
                "INSERT INTO recurrings (id, description, amountCents, type, accountId, frequency, startDate, isSubscription, active) " +
                    "VALUES (1, 'Aluguel', 150000, 'EXPENSE', 1, 'MONTHLY', 20000, 0, 1)",
            )
        }
        helper.runMigrationsAndValidate(DB, 2, true).use { db ->
            db.query("SELECT description, generatedUntil FROM recurrings").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Aluguel", cursor.getString(0))
                assertTrue(cursor.isNull(1))
            }
        }
    }

    private companion object {
        const val DB = "migracao-teste.db"
    }
}
