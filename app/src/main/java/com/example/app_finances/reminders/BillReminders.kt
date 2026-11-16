package com.example.app_finances.reminders

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.app_finances.R
import com.example.app_finances.core.work.delayUntilNext
import com.example.app_finances.core.notifications.createNotificationChannel
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/** Agenda (ou cancela) o lembrete diário de contas a vencer, por volta das 9h. */
object BillReminders {
    const val CHANNEL_ID = "bill_reminders"
    private const val WORK_NAME = "bill_reminders"
    private val REMIND_AT: LocalTime = LocalTime.of(9, 0)

    /** Idempotente: chamar de novo não cria um segundo agendamento. */
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<BillReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delayUntilNext(REMIND_AT, LocalDateTime.now()).toMinutes(), TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    /** Agenda se [enabled], senão cancela — o único ponto que liga ou desliga o lembrete. */
    fun apply(context: Context, enabled: Boolean) = if (enabled) schedule(context) else cancel(context)

    /** Canal de notificação (Android 8+); criar de novo não faz nada. */
    fun createChannel(context: Context) {
        context.createNotificationChannel(CHANNEL_ID, R.string.reminder_channel)
    }
}
