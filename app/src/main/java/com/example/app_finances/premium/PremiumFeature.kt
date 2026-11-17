package com.example.app_finances.premium

import androidx.annotation.StringRes
import com.example.app_finances.R

/**
 * O que o Premium libera, num lugar só: a tela do Premium lista daqui. Cada recurso confere
 * `AppSettings.premium` onde é usado (telas, Configurações, tarefas, widget, leitor de notificações).
 */
enum class PremiumFeature(@param:StringRes val benefitRes: Int) {
    PURCHASE_NOTIFICATIONS(R.string.premium_benefit_notifications),
    OUTLOOK(R.string.premium_benefit_outlook),
    GOALS(R.string.premium_benefit_goals),
    NET_WORTH(R.string.premium_benefit_networth),
    MONTH_ALERTS(R.string.premium_benefit_alerts),
    AUTO_BACKUP(R.string.premium_benefit_backup),
    WIDGET(R.string.premium_benefit_widget),
}
