package com.example.app_finances.alerts

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.app_finances.R
import com.example.app_finances.appContainer
import com.example.app_finances.core.format.Dates
import com.example.app_finances.core.format.Money
import com.example.app_finances.core.notifications.AppNotification
import com.example.app_finances.core.notifications.postNotification
import com.example.app_finances.data.local.entity.CategoryEntity
import com.example.app_finances.data.settings.AlertsState
import com.example.app_finances.domain.BudgetLevel
import com.example.app_finances.domain.Budgets
import com.example.app_finances.domain.Ledger
import com.example.app_finances.domain.MoneyAlert
import com.example.app_finances.domain.MonthSummaries
import com.example.app_finances.domain.MonthSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.time.YearMonth
import kotlin.math.abs

/** Resumo do mês que acabou (uma vez por mês) e alertas novos de orçamento e de limite do cartão. */
class MonthAlertsWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val container get() = applicationContext.appContainer

    override suspend fun doWork(): Result = try {
        if (container.settingsRepository.settings.first().monthAlerts) check()
        Result.success()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "Falha ao montar o resumo e os alertas", e)
        if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
    }

    private suspend fun check() {
        val today = container.today()
        val month = YearMonth.from(today)
        val ledger = container.transactionRepository.observeLedger(month.minusMonths(2), month).first()
        val categories = container.categoryRepository.observeAll().first().associateBy { it.id }
        val state = container.settingsRepository.alertsState.first()
        val summaryDue = state.lastSummaryMonth != month
        if (summaryDue) {
            val previous = month.minusMonths(1)
            MonthSummaries.of(previous, ledger.of(previous), ledger.of(previous.minusMonths(1)))?.let { notifySummary(it, categories) }
        }
        val budgets = Budgets.statuses(categories.values.toList(), ledger.of(month))
        val cards = container.cardRepository.observeCardData().first().summaries(today)
        val alerts = MoneyAlert.due(budgets, cards, month, state.sent)
        alerts.forEach(::notifyAlert)
        if (summaryDue || alerts.isNotEmpty()) {
            val sent = MoneyAlert.remember(state.sent, alerts.map { it.key }, keepFrom = month.minusMonths(1))
            container.settingsRepository.setAlertsState(AlertsState(month, sent))
        }
    }

    private fun Ledger.of(month: YearMonth) = months.within(transactions, month, month)

    /** "Resumo de outubro: gastou R$ X e recebeu R$ Y. Alimentação: 18% a mais que em setembro." */
    private fun notifySummary(summary: MonthSummary, categories: Map<Long, CategoryEntity>) {
        val monthName = Dates.month(summary.month)
        val base = applicationContext.getString(R.string.alerts_summary_text, Money.format(summary.totals.expenseCents), Money.format(summary.totals.incomeCents))
        val change = summary.change?.let { change ->
            val name = categories[change.categoryId]?.name ?: return@let null
            val previous = Dates.month(summary.month.minusMonths(1))
            val res = if (change.percent > 0) R.string.alerts_summary_more else R.string.alerts_summary_less
            applicationContext.getString(res, name, abs(change.percent), previous)
        }
        val content = AppNotification(
            title = applicationContext.getString(R.string.alerts_summary_title, monthName),
            text = listOfNotNull(base, change).joinToString(" "),
            publicTitle = applicationContext.getString(R.string.alerts_summary_public),
        )
        applicationContext.postNotification(MonthAlerts.CHANNEL_ID, SUMMARY_ID, content)
    }

    private fun notifyAlert(alert: MoneyAlert) {
        val (title, text) = when (alert) {
            is MoneyAlert.Budget -> budgetTexts(alert)
            is MoneyAlert.CardLimit -> applicationContext.getString(R.string.alerts_card_title, alert.summary.card.name) to
                applicationContext.getString(R.string.alerts_card_text, (alert.summary.usedFraction * 100).toInt(), Money.format(alert.summary.availableCents))
        }
        val content = AppNotification(title, text, applicationContext.getString(R.string.alerts_public))
        applicationContext.postNotification(MonthAlerts.CHANNEL_ID, alert.key.hashCode(), content)
    }

    private fun budgetTexts(alert: MoneyAlert.Budget): Pair<String, String> {
        val status = alert.status
        val name = status.category.name
        return if (status.level == BudgetLevel.OVER) {
            applicationContext.getString(R.string.alerts_budget_over_title, name) to
                applicationContext.getString(R.string.alerts_budget_over_text, Money.format(-status.remainingCents), Money.format(status.budgetCents))
        } else {
            applicationContext.getString(R.string.alerts_budget_warning_title, name, (status.fraction * 100).toInt()) to
                applicationContext.getString(R.string.alerts_budget_warning_text, Money.format(status.remainingCents))
        }
    }

    private companion object {
        const val TAG = "MonthAlertsWorker"
        const val MAX_RETRIES = 3
        const val SUMMARY_ID = 3001
    }
}
