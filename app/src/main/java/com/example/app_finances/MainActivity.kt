package com.example.app_finances

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import com.example.app_finances.core.security.LockScreen
import com.example.app_finances.data.settings.AppSettings
import com.example.app_finances.navigation.FinancesApp
import kotlinx.coroutines.launch

/** FragmentActivity (e não só ComponentActivity) porque o pedido de biometria precisa dela. */
class MainActivity : FragmentActivity() {

    private val app get() = application as FinancesApplication

    /** Texto recebido pelo "Compartilhar", esperando o formulário abrir (depois do desbloqueio). */
    private var sharedText by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // Ícones claros nas barras do sistema, já que o app é sempre escuro.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        // Uma vez por abertura (não a cada rotação de tela).
        if (savedInstanceState == null) {
            runStartupTasks()
            sharedText = intent.sharedText()
        }
        setContent {
            AppfinancesTheme {
                // Até as preferências carregarem não desenha nada (só o fundo escuro da janela):
                // assim nenhum dado aparece antes do bloqueio por biometria.
                val settings by app.container.settingsRepository.settings.collectAsStateWithLifecycle(initialValue = null)
                settings?.let { AppRoot(it) }
            }
        }
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
                FinancesApp(sharedText, onSharedTextUsed = { sharedText = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.sharedText()?.let { sharedText = it }
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

    private companion object {
        const val TAG = "MainActivity"
        const val MAX_SHARED_TEXT = 300
    }
}
