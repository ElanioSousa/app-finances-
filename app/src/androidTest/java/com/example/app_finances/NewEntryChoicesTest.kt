package com.example.app_finances

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import com.example.app_finances.core.designsystem.theme.SystemNightMode
import com.example.app_finances.data.settings.ThemeMode
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Novo lançamento com Despesa | Receita | Recorrência, transferência na tela "Mais" e o tema claro. */
@RunWith(AndroidJUnit4::class)
class NewEntryChoicesTest {

    @get:Rule
    val compose = createEmptyComposeRule()
    private lateinit var scenario: ActivityScenario<MainActivity>

    private fun string(id: Int) = TestApp.string(id)
    private fun waitFor(text: String) = compose.waitUntil(5_000) { compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty() }
    private fun openMore() = compose.onAllNodesWithText(string(R.string.nav_more)).onFirst().performClick()

    @Before
    fun start() {
        resetTheme()
        scenario = TestApp.launchWithFreshDatabase()
    }

    @After
    fun stop() {
        scenario.close()
        resetTheme()
    }

    /** A preferência fica num arquivo de verdade: começa e termina no padrão (escuro). */
    private fun resetTheme() {
        runBlocking { TestApp.context.container.settingsRepository.setThemeMode(ThemeMode.DARK) }
        SystemNightMode.apply(TestApp.context, ThemeMode.DARK)
    }

    @Test
    fun recorrenciaNoNovoLancamentoAbreOCadastroDela() {
        compose.onNodeWithContentDescription(string(R.string.action_new_transaction)).performClick()
        compose.onNodeWithText(string(R.string.type_transfer)).assertDoesNotExist()
        compose.onNodeWithText(string(R.string.type_recurring)).performClick()

        waitFor(string(R.string.recurring_new))
        compose.onNode(hasSetTextAction() and hasText(string(R.string.form_amount))).performTextInput("5590")
        compose.onNode(hasSetTextAction() and hasText(string(R.string.form_description))).performTextInput("Academia")
        compose.onNodeWithText(string(R.string.action_save)).performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText(string(R.string.recurring_new)).fetchSemanticsNodes().isEmpty() }

        openMore()
        compose.onNodeWithText(string(R.string.more_recurring)).performClick()
        waitFor("Academia")
    }

    @Test
    fun transferenciaAbrePelaTelaMais() {
        openMore()
        compose.onNodeWithText(string(R.string.more_transfer)).performClick()
        waitFor(string(R.string.form_new_transfer_title))
        compose.onNodeWithText(string(R.string.form_to_account)).assertExists()
        compose.onNodeWithText(string(R.string.type_recurring)).assertDoesNotExist()
    }

    @Test
    fun aparenciaTrocaParaOTemaClaro() {
        openMore()
        compose.onNodeWithText(string(R.string.more_settings)).performClick()
        compose.onNodeWithText(string(R.string.settings_theme_light)).performClick()
        // A gravação é assíncrona (e o sistema pode recriar a tela): espera a preferência mudar.
        compose.waitUntil(10_000) { runBlocking { TestApp.context.container.settingsRepository.settings.first().themeMode == ThemeMode.LIGHT } }
        assertEquals(ThemeMode.LIGHT, runBlocking { TestApp.context.container.settingsRepository.settings.first().themeMode })
    }
}
