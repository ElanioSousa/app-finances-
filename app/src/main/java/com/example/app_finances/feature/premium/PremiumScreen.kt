package com.example.app_finances.feature.premium

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.app_finances.BuildConfig
import com.example.app_finances.R
import com.example.app_finances.appContainer
import com.example.app_finances.core.designsystem.component.ScreenColumn
import com.example.app_finances.core.designsystem.component.SectionCard
import com.example.app_finances.core.ui.LocalPremium
import com.example.app_finances.premium.PremiumFeature
import com.example.app_finances.premium.PremiumOffer
import kotlinx.coroutines.launch

@Composable
fun PremiumScreen(scaffoldPadding: PaddingValues, onBack: () -> Unit) {
    val context = LocalContext.current
    val store = context.appContainer.premiumStore
    val offer by store.offer.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val refresh: () -> Unit = { scope.launch { runCatching { store.refresh() } } }
    LaunchedEffect(store) { runCatching { store.refresh() } }

    ScreenColumn(title = stringResource(R.string.premium_title), scaffoldPadding = scaffoldPadding, onBack = onBack) {
        item { Benefits() }
        item { if (LocalPremium.current) Owned() else Purchase(offer, onBuy = { context.findActivity()?.let(store::buy) }, onRestore = refresh) }
        if (BuildConfig.DEBUG) item { DebugUnlock() }
    }
}

@Composable
private fun Benefits() {
    SectionCard(title = stringResource(R.string.premium_includes)) {
        PremiumFeature.entries.forEach { Text("• " + stringResource(it.benefitRes), style = MaterialTheme.typography.bodyMedium) }
        Text(stringResource(R.string.premium_once), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Owned() {
    SectionCard { Text(stringResource(R.string.premium_owned), style = MaterialTheme.typography.titleMedium) }
}

@Composable
private fun Purchase(offer: PremiumOffer, onBuy: () -> Unit, onRestore: () -> Unit) {
    SectionCard {
        when (offer) {
            is PremiumOffer.Available -> Button(onClick = onBuy, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.premium_buy, offer.price)) }
            PremiumOffer.Pending -> Text(stringResource(R.string.premium_pending))
            PremiumOffer.Loading -> Text(stringResource(R.string.premium_loading), color = MaterialTheme.colorScheme.onSurfaceVariant)
            PremiumOffer.Unavailable -> Text(stringResource(R.string.premium_unavailable), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedButton(onClick = onRestore, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.premium_restore)) }
    }
}

/** Só na versão de teste (fora da Play Store a compra não existe): libera para experimentar. */
@Composable
private fun DebugUnlock() {
    val settings = LocalContext.current.appContainer.settingsRepository
    val scope = rememberCoroutineScope()
    val premium = LocalPremium.current
    TextButton(onClick = { scope.launch { settings.setPremium(!premium) } }) {
        Text(stringResource(if (premium) R.string.premium_debug_lock else R.string.premium_debug_unlock))
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
