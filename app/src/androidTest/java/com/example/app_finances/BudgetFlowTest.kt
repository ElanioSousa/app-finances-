package com.example.app_finances

import androidx.compose.ui.test.hasClickAction
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
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Categoria nova com orçamento: gasto de 90% acende o alerta no Início e aparece nos relatórios. */
@RunWith(AndroidJUnit4::class)
class BudgetFlowTest {

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
    fun orcamentoAlertaEmOitentaPorCentoEApareceNosRelatorios() {
        createCategoryWithBudget(name = "Academia", budgetDigits = "10000")
        addExpense(digits = "9000", category = "Academia")

        compose.onAllNodesWithText(string(R.string.nav_home)).onFirst().performClick()
        waitFor(string(R.string.home_budgets))
        compose.onNodeWithText(TestApp.context.getString(R.string.budget_warning, 90)).performScrollTo()

        // A aba Mais volta para Categorias (cada aba guarda sua pilha); tocar de novo volta ao início dela.
        compose.onAllNodesWithText(string(R.string.nav_more)).onFirst().performClick()
        compose.onAllNodesWithText(string(R.string.nav_more)).onFirst().performClick()
        compose.onNodeWithText(string(R.string.more_reports)).performClick()
        waitFor(string(R.string.reports_expenses_by_category))
        compose.onAllNodesWithText("Academia").onFirst().assertExists()
        compose.onNodeWithText(string(R.string.reports_year)).performClick()
        waitFor(string(R.string.reports_by_origin))
    }

    private fun createCategoryWithBudget(name: String, budgetDigits: String) {
        compose.onAllNodesWithText(string(R.string.nav_more)).onFirst().performClick()
        compose.onNodeWithText(string(R.string.more_categories)).performClick()
        compose.onNodeWithText(string(R.string.categories_new)).performScrollTo().performClick()
        compose.onNode(hasSetTextAction() and hasText(string(R.string.category_name))).performTextInput(name)
        compose.onNodeWithContentDescription("fitness").performClick()
        compose.onNode(hasSetTextAction() and hasText(string(R.string.category_budget))).performScrollTo().performTextInput(budgetDigits)
        compose.onNodeWithText(string(R.string.action_save)).performClick()
        waitFor(TestApp.context.getString(R.string.budget_ok, 0))
    }

    private fun addExpense(digits: String, category: String) {
        compose.onAllNodesWithText(string(R.string.nav_home)).onFirst().performClick()
        waitFor(string(R.string.home_total_balance))
        compose.onNodeWithContentDescription(string(R.string.action_new_transaction)).performClick()
        compose.waitUntil(5_000) { compose.onAllNodes(hasText(string(R.string.action_save)) and isEnabled()).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasSetTextAction() and hasText(string(R.string.form_amount))).performTextInput(digits)
        compose.onNode(hasText(category) and hasClickAction()).performClick()
        compose.onNodeWithText(string(R.string.action_save)).performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText(string(R.string.form_new_title)).fetchSemanticsNodes().isEmpty() }
    }
}
