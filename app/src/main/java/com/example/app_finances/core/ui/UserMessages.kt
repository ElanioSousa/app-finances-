package com.example.app_finances.core.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Ações rápidas de um ViewModel (pausar, marcar como pago…) cujas falhas viram
 * avisos de uma só vez para a tela mostrar num snackbar.
 */
class UserMessages(private val scope: CoroutineScope) {
    private val channel = Channel<String>(Channel.BUFFERED)
    val flow: Flow<String> = channel.receiveAsFlow()

    fun send(message: String) {
        channel.trySend(message)
    }

    /** Executa [block]; se falhar com um motivo conhecido, avisa o usuário. */
    fun launch(block: suspend () -> Unit) {
        scope.launch { runForUserMessage(block)?.let(::send) }
    }
}
