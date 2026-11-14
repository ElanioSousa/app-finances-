package com.example.app_finances.core.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.Flow

/** Snackbar do Scaffold principal, para qualquer tela avisar algo (ex.: "Desfazer"). */
val LocalSnackbarHostState = staticCompositionLocalOf<SnackbarHostState> {
    error("LocalSnackbarHostState não foi fornecido")
}

/** Mostra no snackbar cada aviso de [messages] (falhas de um ViewModel); [afterEach] roda depois de cada um. */
@Composable
fun ShowMessages(messages: Flow<String>, afterEach: () -> Unit = {}) {
    val snackbar = LocalSnackbarHostState.current
    val after by rememberUpdatedState(afterEach)
    LaunchedEffect(messages) { messages.collect { snackbar.showSnackbar(it); after() } }
}
