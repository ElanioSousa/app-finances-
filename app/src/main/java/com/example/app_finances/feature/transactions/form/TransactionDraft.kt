package com.example.app_finances.feature.transactions.form

import com.example.app_finances.data.local.entity.TransactionEntity
import com.example.app_finances.data.local.entity.TransactionType
import java.time.LocalDate

/** O que o usuário está preenchendo no formulário, antes de virar um lançamento. */
data class TransactionDraft(
    val type: TransactionType = TransactionType.EXPENSE,
    val amountCents: Long = 0,
    val date: LocalDate = LocalDate.now(),
    val description: String = "",
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val destinationAccountId: Long? = null,
    val paid: Boolean = true,
    val notes: String = "",
) {
    val isTransfer: Boolean get() = type == TransactionType.TRANSFER

    /** Troca o tipo limpando o que não vale para o novo (categoria de outro tipo, destino). */
    fun withType(newType: TransactionType): TransactionDraft =
        if (newType == type) this else copy(type = newType, categoryId = null, destinationAccountId = null)

    /**
     * Monta o lançamento. Sem descrição, usa o nome da categoria.
     * Ao editar, preserva o que o formulário não mostra (parcelas, recorrência, cartão).
     */
    fun toEntity(original: TransactionEntity?, categoryName: String?): TransactionEntity {
        val base = original ?: TransactionEntity(type = type, amountCents = 0, date = date, description = "")
        return base.copy(
            type = type,
            amountCents = amountCents,
            date = date,
            description = description.trim().ifEmpty { categoryName.orEmpty() },
            notes = notes.trim().ifEmpty { null },
            categoryId = categoryId.takeUnless { isTransfer },
            accountId = accountId,
            destinationAccountId = destinationAccountId.takeIf { isTransfer },
            paid = paid,
        )
    }

    companion object {
        fun from(entity: TransactionEntity) = TransactionDraft(
            type = entity.type,
            amountCents = entity.amountCents,
            date = entity.date,
            description = entity.description,
            categoryId = entity.categoryId,
            accountId = entity.accountId,
            destinationAccountId = entity.destinationAccountId,
            paid = entity.paid,
            notes = entity.notes.orEmpty(),
        )
    }
}
