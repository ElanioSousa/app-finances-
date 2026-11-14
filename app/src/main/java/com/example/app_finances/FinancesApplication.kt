package com.example.app_finances

import android.app.Application
import android.content.Context
import android.util.Log
import com.example.app_finances.autobackup.AutoBackup
import com.example.app_finances.core.security.AppLock
import com.example.app_finances.data.settings.AppSettings
import com.example.app_finances.di.AppContainer
import com.example.app_finances.reminders.BillReminders
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

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
        keepScheduled({ it.billReminders }, BillReminders::apply)
        keepScheduled({ it.autoBackup != null }, AutoBackup::apply)
    }

    /** Uma tarefa agendada segue a preferência: ligar agenda, desligar cancela (de qualquer tela). */
    private fun keepScheduled(enabledIn: (AppSettings) -> Boolean, apply: (Context, Boolean) -> Unit) {
        MainScope().launch {
            container.settingsRepository.settings
                .map(enabledIn)
                .distinctUntilChanged()
                .catch { Log.e(TAG, "Falha ao ler as preferências", it) }
                .collect { enabled -> apply(this@FinancesApplication, enabled) }
        }
    }

    private companion object {
        const val TAG = "FinancesApplication"
    }
}
