package com.example.app_finances.widget

import com.example.app_finances.core.designsystem.component.HiddenMoney
import com.example.app_finances.core.designsystem.component.VisibleMoney
import com.example.app_finances.di.AppContainer
import com.example.app_finances.domain.BalanceCalculator
import com.example.app_finances.domain.MonthForecast
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import java.time.YearMonth

/** Textos do widget, já formatados (escondidos com "Ocultar valores" ou com o bloqueio ligado). */
data class WidgetSummary(val balance: String, val forecast: String) {
    companion object {
        /** Lê tudo em paralelo (são consultas independentes) e formata. */
        suspend fun load(container: AppContainer): WidgetSummary = coroutineScope {
            val today = container.today()
            val settings = async { container.settingsRepository.settings.first() }
            val balances = async { container.accountRepository.observeBalances().first() }
            val pending = async { container.transactionRepository.observePendingUntil(YearMonth.from(today).atEndOfMonth()).first() }
            val cards = async { container.cardRepository.observeCardData().first().summaries(today) }
            val recurrings = async { container.recurringRepository.observeAll().first() }
            val balance = BalanceCalculator.total(balances.await())
            val forecast = MonthForecast.endOfMonth(balance, pending.await(), recurrings.await(), cards.await(), today)
            // Na tela inicial qualquer um vê: com bloqueio por biometria, os valores ficam escondidos.
            val money = settings.await().let { if (it.hideValues || it.appLock) HiddenMoney else VisibleMoney }
            WidgetSummary(money.format(balance, false), money.format(forecast, false))
        }
    }
}
