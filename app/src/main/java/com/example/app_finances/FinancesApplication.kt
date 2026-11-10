package com.example.app_finances

import android.app.Application
import com.example.app_finances.core.security.AppLock
import com.example.app_finances.di.AppContainer
import com.example.app_finances.reminders.BillReminders

class FinancesApplication : Application() {
    /** Trocável só nos testes, para cada teste começar com um banco limpo. */
    lateinit var container: AppContainer
        internal set

    /** Estado do bloqueio por biometria, do processo inteiro (sobrevive a girar a tela). */
    val appLock = AppLock()

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        BillReminders.createChannel(this)
        appLock.watchAppBackground(this)
    }
}
