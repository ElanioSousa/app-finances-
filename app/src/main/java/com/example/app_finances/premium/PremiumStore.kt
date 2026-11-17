package com.example.app_finances.premium

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

/** O que a tela do Premium mostra sobre a compra. */
sealed interface PremiumOffer {
    data object Loading : PremiumOffer

    /** Pronto para comprar, com o preço da Play Store ("R$ 79,90"). */
    data class Available(val price: String) : PremiumOffer

    /** Pagamento iniciado e ainda não confirmado (ex.: boleto). */
    data object Pending : PremiumOffer

    /** Sem Play Store ou sem o produto (ex.: app instalado fora da loja). */
    data object Unavailable : PremiumOffer
}

/**
 * Compra única do Premium. Quem tem o Premium fica guardado no aparelho
 * (`AppSettings.premium`), para valer também sem internet.
 */
interface PremiumStore {
    val offer: StateFlow<PremiumOffer>

    /** Confere as compras na loja (ao abrir o app, no "Restaurar compra") e atualiza o Premium. */
    suspend fun refresh()

    /** Abre a compra da Play Store. Falso se ainda não dá para comprar. */
    fun buy(activity: Activity): Boolean

    companion object {
        /** Produto "compra única" cadastrado no Play Console com este id. */
        const val PRODUCT_ID = "premium_vitalicio"
    }
}
