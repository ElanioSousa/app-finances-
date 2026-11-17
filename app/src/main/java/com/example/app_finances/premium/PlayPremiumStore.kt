package com.example.app_finances.premium

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.example.app_finances.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume

/** Premium pela Play Store (Play Billing). Não há servidor: a loja é quem diz se a compra existe. */
class PlayPremiumStore(context: Context, private val settings: SettingsRepository) : PremiumStore, PurchasesUpdatedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val offerState = MutableStateFlow<PremiumOffer>(PremiumOffer.Loading)
    private var details: ProductDetails? = null

    /** Abrir o app e a tela do Premium juntos não fazem duas conferências ao mesmo tempo. */
    private val refreshing = Mutex()

    private val client = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    override val offer: StateFlow<PremiumOffer> = offerState.asStateFlow()

    override suspend fun refresh() = refreshing.withLock {
        if (!connect()) {
            offerState.value = PremiumOffer.Unavailable
            return@withLock
        }
        if (details == null) loadOffer()
        syncPurchases()
    }

    override fun buy(activity: Activity): Boolean {
        val product = details ?: return false
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(product).build()))
            .build()
        return client.launchBillingFlow(activity, params).responseCode == BillingResponseCode.OK
    }

    /** Resposta da Play Store depois da compra. */
    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode != BillingResponseCode.OK || purchases == null) return
        scope.launch { runCatching { apply(purchases) }.onFailure { Log.e(TAG, "Falha ao confirmar a compra", it) } }
    }

    private suspend fun connect(): Boolean {
        if (client.isReady) return true
        return suspendCancellableCoroutine { continuation ->
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (continuation.isActive) continuation.resume(result.responseCode == BillingResponseCode.OK)
                }

                override fun onBillingServiceDisconnected() = Unit
            })
        }
    }

    private suspend fun loadOffer() {
        val product = QueryProductDetailsParams.Product.newBuilder().setProductId(PremiumStore.PRODUCT_ID).setProductType(ProductType.INAPP).build()
        val result = client.queryProductDetails(QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build())
        details = result.productDetailsList?.firstOrNull()
        val price = details?.oneTimePurchaseOfferDetails?.formattedPrice
        offerState.value = if (price != null) PremiumOffer.Available(price) else PremiumOffer.Unavailable
    }

    /** Só muda o Premium guardado se a loja respondeu: sem conexão, vale o que já estava. */
    private suspend fun syncPurchases() {
        val result = client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(ProductType.INAPP).build())
        if (result.billingResult.responseCode == BillingResponseCode.OK) apply(result.purchasesList)
    }

    /** Confirma (acknowledge) o que foi pago — sem isso a Play devolve o dinheiro em 3 dias — e grava o Premium. */
    private suspend fun apply(purchases: List<Purchase>) {
        val (owned, other) = purchases.filter { PremiumStore.PRODUCT_ID in it.products }
            .partition { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        owned.filterNot { it.isAcknowledged }.forEach { purchase ->
            client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build())
        }
        if (other.any { it.purchaseState == Purchase.PurchaseState.PENDING }) offerState.value = PremiumOffer.Pending
        settings.setPremium(owned.isNotEmpty())
    }

    private companion object {
        const val TAG = "PlayPremiumStore"
    }
}
