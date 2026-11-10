package com.example.app_finances.core.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Um rascunho aberto num diálogo, com a situação do envio. */
data class Editing<T>(
    val value: T,
    val isSaving: Boolean = false,
    val error: String? = null,
)

/**
 * Ciclo de um diálogo de edição: abrir, alterar, salvar e fechar.
 * Só fecha depois que a gravação for aceita; se falhar, mostra o motivo e continua aberto.
 */
class EditorController<T>(private val scope: CoroutineScope) {
    private val editing = MutableStateFlow<Editing<T>?>(null)
    val state: StateFlow<Editing<T>?> = editing.asStateFlow()

    fun open(value: T) {
        editing.value = Editing(value)
    }

    fun close() {
        editing.value = null
    }

    fun edit(transform: (T) -> T) = editing.update { it?.copy(value = transform(it.value), error = null) }

    fun save(persist: suspend (T) -> Unit) {
        val current = editing.value ?: return
        if (current.isSaving) return
        editing.value = current.copy(isSaving = true)
        scope.launch {
            val error = runForUserMessage { persist(current.value) }
            editing.update { if (error == null) null else it?.copy(isSaving = false, error = error) }
        }
    }
}
