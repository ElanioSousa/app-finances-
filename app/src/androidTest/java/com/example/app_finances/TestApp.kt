package com.example.app_finances

import android.app.Activity
import android.content.Intent
import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.app_finances.data.local.AppDatabase
import com.example.app_finances.data.local.SeedCallback
import com.example.app_finances.di.AppContainer
import com.example.app_finances.premium.PremiumOffer
import com.example.app_finances.premium.PremiumStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking

/** Acesso ao app nos testes de tela. */
object TestApp {
    val context: FinancesApplication get() = ApplicationProvider.getApplicationContext()

    fun string(id: Int): String = context.getString(id)

    /** Abre o app com um banco novo em memória (já com Carteira e categorias), isolando cada teste. */
    fun launchWithFreshDatabase(intent: Intent? = null, premium: Boolean = true): ActivityScenario<MainActivity> {
        context.container = AppContainer(
            context = context,
            createDatabase = { Room.inMemoryDatabaseBuilder(it, AppDatabase::class.java).addCallback(SeedCallback).build() },
            createPremiumStore = { _, _ -> NoStorePremium },
        )
        // O Premium fica num arquivo de verdade: cada teste diz como quer começar.
        runBlocking { context.container.settingsRepository.setPremium(premium) }
        return if (intent == null) ActivityScenario.launch(MainActivity::class.java) else ActivityScenario.launch(intent)
    }

    /** Dá a permissão de notificação e só volta quando o Android terminar (senão a 1ª notificação pode se perder). */
    fun grantNotifications() {
        val command = "pm grant ${context.packageName} android.permission.POST_NOTIFICATIONS"
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
            .let { ParcelFileDescriptor.AutoCloseInputStream(it) }.use { it.readBytes() }
    }

    /** Loja de mentira: os testes não falam com a Play Store (e ela não muda o Premium). */
    private object NoStorePremium : PremiumStore {
        override val offer: StateFlow<PremiumOffer> = MutableStateFlow(PremiumOffer.Unavailable)
        override suspend fun refresh() = Unit
        override fun buy(activity: Activity): Boolean = false
    }

    /** Como o atalho "Lançamento por voz" do ícone abre o app. */
    fun voiceShortcutIntent(): Intent = Intent(context, MainActivity::class.java).setAction(MainActivity.ACTION_VOICE_ENTRY)

    /** Como o WhatsApp faz ao "Compartilhar" um texto com o app. */
    fun shareIntent(text: String): Intent = Intent(context, MainActivity::class.java)
        .setAction(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_TEXT, text)
}
