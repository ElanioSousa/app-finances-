package com.example.app_finances.reminders

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.app_finances.FinancesApplication
import com.example.app_finances.MainActivity
import com.example.app_finances.R
import com.example.app_finances.core.format.Dates
import com.example.app_finances.core.format.Money
import com.example.app_finances.domain.CardSummary
import com.example.app_finances.domain.Insights
import com.example.app_finances.domain.UpcomingBill
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/** Uma vez por dia: avisa o que vence hoje ou amanhã (e o que já venceu). */
class BillReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as FinancesApplication).container
        val today = container.today()
        val pending = container.transactionRepository.observePendingUntil(today.plusDays(1)).first()
        val cards = container.cardRepository.observeCardData().first()
        val summaries = CardSummary.ofAll(cards.cards, cards.purchases, cards.payments, today)
        val due = Insights.billsDueByTomorrow(Insights.upcomingBills(pending, summaries, today), today)
        if (due.isNotEmpty() && canNotify()) notify(due, today)
        return Result.success()
    }

    private fun canNotify(): Boolean =
        ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    @Suppress("MissingPermission") // conferida em canNotify()
    private fun notify(due: List<UpcomingBill>, today: LocalDate) {
        val title = if (due.size == 1) applicationContext.getString(R.string.reminder_title_one)
        else applicationContext.getString(R.string.reminder_title, due.size)
        val lines = due.map { line(it, today) }
        val open = PendingIntent.getActivity(
            applicationContext, 0,
            Intent(applicationContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        // Na tela de bloqueio aparece só o título, sem valores.
        val public = NotificationCompat.Builder(applicationContext, BillReminders.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .build()
        val notification = NotificationCompat.Builder(applicationContext, BillReminders.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(public)
            .setContentTitle(title)
            .setContentText(lines.first())
            .setStyle(NotificationCompat.InboxStyle().also { style -> lines.forEach(style::addLine) })
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
    }

    /** "Aluguel · R$ 1.800,00 · 12/11". */
    private fun line(bill: UpcomingBill, today: LocalDate): String {
        val name = when (bill) {
            is UpcomingBill.Pending -> bill.transaction.description
            is UpcomingBill.CardInvoice -> applicationContext.getString(R.string.home_card_invoice, bill.card.name)
        }
        val label = if (bill.isOverdue(today)) applicationContext.getString(R.string.reminder_overdue, name) else name
        return "$label · ${Money.format(bill.amountCents)} · ${Dates.dayMonth(bill.date, today)}"
    }

    private companion object {
        const val NOTIFICATION_ID = 1001
    }
}
