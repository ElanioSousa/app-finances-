package com.example.app_finances.autobackup

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/** Agenda (ou cancela) o backup automático semanal. O primeiro roda logo depois de ligar. */
object AutoBackup {
    private const val WORK_NAME = "auto_backup"

    /** Idempotente: chamar de novo não cria um segundo agendamento. */
    private fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(7, TimeUnit.DAYS)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /** Agenda se [enabled], senão cancela — o único ponto que liga ou desliga o backup automático. */
    fun apply(context: Context, enabled: Boolean) {
        if (enabled) schedule(context) else WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
