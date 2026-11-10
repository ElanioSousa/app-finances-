package com.example.app_finances

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
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
import com.example.app_finances.domain.Insights
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Conta pendente aparece em "Próximos 7 dias" e some ao marcar como paga; o saldo acompanha. */
@RunWith(AndroidJUnit4::class)
class DashboardFlowTest {

    @get:Rule
    val compose = createEmptyComposeRule()
    private lateinit var scenario: ActivityScenario<MainActivity>

    private fun string(id: Int) = TestApp.string(id)
    private fun exists(text: String) = compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()

    @Before
    fun start() {
        scenario = TestApp.launchWithFreshDatabase()
    }

    @After
    fun stop() = scenario.close()

    @Test
    fun contaPendenteAparecePertoDoVencimentoESomeAoPagar() {
        addPendingExpense(digits = "11990", category = "Alimentação")
        compose.waitUntil(5_000) { exists(string(R.string.home_due_today)) }
        compose.onNodeWithText(string(R.string.home_spending)).performScrollTo()
        compose.onAllNodesWithText("Alimentação", substring = true).onFirst().assertExists()
        // Pendente ainda não sai do saldo.
        compose.onNodeWithText(string(R.string.home_total_balance)).performScrollTo()
        assertTrue(compose.onAllNodesWithText(Money.format(-11_990)).fetchSemanticsNodes().isEmpty())

        compose.onAllNodesWithContentDescription(string(R.string.home_mark_paid)).onFirst().performClick()
        compose.waitUntil(5_000) { exists(TestApp.context.getString(R.string.home_upcoming_empty, Insights.UPCOMING_DAYS)) }
        compose.waitUntil(5_000) { exists(Money.format(-11_990)) }
    }

    private fun addPendingExpense(digits: String, category: String) {
        compose.waitUntil(5_000) { exists(string(R.string.home_total_balance)) }
        compose.onNodeWithContentDescription(string(R.string.action_new_transaction)).performClick()
        compose.waitUntil(5_000) { compose.onAllNodes(hasText(string(R.string.action_save)) and isEnabled()).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasSetTextAction() and hasText(string(R.string.form_amount))).performTextInput(digits)
        compose.onNodeWithText(category).performScrollTo().performClick()
        compose.onNodeWithText(string(R.string.form_paid_expense)).performScrollTo().performClick()
        compose.onNodeWithText(string(R.string.action_save)).performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText(string(R.string.form_new_title)).fetchSemanticsNodes().isEmpty() }
    }
}
