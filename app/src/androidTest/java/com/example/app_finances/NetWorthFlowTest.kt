package com.example.app_finances

import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** A tela de patrimônio abre pela tela "Mais" e mostra o que tem e o que deve. */
@RunWith(AndroidJUnit4::class)
class NetWorthFlowTest {

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
    fun abrePelaTelaMais() {
        compose.onAllNodesWithText(string(R.string.nav_more)).onFirst().performClick()
        compose.onNodeWithText(string(R.string.more_networth)).performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText(string(R.string.networth_net)).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(string(R.string.networth_assets)).assertExists()
        compose.onNodeWithText(string(R.string.networth_debts)).assertExists()
    }
}
