package com.example.app_finances.feature.transactions.form

import com.example.app_finances.data.local.entity.TransactionEntity
import com.example.app_finances.data.local.entity.TransactionType
import com.example.app_finances.domain.Installments
import java.time.LocalDate

/** De onde sai o dinheiro de uma despesa. */
enum class PaymentOrigin { ACCOUNT, CARD }

/** O que o usuário está preenchendo no formulário, antes de virar lançamento(s). */
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
    val origin: PaymentOrigin = PaymentOrigin.ACCOUNT,
    val cardId: Long? = null,
    val installments: Int = 1,
) {
    val isTransfer: Boolean get() = type == TransactionType.TRANSFER

    /** Só despesas podem ir para o cartão. */
    val isCardPurchase: Boolean get() = type == TransactionType.EXPENSE && origin == PaymentOrigin.CARD

    /** Troca o tipo limpando o que não vale para o novo (categoria, destino, cartão). */
    fun withType(newType: TransactionType): TransactionDraft =
        if (newType == type) this
        else copy(type = newType, categoryId = null, destinationAccountId = null, origin = PaymentOrigin.ACCOUNT, installments = 1)

    /**
     * Monta o lançamento. Sem descrição, usa o nome da categoria.
     * Ao editar, preserva o que o formulário não mostra (parcelas, recorrência).
     * Compra no cartão não tem conta e não fica "pendente": quem é pago é a fatura.
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
            accountId = accountId.takeUnless { isCardPurchase },
            destinationAccountId = destinationAccountId.takeIf { isTransfer },
            cardId = cardId.takeIf { isCardPurchase },
            paid = paid || isCardPurchase,
        )
    }

    /** Novo lançamento: uma compra parcelada vira um lançamento por parcela. */
    fun toNewEntities(categoryName: String?, newGroupId: () -> String): List<TransactionEntity> {
        val entity = toEntity(original = null, categoryName = categoryName)
        return Installments.schedule(entity, if (isCardPurchase) installments else 1, newGroupId)
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
            origin = if (entity.cardId != null) PaymentOrigin.CARD else PaymentOrigin.ACCOUNT,
            cardId = entity.cardId,
        )
    }
}
