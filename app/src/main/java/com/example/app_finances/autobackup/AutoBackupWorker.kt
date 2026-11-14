package com.example.app_finances.autobackup

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.app_finances.appContainer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

/** Uma vez por semana: salva um backup completo na pasta escolhida e anota a data. */
class AutoBackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        backUp()
        Result.success()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // Pasta sem acesso ou falha pontual: tenta de novo algumas vezes; a tela mostra a data do último que deu certo.
        Log.e(TAG, "Falha no backup automático", e)
        if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
    }

    private suspend fun backUp() {
        val container = applicationContext.appContainer
        val setting = container.settingsRepository.settings.first().autoBackup ?: return
        val today = container.today()
        container.backupRepository.exportToFolder(setting.folderUri.toUri(), today)
        container.settingsRepository.setAutoBackupDone(today)
    }

    private companion object {
        const val TAG = "AutoBackupWorker"
        const val MAX_RETRIES = 3
    }
}
