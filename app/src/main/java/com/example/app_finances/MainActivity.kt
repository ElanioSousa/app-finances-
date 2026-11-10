package com.example.app_finances

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.app_finances.core.designsystem.theme.AppfinancesTheme
import com.example.app_finances.navigation.FinancesApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ícones claros nas barras do sistema, já que o app é sempre escuro.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        // Uma vez por abertura (não a cada rotação de tela).
        if (savedInstanceState == null) generateRecurringTransactions()
        setContent {
            AppfinancesTheme {
                FinancesApp()
            }
        }
    }

    /** Lança salário, aluguel, assinaturas… do mês. Pode rodar várias vezes: nunca duplica. */
    private fun generateRecurringTransactions() {
        val repository = (application as FinancesApplication).container.recurringRepository
        lifecycleScope.launch {
            runCatching { repository.generateDue() }
                .onFailure { Log.e(TAG, "Falha ao gerar lançamentos recorrentes", it) }
        }
    }

    private companion object {
        const val TAG = "MainActivity"
    }
}
