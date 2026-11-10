package com.example.app_finances

import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.app_finances.data.local.AppDatabase
import com.example.app_finances.data.local.SeedCallback
import com.example.app_finances.di.AppContainer

/** Acesso ao app nos testes de tela. */
object TestApp {
    val context: FinancesApplication get() = ApplicationProvider.getApplicationContext()

    fun string(id: Int): String = context.getString(id)

    /** Abre o app com um banco novo em memória (já com Carteira e categorias), isolando cada teste. */
    fun launchWithFreshDatabase(): ActivityScenario<MainActivity> {
        context.container = AppContainer(
            context = context,
            createDatabase = { Room.inMemoryDatabaseBuilder(it, AppDatabase::class.java).addCallback(SeedCallback).build() },
        )
        return ActivityScenario.launch(MainActivity::class.java)
    }
}
