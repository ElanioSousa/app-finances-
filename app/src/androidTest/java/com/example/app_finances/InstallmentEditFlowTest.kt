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
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.YearMonth

/** Receita parcelada na conta: três parcelas, e mudar o valor da 2ª não mexe nas outras. */
@RunWith(AndroidJUnit4::class)
class InstallmentEditFlowTest {

    @get:Rule
    val compose = createEmptyComposeRule()
    private lateinit var scenario: ActivityScenario<MainActivity>
    private fun string(id: Int) = TestApp.string(id)
    private fun waitUntilReady() = compose.waitUntil(5_000) { compose.onAllNodes(hasText(string(R.string.action_save)) and isEnabled()).fetchSemanticsNodes().isNotEmpty() }
    private fun waitGone(text: String) = compose.waitUntil(5_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty() }

    @Before
    fun start() {
        scenario = TestApp.launchWithFreshDatabase()
    }

    @After
    fun stop() = scenario.close()

    @Test
    fun receitaParceladaNaContaComUmaParcelaAlterada() {
        compose.onNodeWithContentDescription(string(R.string.action_new_transaction)).performClick()
        waitUntilReady()
        compose.onNodeWithText(string(R.string.type_income)).performClick()
        compose.onNode(hasSetTextAction() and hasText(string(R.string.form_amount))).performTextInput("60000")
        compose.onNode(hasSetTextAction() and hasText(string(R.string.form_description))).performTextInput("Projeto")
        compose.onNode(hasText(string(R.string.form_payment))).performScrollTo().performClick()
        compose.onNodeWithText(string(R.string.form_installments)).performClick()
        compose.onNode(hasSetTextAction() and hasText(string(R.string.form_installments_count))).performScrollTo().performTextReplacement("3")
        compose.onNodeWithText(string(R.string.form_first_due_date)).assertExists()
        compose.onNodeWithText(string(R.string.action_save)).performScrollTo().performClick()
        waitGone(string(R.string.form_new_title))

        // A 2ª parcela vence no mês que vem: abre por Lançamentos e muda só o valor dela.
        compose.onAllNodesWithText(string(R.string.nav_transactions)).onFirst().performClick()
        compose.onNodeWithContentDescription(string(R.string.month_next)).performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Projeto", substring = true).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithText("Projeto", substring = true).onFirst().performClick()
        waitUntilReady()
        compose.onNodeWithText(string(R.string.form_installment_info).format(2, 3)).performScrollTo()
        compose.onNode(hasSetTextAction() and hasText(string(R.string.form_amount))).performTextReplacement("25000")
        compose.onNodeWithText(string(R.string.action_save)).performScrollTo().performClick()
        waitGone(string(R.string.form_edit_title))

        val now = YearMonth.now()
        val parcels = runBlocking { TestApp.context.container.transactionRepository.observeLedger(now, now.plusMonths(2)).first().transactions }
            .sortedBy { it.installmentNumber }
        assertEquals(listOf(20_000L, 25_000L, 20_000L), parcels.map { it.amountCents })
        assertEquals(listOf(true, false, false), parcels.map { it.paid })
    }
}
