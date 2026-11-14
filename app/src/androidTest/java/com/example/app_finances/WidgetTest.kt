package com.example.app_finances

import android.content.Intent
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.app_finances.core.format.Money
import com.example.app_finances.widget.WidgetSummary
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** O widget mostra saldo e previsão, esconde os valores quando pedido e o "+" abre o formulário. */
@RunWith(AndroidJUnit4::class)
class WidgetTest {

    @get:Rule
    val compose = createEmptyComposeRule()
    private var scenario: ActivityScenario<MainActivity>? = null
    private val settings get() = TestApp.context.container.settingsRepository

    @After
    fun stop() {
        scenario?.close()
        runBlocking { settings.setHideValues(false) }
    }

    @Test
    fun mostraOsValoresEEscondeComOcultarValores() {
        scenario = TestApp.launchWithFreshDatabase()
        val container = TestApp.context.container
        assertEquals(WidgetSummary(Money.format(0), Money.format(0)), runBlocking { WidgetSummary.load(container) })

        runBlocking { settings.setHideValues(true) }
        assertEquals(WidgetSummary(Money.HIDDEN, Money.HIDDEN), runBlocking { WidgetSummary.load(container) })
    }

    @Test
    fun maisDoWidgetAbreONovoLancamento() {
        val intent = Intent(TestApp.context, MainActivity::class.java).setAction(MainActivity.ACTION_NEW_ENTRY)
        scenario = TestApp.launchWithFreshDatabase(intent)
        compose.waitUntil(5_000) { compose.onAllNodesWithText(TestApp.string(R.string.form_new_title)).fetchSemanticsNodes().isNotEmpty() }
    }
}
