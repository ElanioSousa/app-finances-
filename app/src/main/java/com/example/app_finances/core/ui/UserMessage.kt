package com.example.app_finances.core.ui

import android.database.sqlite.SQLiteException

/**
 * Executa uma gravação e traduz falhas esperadas numa mensagem para o usuário.
 * Regras de negócio chegam como [IllegalArgumentException] com texto em português;
 * erros do banco (ex.: restrições) viram uma mensagem genérica.
 */
suspend fun runForUserMessage(block: suspend () -> Unit): String? = try {
    block()
    null
} catch (e: IllegalArgumentException) {
    e.message ?: GENERIC_ERROR
} catch (e: SQLiteException) {
    GENERIC_ERROR
}

private const val GENERIC_ERROR = "Não foi possível salvar. Tente novamente."
