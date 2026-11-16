package com.example.app_finances.alerts

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.app_finances.R
import com.example.app_finances.core.notifications.createNotificationChannel
import com.example.app_finances.core.work.delayUntilNext
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * Agenda o resumo do mês e os alertas: uma vez por dia (por volta das 9h) e, com o app
 * aberto, logo depois de sair dele (o alerta de orçamento chega junto com o gasto).
 */
object MonthAlerts {
    const val CHANNEL_ID = "month_alerts"
    private const val DAILY_WORK = "month_alerts_daily"
    private const val NOW_WORK = "month_alerts_now"
    private val CHECK_AT: LocalTime = LocalTime.of(9, 0)

    fun createChannel(context: Context) = context.createNotificationChannel(CHANNEL_ID, R.string.alerts_channel)

    /** Agenda se [enabled], senão cancela — o único ponto que liga ou desliga os alertas. */
    fun apply(context: Context, enabled: Boolean) {
        val work = WorkManager.getInstance(context)
        if (!enabled) {
            work.cancelUniqueWork(DAILY_WORK)
            return
        }
        val request = PeriodicWorkRequestBuilder<MonthAlertsWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delayUntilNext(CHECK_AT, LocalDateTime.now()).toMinutes(), TimeUnit.MINUTES)
            .build()
        work.enqueueUniquePeriodicWork(DAILY_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
        checkNow(context)
    }

    /** Confere agora (ex.: ao sair do app). Se os alertas estiverem desligados, a tarefa não faz nada. */
    fun checkNow(context: Context) {
        WorkManager.getInstance(context)
            .enqueueUniqueWork(NOW_WORK, ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<MonthAlertsWorker>().build())
    }
}
