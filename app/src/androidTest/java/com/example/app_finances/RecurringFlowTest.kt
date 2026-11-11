package com.example.app_finances

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.app_finances.core.format.Money
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Cadastra uma assinatura e confere que ela vira um lançamento pendente do mês. */
@RunWith(AndroidJUnit4::class)
class RecurringFlowTest {

    @get:Rule
    val compose = createEmptyComposeRule()
    private lateinit var scenario: ActivityScenario<MainActivity>

    private fun string(id: Int) = TestApp.string(id)
    private fun waitFor(text: String) = compose.waitUntil(5_000) { compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty() }

    @Before
    fun start() {
        scenario = TestApp.launchWithFreshDatabase()
    }

    @After
    fun stop() = scenario.close()

    @Test
    fun assinaturaViraLancamentoPendente() {
        compose.onAllNodesWithText(string(R.string.nav_more)).onFirst().performClick()
        compose.onNodeWithText(string(R.string.more_recurring)).performClick()
        compose.onNodeWithText(string(R.string.recurring_new)).performClick()
        compose.onNode(hasSetTextAction() and hasText(string(R.string.form_amount))).performTextInput("3990")
        compose.onNode(hasSetTextAction() and hasText(string(R.string.form_description))).performTextInput("Streaming")
        compose.onNodeWithText(string(R.string.recurring_is_subscription)).performClick()
        compose.onNode(hasText(string(R.string.recurring_is_subscription))).assertExists()
        compose.onNodeWithText(string(R.string.action_save)).performClick()

        waitFor(string(R.string.recurring_subscriptions_monthly))
        compose.onAllNodesWithText(Money.format(3_990)).onFirst().assertExists()

        compose.onAllNodesWithText(string(R.string.nav_transactions)).onFirst().performClick()
        waitFor("Streaming")
        compose.onNodeWithText(string(R.string.transaction_pending)).assertExists()
    }

    @Test
    fun emprestimoComPrazoNumeraAsParcelas() {
        compose.onAllNodesWithText(string(R.string.nav_more)).onFirst().performClick()
        compose.onNodeWithText(string(R.string.more_recurring)).performClick()
        compose.onNodeWithText(string(R.string.recurring_new)).performClick()
        compose.onNode(hasSetTextAction() and hasText(string(R.string.form_amount))).performTextInput("50000")
        compose.onNode(hasSetTextAction() and hasText(string(R.string.form_description))).performTextInput("Empréstimo")
        compose.onNode(hasText(string(R.string.recurring_term))).performScrollTo().performClick()
        compose.onNodeWithText(string(R.string.recurring_term_installments)).performClick()
        compose.onNode(hasSetTextAction() and hasText(string(R.string.recurring_occurrences))).performScrollTo().performTextInput("12")
        compose.onNodeWithText(string(R.string.action_save)).performClick()

        waitFor(string(R.string.recurring_term_count).format(12))
        compose.onAllNodesWithText(string(R.string.nav_transactions)).onFirst().performClick()
        waitFor("Empréstimo (1/12)")
    }
}
