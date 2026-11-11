package com.example.app_finances.feature.transactions.form

import androidx.compose.runtime.saveable.Saver

/** O que o formulário de lançamento abre: um existente, um novo, ou uma compra nova no cartão. */
sealed interface FormRequest {
    data class Edit(val id: Long) : FormRequest

    data object New : FormRequest

    /** Compra no cartão, já com o cartão escolhido quando vem da tela dele. */
    data class CardPurchase(val cardId: Long? = null) : FormRequest

    companion object {
        private const val NONE = -1L

        /** Guarda o pedido ao girar a tela: [tipo, id]. */
        val Saver = Saver<FormRequest?, List<Long>>(
            save = { request ->
                when (request) {
                    is Edit -> listOf(0, request.id)
                    New -> listOf(1, NONE)
                    is CardPurchase -> listOf(2, request.cardId ?: NONE)
                    null -> null
                }
            },
            restore = { (kind, id) ->
                when (kind) {
                    0L -> Edit(id)
                    2L -> CardPurchase(id.takeIf { it != NONE })
                    else -> New
                }
            },
        )
    }
}
