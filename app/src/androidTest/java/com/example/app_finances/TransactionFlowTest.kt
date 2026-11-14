package com.example.app_finances

import androidx.compose.ui.test.assertIsDisplayed
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
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.app_finances.core.format.Money
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import com.example.app_finances.feature.dashboard.TAG_BALANCE
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Fluxo real: criar despesa pelo "+", ver saldo no Início, excluir e desfazer. */
@RunWith(AndroidJUnit4::class)
class TransactionFlowTest {

    @get:Rule
    val compose = createEmptyComposeRule()
    private lateinit var scenario: ActivityScenario<MainActivity>
    private fun string(id: Int) = TestApp.string(id)

    @Before
    fun start() {
        scenario = TestApp.launchWithFreshDatabase()
    }

    @After
    fun stop() = scenario.close()

    @Test
    fun criaDespesaAtualizaSaldoExcluiEDesfaz() {
        // Duas seguidas: o formulário precisa abrir limpo depois de um salvamento.
        addExpense(digits = "12345", category = "Alimentação")
        addExpense(digits = "5000", category = "Compras")
        compose.onNodeWithTag(TAG_BALANCE).assertTextEquals(Money.format(-17_345))

        compose.onAllNodesWithText(string(R.string.nav_transactions)).onFirst().performClick()
        compose.onAllNodesWithText("Alimentação · Carteira").onFirst().performTouchInput { swipeLeft() }
        compose.onNodeWithText(string(R.string.action_undo)).performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Alimentação · Carteira").fetchSemanticsNodes().isNotEmpty() }
        // E continua lá: o item restaurado não pode ser excluído de novo sozinho.
        Thread.sleep(1_000)
        compose.onAllNodesWithText("Alimentação · Carteira").onFirst().assertIsDisplayed()
    }

    @Test
    fun valorZeroMostraErroESegueAberto() {
        openForm()
        compose.onNodeWithText(string(R.string.action_save)).performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("O valor deve ser maior que zero.", substring = true).fetchSemanticsNodes().isNotEmpty() }
    }

    /** Abre o formulário e espera ele terminar de carregar (Salvar habilitado). */
    private fun openForm() {
        compose.onNodeWithContentDescription(string(R.string.action_new_transaction)).performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodes(hasText(string(R.string.action_save)) and isEnabled()).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun addExpense(digits: String, category: String) {
        openForm()
        compose.onNode(hasSetTextAction() and hasText(string(R.string.form_amount))).performTextInput(digits)
        compose.onNodeWithText(category).performClick()
        compose.onNodeWithText(string(R.string.action_save)).performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText(string(R.string.form_new_title)).fetchSemanticsNodes().isEmpty() }
    }
}
