package com.example.app_finances.feature.settings

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_finances.core.ui.WhileUiSubscribed
import com.example.app_finances.core.ui.runForUserMessage
import com.example.app_finances.data.backup.BackupRepository
import com.example.app_finances.data.settings.AppSettings
import com.example.app_finances.data.settings.SettingsRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Operação com arquivo concluída (a tela escolhe o texto). */
enum class FileTask { BACKUP, RESTORE, CSV }

sealed interface SettingsEvent {
    data class Done(val task: FileTask) : SettingsEvent
    data class Failed(val message: String) : SettingsEvent
}

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    /** Arquivo escolhido para restaurar, aguardando a confirmação. */
    val pendingRestore: Uri? = null,
    val isWorking: Boolean = false,
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val backup: BackupRepository,
) : ViewModel() {

    private val pendingRestore = MutableStateFlow<Uri?>(null)
    private val working = MutableStateFlow(false)
    private val events = Channel<SettingsEvent>(Channel.BUFFERED)

    val eventFlow: Flow<SettingsEvent> = events.receiveAsFlow()

    val state: StateFlow<SettingsUiState> = combine(settingsRepository.settings, pendingRestore, working, ::SettingsUiState)
        .stateIn(viewModelScope, WhileUiSubscribed, SettingsUiState())

    fun setHideValues(value: Boolean) = launchSetting { settingsRepository.setHideValues(value) }

    fun setAppLock(value: Boolean) = launchSetting { settingsRepository.setAppLock(value) }

    fun setDarkTheme(value: Boolean) = launchSetting { settingsRepository.setDarkTheme(value) }

    /** Liga o backup automático na pasta escolhida (guardando o acesso a ela). */
    fun enableAutoBackup(folder: Uri) = launchSetting {
        settingsRepository.enableAutoBackup(folder.toString(), backup.keepFolderAccess(folder))
    }

    /** Desliga o backup automático e devolve o acesso à pasta (os arquivos já salvos ficam lá). */
    fun disableAutoBackup() = launchSetting {
        settingsRepository.disableAutoBackup()?.let { backup.releaseFolderAccess(it.toUri()) }
    }

    /** O agendamento acompanha a preferência sozinho (ver `FinancesApplication`). */
    fun setBillReminders(value: Boolean) = launchSetting { settingsRepository.setBillReminders(value) }

    /** Aviso pronto vindo da tela (ex.: aparelho sem bloqueio de tela). */
    fun warn(message: String) {
        events.trySend(SettingsEvent.Failed(message))
    }

    fun exportBackup(uri: Uri) = runFileTask(FileTask.BACKUP) { backup.exportBackup(uri) }

    fun exportCsv(uri: Uri) = runFileTask(FileTask.CSV) { backup.exportCsv(uri) }

    /** Restaurar apaga os dados atuais: primeiro pede confirmação. */
    fun askRestore(uri: Uri) = pendingRestore.update { uri }

    fun cancelRestore() = pendingRestore.update { null }

    fun confirmRestore() {
        val uri = pendingRestore.value ?: return
        pendingRestore.update { null }
        runFileTask(FileTask.RESTORE) { backup.restoreBackup(uri) }
    }

    private fun launchSetting(block: suspend () -> Unit) {
        viewModelScope.launch { runForUserMessage(block)?.let { events.send(SettingsEvent.Failed(it)) } }
    }

    private fun runFileTask(task: FileTask, block: suspend () -> Unit) {
        if (working.value) return
        working.value = true
        viewModelScope.launch {
            val error = runForUserMessage(block)
            working.value = false
            events.send(if (error == null) SettingsEvent.Done(task) else SettingsEvent.Failed(error))
        }
    }
}
