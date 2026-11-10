package com.example.app_finances.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Preferências do app (não são dados financeiros: ficam fora do banco e do backup). */
data class AppSettings(
    /** Mostra "R$ ••••" no lugar dos valores (para usar o app em público). */
    val hideValues: Boolean = false,
    /** Notificação diária de contas que vencem hoje ou amanhã. */
    val billReminders: Boolean = false,
    /** Pede biometria (ou o bloqueio de tela) ao abrir o app. */
    val appLock: Boolean = false,
)

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(context: Context) {
    private val store = context.applicationContext.settingsStore

    val settings: Flow<AppSettings> = store.data.map { prefs ->
        AppSettings(
            hideValues = prefs[HIDE_VALUES] ?: false,
            billReminders = prefs[BILL_REMINDERS] ?: false,
            appLock = prefs[APP_LOCK] ?: false,
        )
    }

    suspend fun setHideValues(value: Boolean) = set(HIDE_VALUES, value)

    suspend fun setBillReminders(value: Boolean) = set(BILL_REMINDERS, value)

    suspend fun setAppLock(value: Boolean) = set(APP_LOCK, value)

    private suspend fun set(key: Preferences.Key<Boolean>, value: Boolean) {
        store.edit { it[key] = value }
    }

    private companion object {
        val HIDE_VALUES = booleanPreferencesKey("hide_values")
        val BILL_REMINDERS = booleanPreferencesKey("bill_reminders")
        val APP_LOCK = booleanPreferencesKey("app_lock")
    }
}
