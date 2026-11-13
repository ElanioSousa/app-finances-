package com.example.app_finances

import android.content.Intent
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
    fun launchWithFreshDatabase(intent: Intent? = null): ActivityScenario<MainActivity> {
        context.container = AppContainer(
            context = context,
            createDatabase = { Room.inMemoryDatabaseBuilder(it, AppDatabase::class.java).addCallback(SeedCallback).build() },
        )
        return if (intent == null) ActivityScenario.launch(MainActivity::class.java) else ActivityScenario.launch(intent)
    }

    /** Como o atalho "Lançamento por voz" do ícone abre o app. */
    fun voiceShortcutIntent(): Intent = Intent(context, MainActivity::class.java).setAction(MainActivity.ACTION_VOICE_ENTRY)

    /** Como o WhatsApp faz ao "Compartilhar" um texto com o app. */
    fun shareIntent(text: String): Intent = Intent(context, MainActivity::class.java)
        .setAction(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_TEXT, text)
}
