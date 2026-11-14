package com.example.app_finances

import androidx.compose.ui.test.assertCountEquals
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
import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.app_finances.core.format.Money
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Lançamento a partir de texto: pelo campo rápido, pelo "Compartilhar" de outro app e pela voz. */
@RunWith(AndroidJUnit4::class)
class QuickEntryFlowTest {

    @get:Rule
    val compose = createEmptyComposeRule()
    private var scenario: ActivityScenario<MainActivity>? = null
    private fun string(id: Int) = TestApp.string(id)
    private fun waitFor(text: String) = compose.waitUntil(5_000) { compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty() }
    private fun waitUntilReady() = compose.waitUntil(5_000) { compose.onAllNodes(hasText(string(R.string.action_save)) and isEnabled()).fetchSemanticsNodes().isNotEmpty() }

    private var stubbingVoice = false

    @After
    fun stop() {
        scenario?.close()
        if (stubbingVoice) Intents.release()
    }

    /** O reconhecimento de voz do sistema "ouve" [spoken] (o emulador não tem como falar). */
    private fun speak(spoken: String) {
        Intents.init()
        stubbingVoice = true
        val result = Intent().putStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS, arrayListOf(spoken))
        intending(hasAction(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)).respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, result))
    }

    @Test
    fun campoRapidoPreencheOFormulario() {
        scenario = TestApp.launchWithFreshDatabase()
        compose.onNodeWithContentDescription(string(R.string.action_new_transaction)).performClick()
        waitUntilReady()
        compose.onNode(hasSetTextAction() and hasText(string(R.string.form_quick))).performTextInput("uber 23,50")
        compose.onNodeWithContentDescription(string(R.string.form_quick_apply)).performClick()
        waitFor(Money.format(2_350))
        compose.onNodeWithText(string(R.string.action_save)).performScrollTo().performClick()

        compose.onAllNodesWithText(string(R.string.nav_transactions)).onFirst().performClick()
        waitFor("Uber")
        compose.onAllNodesWithText("Transporte", substring = true).onFirst().assertExists()
    }

    @Test
    fun textoCompartilhadoAbreOFormularioPreenchido() {
        scenario = TestApp.launchWithFreshDatabase(TestApp.shareIntent("mercado 45,90"))
        waitFor(string(R.string.form_new_title))
        waitUntilReady()
        waitFor(Money.format(4_590))
        compose.onNodeWithText(string(R.string.action_save)).performScrollTo().performClick()

        compose.onAllNodesWithText(string(R.string.nav_transactions)).onFirst().performClick()
        waitFor("Mercado")
    }

    /** Com o app já aberto (e até com um formulário aberto), cada "Compartilhar" abre o formulário com o seu texto. */
    @Test
    fun compartilharComOAppAbertoUsaATelaJaAberta() {
        scenario = TestApp.launchWithFreshDatabase()
        waitFor(string(R.string.nav_home))
        share("mercado 45,90")
        waitFor(string(R.string.form_new_title))
        waitFor(Money.format(4_590))

        share("uber 12,00")
        waitFor(Money.format(1_200))
        compose.onAllNodesWithText(Money.format(4_590)).assertCountEquals(0)
        assertEquals("uma tela só, sem cópias empilhadas", 1, openMainActivities())
    }

    /** Quantas telas principais estão vivas (cada "Compartilhar" não pode criar outra). */
    private fun openMainActivities(): Int {
        var count = 0
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val monitor = ActivityLifecycleMonitorRegistry.getInstance()
            count = Stage.entries.filter { it != Stage.DESTROYED && it != Stage.PRE_ON_CREATE }
                .sumOf { stage -> monitor.getActivitiesInStage(stage).count { it is MainActivity } }
        }
        return count
    }

    /** Como o Android entrega um "Compartilhar" vindo de outro app ao app que já está aberto. */
    private fun share(text: String) =
        TestApp.context.startActivity(TestApp.shareIntent(text).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

    @Test
    fun atalhoDeVozAbreOFormularioOuvindoEPreenche() {
        speak("mercado quarenta e cinco reais e noventa centavos")
        scenario = TestApp.launchWithFreshDatabase(TestApp.voiceShortcutIntent())
        waitFor(string(R.string.form_new_title))
        waitFor(Money.format(4_590))
        compose.onNodeWithText(string(R.string.action_save)).performScrollTo().performClick()
        compose.onAllNodesWithText(string(R.string.nav_transactions)).onFirst().performClick()
        waitFor("Mercado")
    }

    @Test
    fun microfoneDoCampoRapidoPreencheOFormulario() {
        speak("uber vinte e três e cinquenta")
        scenario = TestApp.launchWithFreshDatabase()
        compose.onNodeWithContentDescription(string(R.string.action_new_transaction)).performClick()
        waitUntilReady()
        compose.onNodeWithContentDescription(string(R.string.form_voice)).performClick()
        waitFor(Money.format(2_350))
        // A frase aparece no campo já escrita, para conferir.
        compose.onAllNodesWithText("uber 23,50").onFirst().assertExists()
    }
}
