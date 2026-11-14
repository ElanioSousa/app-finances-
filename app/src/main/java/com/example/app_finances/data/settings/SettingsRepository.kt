package com.example.app_finances.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** Preferências do app (não são dados financeiros: ficam fora do banco e do backup). */
data class AppSettings(
    /** Mostra "R$ ••••" no lugar dos valores (para usar o app em público). */
    val hideValues: Boolean = false,
    /** Notificação diária de contas que vencem hoje ou amanhã. */
    val billReminders: Boolean = false,
    /** Pede biometria (ou o bloqueio de tela) ao abrir o app. */
    val appLock: Boolean = false,
    /** Escuro (padrão), claro ou o mesmo do celular. */
    val themeMode: ThemeMode = ThemeMode.DARK,
    /** Backup semanal numa pasta escolhida; nulo = desligado. */
    val autoBackup: AutoBackupSetting? = null,
)

enum class ThemeMode {
    DARK, LIGHT, SYSTEM;

    /** Se o app fica escuro, dado o modo do celular ([systemDark]) para o "Automático". */
    fun isDark(systemDark: Boolean): Boolean = when (this) {
        DARK -> true
        LIGHT -> false
        SYSTEM -> systemDark
    }
}

/** Pasta do backup automático (com permissão guardada) e quando ele rodou pela última vez. */
data class AutoBackupSetting(
    val folderUri: String,
    val folderName: String,
    val lastDone: LocalDate? = null,
)

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(context: Context) {
    private val store = context.applicationContext.settingsStore

    val settings: Flow<AppSettings> = store.data.map { prefs ->
        AppSettings(
            hideValues = prefs[HIDE_VALUES] ?: false,
            billReminders = prefs[BILL_REMINDERS] ?: false,
            appLock = prefs[APP_LOCK] ?: false,
            themeMode = prefs[THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: if (prefs[DARK_THEME] == false) ThemeMode.LIGHT else ThemeMode.DARK,
            autoBackup = prefs[AUTO_BACKUP_FOLDER]?.let { folder ->
                AutoBackupSetting(folder, prefs[AUTO_BACKUP_FOLDER_NAME].orEmpty(), prefs[AUTO_BACKUP_LAST]?.let(LocalDate::parse))
            },
        )
    }

    suspend fun setHideValues(value: Boolean) = set(HIDE_VALUES, value)

    suspend fun setBillReminders(value: Boolean) = set(BILL_REMINDERS, value)

    suspend fun setAppLock(value: Boolean) = set(APP_LOCK, value)

    suspend fun setThemeMode(mode: ThemeMode) {
        store.edit {
            it[THEME_MODE] = mode.name
            it.remove(DARK_THEME)
        }
    }

    /** Liga o backup automático nesta pasta (ainda sem nenhum feito). */
    suspend fun enableAutoBackup(folderUri: String, folderName: String) {
        store.edit { prefs ->
            prefs[AUTO_BACKUP_FOLDER] = folderUri
            prefs[AUTO_BACKUP_FOLDER_NAME] = folderName
            prefs.remove(AUTO_BACKUP_LAST)
        }
    }

    /** Desliga o backup automático e devolve a pasta que ele usava (para liberar o acesso). */
    suspend fun disableAutoBackup(): String? {
        var folder: String? = null
        store.edit { prefs ->
            folder = prefs[AUTO_BACKUP_FOLDER]
            listOf(AUTO_BACKUP_FOLDER, AUTO_BACKUP_FOLDER_NAME, AUTO_BACKUP_LAST).forEach { prefs.remove(it) }
        }
        return folder
    }

    suspend fun setAutoBackupDone(date: LocalDate) {
        store.edit { it[AUTO_BACKUP_LAST] = date.toString() }
    }

    private suspend fun set(key: Preferences.Key<Boolean>, value: Boolean) {
        store.edit { it[key] = value }
    }

    private companion object {
        val HIDE_VALUES = booleanPreferencesKey("hide_values")
        val BILL_REMINDERS = booleanPreferencesKey("bill_reminders")
        val APP_LOCK = booleanPreferencesKey("app_lock")
        /** Preferência antiga (só escuro/claro), lida enquanto o tema novo não for escolhido. */
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val AUTO_BACKUP_FOLDER = stringPreferencesKey("auto_backup_folder")
        val AUTO_BACKUP_FOLDER_NAME = stringPreferencesKey("auto_backup_folder_name")
        val AUTO_BACKUP_LAST = stringPreferencesKey("auto_backup_last")
    }
}
