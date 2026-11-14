package com.example.app_finances

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.app_finances.core.designsystem.component.HiddenMoney
import com.example.app_finances.core.designsystem.component.LocalMoneyFormatter
import com.example.app_finances.core.designsystem.component.VisibleMoney
import com.example.app_finances.core.designsystem.theme.AppfinancesTheme
import com.example.app_finances.core.designsystem.theme.SystemNightMode
import com.example.app_finances.core.security.LockScreen
import com.example.app_finances.data.settings.AppSettings
import com.example.app_finances.feature.transactions.form.FormRequest
import com.example.app_finances.navigation.FinancesApp
import com.example.app_finances.widget.SummaryWidget
import kotlinx.coroutines.launch

/** FragmentActivity (e não só ComponentActivity) porque o pedido de biometria precisa dela. */
class MainActivity : FragmentActivity() {

    private val app get() = application as FinancesApplication

    /** Formulário pedido de fora ("Compartilhar" ou atalho de voz), esperando abrir (depois do desbloqueio). */
    private var launchRequest by mutableStateOf<FormRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // Começa como o fundo da janela (que já segue o tema escolhido, ver SystemNightMode).
        applySystemBars(darkTheme = isNightConfiguration())
        // Uma vez por abertura (não a cada rotação de tela).
        if (savedInstanceState == null) {
            runStartupTasks()
            launchRequest = intent.formRequest()
        }
        setContent {
            // Até as preferências carregarem não desenha nada (só o fundo escuro da janela):
            // assim nenhum dado aparece antes do bloqueio por biometria.
            val settings by app.container.settingsRepository.settings.collectAsStateWithLifecycle(initialValue = null)
            settings?.let { current ->
                val dark = current.themeMode.isDark(isSystemInDarkTheme())
                LaunchedEffect(current.themeMode) { SystemNightMode.apply(this@MainActivity, current.themeMode) }
                LaunchedEffect(dark) { applySystemBars(dark) }
                AppfinancesTheme(darkTheme = dark) { AppRoot(current) }
            }
        }
    }

    private fun isNightConfiguration(): Boolean =
        resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

    /** Ícones das barras do sistema claros no tema escuro e escuros no tema claro. */
    private fun applySystemBars(darkTheme: Boolean) {
        val style = if (darkTheme) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }

    @Composable
    private fun AppRoot(settings: AppSettings) {
        // Se o aparelho perdeu o bloqueio de tela, não há como autenticar: não trava o usuário fora do app.
        if (settings.appLock && !app.appLock.unlocked && app.appLock.available) {
            val title = stringResource(R.string.lock_prompt_title)
            LockScreen(onUnlock = { app.appLock.prompt(this, title) })
            return
        }
        CompositionLocalProvider(LocalMoneyFormatter provides if (settings.hideValues) HiddenMoney else VisibleMoney) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                FinancesApp(launchRequest, onLaunchRequestUsed = { launchRequest = null })
            }
        }
    }

    /** Ao sair do app, o widget mostra o saldo já com o que mudou. */
    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) SummaryWidget.refresh(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.formRequest()?.let { launchRequest = it }
    }

    /** "Compartilhar" um texto preenche o formulário; o atalho de voz o abre ouvindo. */
    private fun Intent.formRequest(): FormRequest? = when (action) {
        ACTION_VOICE_ENTRY -> FormRequest.Voice
        ACTION_NEW_ENTRY -> FormRequest.New
        else -> sharedText()?.let(FormRequest::Quick)
    }

    /** Texto de um "Compartilhar" (limitado: é só para preencher um lançamento). */
    private fun Intent.sharedText(): String? =
        getStringExtra(Intent.EXTRA_TEXT)
            ?.takeIf { action == Intent.ACTION_SEND && type?.startsWith("text/") == true }
            ?.trim()?.take(MAX_SHARED_TEXT)?.takeIf { it.isNotEmpty() }

    /** Lança as recorrências do mês e mantém o lembrete diário agendado, se ligado. Nunca duplicam. */
    private fun runStartupTasks() {
        val container = app.container
        lifecycleScope.launch {
            runCatching {
                container.recurringRepository.generateDue()
            }.onFailure { Log.e(TAG, "Falha nas tarefas de abertura", it) }
        }
    }

    companion object {
        /** Atalho "Lançamento por voz" do ícone do app (o mesmo de res/xml/shortcuts.xml). */
        const val ACTION_VOICE_ENTRY = "com.example.app_finances.action.VOICE_ENTRY"

        /** "+" do widget: abre o formulário de novo lançamento. */
        const val ACTION_NEW_ENTRY = "com.example.app_finances.action.NEW_ENTRY"
        private const val TAG = "MainActivity"
        private const val MAX_SHARED_TEXT = 300
    }
}
