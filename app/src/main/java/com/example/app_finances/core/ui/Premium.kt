package com.example.app_finances.core.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.app_finances.R
import com.example.app_finances.core.designsystem.component.CategoryIcon
import com.example.app_finances.core.designsystem.component.SectionCard
import com.example.app_finances.core.designsystem.theme.FinanceTheme

/** O usuário tem o Premium? (vem das preferências; o app inteiro lê daqui). */
val LocalPremium = compositionLocalOf { false }

/** Abre a tela do Premium (de onde quer que se esteja). */
val LocalOpenPremium = staticCompositionLocalOf<() -> Unit> { {} }

/** No lugar de um recurso Premium, para quem ainda não tem: o que é e o caminho para liberar. */
@Composable
fun PremiumLocked(feature: String) {
    val open = LocalOpenPremium.current
    SectionCard {
        CategoryIcon(Icons.Outlined.WorkspacePremium, FinanceTheme.colors.warning)
        Text(stringResource(R.string.premium_locked_title, feature), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.premium_locked_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = open, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.premium_see)) }
    }
}

/** [action] se o usuário tem o Premium; senão, abre a tela do Premium (para botões). */
@Composable
fun premiumOnly(action: () -> Unit): () -> Unit = if (LocalPremium.current) action else LocalOpenPremium.current

/** [action] se o usuário tem o Premium; senão, abre a tela do Premium (para interruptores). */
@Composable
fun <T> premiumOnly(action: (T) -> Unit): (T) -> Unit {
    if (LocalPremium.current) return action
    val open = LocalOpenPremium.current
    return { open() }
}
