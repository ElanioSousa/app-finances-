package com.example.app_finances

import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.app_finances.core.format.Money
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Cria uma meta sem prazo, guarda um valor e vê o progresso. */
@RunWith(AndroidJUnit4::class)
class GoalsFlowTest {

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
    fun guardarNaMetaMostraOProgresso() {
        compose.onAllNodesWithText(string(R.string.nav_more)).onFirst().performClick()
        compose.onNodeWithText(string(R.string.more_goals)).performClick()
        waitFor(string(R.string.goals_empty_title))
        compose.onNodeWithText(string(R.string.goals_new)).performClick()
        compose.onNode(hasSetTextAction() and hasText(string(R.string.goals_name))).performTextInput("Viagem")
        compose.onNode(hasSetTextAction() and hasText(string(R.string.goals_target))).performTextInput("500000")
        compose.onNodeWithText(string(R.string.action_save)).performClick()
        waitFor("Viagem")

        compose.onAllNodesWithText(string(R.string.goals_deposit)).onFirst().performClick()
        compose.onNode(hasSetTextAction() and hasText(string(R.string.form_amount))).performTextInput("125000")
        // O botão "Guardar" do diálogo (o do cartão da meta tem o mesmo texto).
        compose.onNode(hasText(string(R.string.goals_deposit)) and hasAnyAncestor(isDialog())).performClick()

        waitFor(string(R.string.goals_progress).format(Money.format(125_000), Money.format(500_000)))
        compose.onAllNodesWithText(string(R.string.goals_remaining).format(Money.format(375_000))).onFirst().assertExists()
    }
}
