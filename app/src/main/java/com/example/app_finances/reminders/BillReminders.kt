package com.example.app_finances.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.app_finances.R
import java.time.Duration
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

    /** O app pode mostrar notificações? (Android 13+ exige a permissão.) */
    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Canal de notificação (Android 8+); criar de novo não faz nada. */
    fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, context.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_DEFAULT)
        context.getSystemService<NotificationManager>()?.createNotificationChannel(channel)
    }

    /** Tempo até o próximo [time] (hoje, se ainda não passou; senão amanhã). */
    internal fun delayUntilNext(time: LocalTime, now: LocalDateTime): Duration {
        val todayAt = now.toLocalDate().atTime(time)
        val next = if (todayAt.isAfter(now)) todayAt else todayAt.plusDays(1)
        return Duration.between(now, next)
    }
}
