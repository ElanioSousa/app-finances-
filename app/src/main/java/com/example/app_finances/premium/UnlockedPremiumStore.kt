package com.example.app_finances.premium

import android.app.Activity
import com.example.app_finances.data.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Versão pessoal do dono (BuildConfig.PREMIUM_LIBERADO): o Premium vem liberado, sem Play Store. */
class UnlockedPremiumStore(private val settings: SettingsRepository) : PremiumStore {
    override val offer: StateFlow<PremiumOffer> = MutableStateFlow(PremiumOffer.Unavailable)

    override suspend fun refresh() = settings.setPremium(true)

    override fun buy(activity: Activity): Boolean = false
}
