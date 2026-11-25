package com.example.app_finances.feature.transactions.form

import androidx.compose.runtime.saveable.Saver
import com.example.app_finances.data.local.entity.TransactionEntity
import com.example.app_finances.data.backup.BackupFile

/** O que o formulário de lançamento abre: um existente, um novo, uma compra no cartão, uma transferência, um texto ou a voz. */
sealed interface FormRequest {
    data class Edit(val id: Long) : FormRequest

    data object New : FormRequest

    /** Compra no cartão, já com o cartão escolhido quando vem da tela dele. */
    data class CardPurchase(val cardId: Long? = null) : FormRequest

    /** Transferência entre contas (aberta pela tela "Mais"). */
    data object Transfer : FormRequest

    /** "Recorrência" escolhida no novo lançamento: abre o cadastro de recorrência/assinatura no lugar da folha. */
    data object Recurring : FormRequest

    /**
     * Mês previsto de uma recorrência (ainda não lançado), para ajustar só ele:
     * salvar grava um lançamento pendente naquela data, e a recorrência não o repete.
     */
    data class Occurrence(val transaction: TransactionEntity) : FormRequest

    /** Texto livre (compartilhado do WhatsApp, por exemplo) para preencher o formulário. */
    data class Quick(val text: String) : FormRequest

    /** Atalho "Lançamento por voz": formulário novo que já começa ouvindo. */
    data object Voice : FormRequest

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
                    Voice -> listOf("voice", NONE)
                    Transfer -> listOf("transfer", NONE)
                    is Occurrence -> listOf("occurrence", BackupFile.json.encodeToString(TransactionEntity.serializer(), request.transaction))
                    Recurring -> listOf("recurring", NONE)
                    null -> null
                }
            },
            restore = { (kind, value) ->
                when (kind) {
                    "edit" -> Edit(value as Long)
                    "card" -> CardPurchase((value as Long).takeIf { it != NONE })
                    "quick" -> Quick(value as String)
                    "voice" -> Voice
                    "transfer" -> Transfer
                    "occurrence" -> Occurrence(BackupFile.json.decodeFromString(TransactionEntity.serializer(), value as String))
                    "recurring" -> Recurring
                    else -> New
                }
            },
        )
    }
}
