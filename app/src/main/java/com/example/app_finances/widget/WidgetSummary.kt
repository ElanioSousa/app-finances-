package com.example.app_finances.widget

import com.example.app_finances.core.designsystem.component.HiddenMoney
import com.example.app_finances.core.designsystem.component.VisibleMoney
import com.example.app_finances.core.format.Money
import com.example.app_finances.di.AppContainer
import com.example.app_finances.domain.BalanceCalculator
import com.example.app_finances.domain.MonthForecast
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import java.time.YearMonth

/** Textos do widget, já formatados (escondidos com "Ocultar valores" ou com o bloqueio ligado). */
data class WidgetSummary(val balance: String, val forecast: String, val locked: Boolean = false) {
    companion object {
        /** Lê tudo em paralelo (são consultas independentes) e formata. */
        suspend fun load(container: AppContainer): WidgetSummary = coroutineScope {
            val settings = container.settingsRepository.settings.first()
            // O widget é do Premium: sem ele, mostra só o convite (sem valores nem consultas).
            if (!settings.premium) return@coroutineScope LOCKED
            val today = container.today()
            val balances = async { container.accountRepository.observeBalances().first() }
            val pending = async { container.transactionRepository.observePendingUntil(YearMonth.from(today).atEndOfMonth()).first() }
            val cardData = async { container.cardRepository.observeCardData().first() }
            val recurrings = async { container.recurringRepository.observeAll().first() }
            val balance = BalanceCalculator.total(balances.await())
            val forecast = MonthForecast.endOfMonth(cardData.await().forecastInputs(balance, pending.await(), recurrings.await()), today)
            // Na tela inicial qualquer um vê: com bloqueio por biometria, os valores ficam escondidos.
            val money = if (settings.hideValues || settings.appLock) HiddenMoney else VisibleMoney
            WidgetSummary(money.format(balance, false), money.format(forecast, false))
        }

        private val LOCKED = WidgetSummary(Money.HIDDEN, Money.HIDDEN, locked = true)
    }
}
