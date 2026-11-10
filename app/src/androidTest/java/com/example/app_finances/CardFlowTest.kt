package com.example.app_finances

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.app_finances.core.format.Money
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Cadastra cartão, compra parcelada, paga a fatura e confere o saldo da conta. */
@RunWith(AndroidJUnit4::class)
class CardFlowTest {

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
    fun compraParceladaNoCartaoEPagamentoDaFatura() {
        createCard("Nubank")
        buyInInstallments(digits = "30000", installmentsLabel = string(R.string.form_installment_option).format(3, Money.format(10_000)))

        // Compra no cartão não mexe no saldo da conta; só a parcela do mês entra nas despesas.
        compose.onAllNodesWithText(string(R.string.nav_home)).onFirst().performClick()
        waitFor(Money.format(10_000))
        assertTrue(compose.onAllNodesWithText(Money.format(-10_000)).fetchSemanticsNodes().isEmpty())

        payCurrentInvoice("Nubank")
        compose.onAllNodesWithText(string(R.string.nav_home)).onFirst().performClick()
        waitFor(Money.format(-10_000))
    }

    private fun createCard(name: String) {
        compose.onAllNodesWithText(string(R.string.nav_cards)).onFirst().performClick()
        compose.onNodeWithText(string(R.string.cards_new)).performClick()
        compose.onNode(hasSetTextAction() and hasText(string(R.string.card_name))).performTextInput(name)
        compose.onNode(hasSetTextAction() and hasText(string(R.string.card_limit))).performTextInput("500000")
        compose.onNodeWithText(string(R.string.action_save)).performClick()
        waitFor(name)
    }

    private fun buyInInstallments(digits: String, installmentsLabel: String) {
        compose.onAllNodesWithText(string(R.string.nav_home)).onFirst().performClick()
        compose.onNodeWithContentDescription(string(R.string.action_new_transaction)).performClick()
        compose.waitUntil(5_000) { compose.onAllNodes(hasText(string(R.string.action_save)) and isEnabled()).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasSetTextAction() and hasText(string(R.string.form_amount))).performTextInput(digits)
        compose.onNodeWithText("Alimentação").performClick()
        compose.onNodeWithText(string(R.string.form_origin_card)).performScrollTo().performClick()
        compose.onNode(hasText(string(R.string.form_installments))).performScrollTo().performClick()
        compose.onNodeWithText(installmentsLabel).performClick()
        compose.onNodeWithText(string(R.string.action_save)).performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText(string(R.string.form_new_title)).fetchSemanticsNodes().isEmpty() }
    }

    private fun payCurrentInvoice(cardName: String) {
        compose.onAllNodesWithText(string(R.string.nav_cards)).onFirst().performClick()
        compose.onNodeWithText(cardName).performClick()
        compose.onNodeWithText(string(R.string.card_pay_invoice)).performScrollTo().performClick()
        compose.onAllNodesWithText(string(R.string.card_pay_invoice)).onLast().performClick()
        waitFor(string(R.string.card_payments))
    }
}
