package com.example.app_finances

import android.app.NotificationManager
import androidx.core.content.getSystemService
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.example.app_finances.alerts.MonthAlertsWorker
import com.example.app_finances.data.local.entity.TransactionEntity
import com.example.app_finances.data.settings.AlertsState
import com.example.app_finances.data.local.entity.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** Gasto que passa de 80% do orçamento vira um alerta (pela tarefa de verdade, com o banco do app). */
@RunWith(AndroidJUnit4::class)
class MonthAlertsTest {

    private var scenario: ActivityScenario<MainActivity>? = null
    private val container get() = TestApp.context.container
    private val notifications get() = TestApp.context.getSystemService<NotificationManager>()!!

    @Before
    fun start() {
        TestApp.grantNotifications()
        scenario = TestApp.launchWithFreshDatabase()
        forgetAlerts()
    }

    @After
    fun stop() {
        scenario?.close()
        runBlocking { container.settingsRepository.setMonthAlerts(false) }
        forgetAlerts()
        notifications.cancelAll()
    }

    /** Os avisos enviados ficam num arquivo de verdade: cada teste começa sem nenhum. */
    private fun forgetAlerts() = runBlocking { container.settingsRepository.setAlertsState(AlertsState()) }

    @Test
    fun orcamentoAcimaDe80PorCentoAvisa() = runBlocking {
        val food = container.categoryRepository.observeAll().first().first { it.name == "Alimentação" }
        container.categoryRepository.update(food.copy(monthlyBudgetCents = 10_000))
        container.transactionRepository.add(
            TransactionEntity(type = TransactionType.EXPENSE, amountCents = 8_500, date = LocalDate.now(), description = "Mercado", categoryId = food.id, accountId = 1),
        )
        container.settingsRepository.setMonthAlerts(true)

        val result = TestListenableWorkerBuilder<MonthAlertsWorker>(TestApp.context).build().doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        val title = TestApp.context.getString(R.string.alerts_budget_warning_title, "Alimentação", 85)
        val titles = notifications.activeNotifications.map { it.notification.extras.getCharSequence("android.title")?.toString() }
        assertTrue("alerta esperado em $titles", title in titles)
        assertTrue(container.settingsRepository.alertsState.first().sent.any { it.contains("budget|${food.id}|WARNING") })
    }
}
