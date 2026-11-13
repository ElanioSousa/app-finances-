package com.example.app_finances

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.app_finances.core.format.Money
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Lançamento a partir de texto: pelo campo rápido e pelo "Compartilhar" de outro app. */
@RunWith(AndroidJUnit4::class)
class QuickEntryFlowTest {

    @get:Rule
    val compose = createEmptyComposeRule()
    private var scenario: ActivityScenario<MainActivity>? = null
    private fun string(id: Int) = TestApp.string(id)
    private fun waitFor(text: String) = compose.waitUntil(5_000) { compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty() }
    private fun waitUntilReady() = compose.waitUntil(5_000) { compose.onAllNodes(hasText(string(R.string.action_save)) and isEnabled()).fetchSemanticsNodes().isNotEmpty() }

    @After
    fun stop() {
        scenario?.close()
    }

    @Test
    fun campoRapidoPreencheOFormulario() {
        scenario = TestApp.launchWithFreshDatabase()
        compose.onNodeWithContentDescription(string(R.string.action_new_transaction)).performClick()
        waitUntilReady()
        compose.onNode(hasSetTextAction() and hasText(string(R.string.form_quick))).performTextInput("uber 23,50")
        compose.onNodeWithContentDescription(string(R.string.form_quick_apply)).performClick()
        waitFor(Money.format(2_350))
        compose.onNodeWithText(string(R.string.action_save)).performScrollTo().performClick()

        compose.onAllNodesWithText(string(R.string.nav_transactions)).onFirst().performClick()
        waitFor("Uber")
        compose.onAllNodesWithText("Transporte", substring = true).onFirst().assertExists()
    }

    @Test
    fun textoCompartilhadoAbreOFormularioPreenchido() {
        scenario = TestApp.launchWithFreshDatabase(TestApp.shareIntent("mercado 45,90"))
        waitFor(string(R.string.form_new_title))
        waitUntilReady()
        waitFor(Money.format(4_590))
        compose.onNodeWithText(string(R.string.action_save)).performScrollTo().performClick()

        compose.onAllNodesWithText(string(R.string.nav_transactions)).onFirst().performClick()
        waitFor("Mercado")
    }
}
