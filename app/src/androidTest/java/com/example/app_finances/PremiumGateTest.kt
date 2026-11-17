package com.example.app_finances

import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Sem o Premium, as metas mostram o convite e levam à tela do Premium (que mostra a compra). */
@RunWith(AndroidJUnit4::class)
class PremiumGateTest {

    @get:Rule
    val compose = createEmptyComposeRule()
    private var scenario: ActivityScenario<MainActivity>? = null
    private fun string(id: Int) = TestApp.string(id)
    private fun waitFor(text: String) = compose.waitUntil(5_000) { compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty() }

    @After
    fun stop() {
        scenario?.close()
    }

    @Test
    fun metasSemPremiumLevamAoPremium() {
        scenario = TestApp.launchWithFreshDatabase(premium = false)
        compose.onAllNodesWithText(string(R.string.nav_more)).onFirst().performClick()
        compose.onNodeWithText(string(R.string.more_goals)).performClick()
        waitFor(string(R.string.premium_locked_title).format(string(R.string.goals_title)))
        compose.onNodeWithText(string(R.string.goals_new)).assertDoesNotExist()

        compose.onNodeWithText(string(R.string.premium_see)).performClick()
        waitFor(string(R.string.premium_includes))
        // Fora da Play Store a compra não existe: o app diz isso em vez de um botão que não funciona.
        waitFor(string(R.string.premium_unavailable))
    }
}
