package com.example.app_finances.feature.transactions.form

import androidx.compose.runtime.saveable.Saver

/** O que o formulário de lançamento abre: um existente, um novo, ou uma compra nova no cartão. */
sealed interface FormRequest {
    data class Edit(val id: Long) : FormRequest

    data object New : FormRequest

    /** Compra no cartão, já com o cartão escolhido quando vem da tela dele. */
    data class CardPurchase(val cardId: Long? = null) : FormRequest

    /** Texto livre (compartilhado do WhatsApp, por exemplo) para preencher o formulário. */
    data class Quick(val text: String) : FormRequest

    companion object {
        private const val NONE = -1L

        /** Guarda o pedido ao girar a tela: [tipo, id ou texto]. */
        val Saver = Saver<FormRequest?, List<Any>>(
            save = { request ->
                when (request) {
                    is Edit -> listOf("edit", request.id)
                    New -> listOf("new", NONE)
                    is CardPurchase -> listOf("card", request.cardId ?: NONE)
                    is Quick -> listOf("quick", request.text)
                    null -> null
                }
            },
            restore = { (kind, value) ->
                when (kind) {
                    "edit" -> Edit(value as Long)
                    "card" -> CardPurchase((value as Long).takeIf { it != NONE })
                    "quick" -> Quick(value as String)
                    else -> New
                }
            },
        )
    }
}
