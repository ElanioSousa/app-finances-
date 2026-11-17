package com.example.app_finances

import android.app.NotificationManager
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.core.content.getSystemService
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.app_finances.core.format.Money
import com.example.app_finances.domain.BankNotification
import com.example.app_finances.purchases.PurchaseSuggestions
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Uma compra do Nubank vira uma notificação do app que, tocada, abre o lançamento preenchido. */
@RunWith(AndroidJUnit4::class)
class PurchaseSuggestionTest {

    @get:Rule
    val compose = createEmptyComposeRule()
    private var scenario: ActivityScenario<MainActivity>? = null
    private val notifications get() = TestApp.context.getSystemService<NotificationManager>()!!

    @Before
    fun allowNotifications() {
        TestApp.grantNotifications()
    }

    @After
    fun stop() {
        scenario?.close()
        notifications.cancelAll()
    }

    @Test
    fun sugestaoAbreOFormularioPreenchido() {
        scenario = TestApp.launchWithFreshDatabase()
        val notice = BankNotification.parse(
            BankNotification.NUBANK,
            "Compra no crédito aprovada",
            "Compra de R$ 27,00 APROVADA em LIFESTYLEACAI para o cartão com final 1157.",
        )!!
        PurchaseSuggestions.suggest(TestApp.context, notice, notice.quickText(emptyList()))

        val posted = notifications.activeNotifications.firstOrNull { it.id == notice.key.hashCode() }
        assertNotNull("a sugestão deveria aparecer", posted)
        posted!!.notification.contentIntent.send()

        compose.waitUntil(10_000) { compose.onAllNodesWithText(Money.format(2_700)).fetchSemanticsNodes().isNotEmpty() }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Lifestyleacai", substring = true).fetchSemanticsNodes().isNotEmpty() }
    }
}
