package com.example.app_finances.feature.transactions.form

import com.example.app_finances.core.model.PaymentOrigin
import com.example.app_finances.data.local.entity.TransactionEntity
import com.example.app_finances.data.local.entity.TransactionType
import com.example.app_finances.domain.Installments
import com.example.app_finances.domain.QuickEntry
import com.example.app_finances.domain.TransactionRules
import java.time.LocalDate

/** O que o usuário está preenchendo no formulário, antes de virar lançamento(s). */
data class TransactionDraft(
    val type: TransactionType = TransactionType.EXPENSE,
    val amountCents: Long = 0,
    /** Vencimento (data da compra, no cartão): é o que decide o mês e o saldo. */
    val date: LocalDate = LocalDate.now(),
    /** Dia em que foi lançado (só registro); por padrão, hoje. Nulo nos antigos: a tela mostra o vencimento. */
    val launchDate: LocalDate? = LocalDate.now(),
    val description: String = "",
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val destinationAccountId: Long? = null,
    val paid: Boolean = true,
    val notes: String = "",
    val origin: PaymentOrigin = PaymentOrigin.ACCOUNT,
    val cardId: Long? = null,
    /** "Parcelado" escolhido (senão é à vista). Na conta ou no cartão; transferência não. */
    val inInstallments: Boolean = false,
    val installments: Int = 1,
) {
    val isTransfer: Boolean get() = type == TransactionType.TRANSFER

    /** Só despesas podem ir para o cartão. */
    val isCardPurchase: Boolean get() = type == TransactionType.EXPENSE && origin == PaymentOrigin.CARD

    /** Troca o tipo limpando o que não vale para o novo (categoria, destino, cartão). */
    fun withType(newType: TransactionType): TransactionDraft =
        if (newType == type) this
        else copy(type = newType, categoryId = null, destinationAccountId = null, origin = PaymentOrigin.ACCOUNT, inInstallments = false, installments = 1)

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
            launchDate = launchDate,
            description = description.trim().ifEmpty { categoryName.orEmpty() },
            notes = notes.trim().ifEmpty { null },
            categoryId = categoryId.takeUnless { isTransfer },
            accountId = accountId.takeUnless { isCardPurchase },
            destinationAccountId = destinationAccountId.takeIf { isTransfer },
            cardId = cardId.takeIf { isCardPurchase },
            paid = paid || isCardPurchase,
        )
    }

    /** Preenche com o que foi entendido de um texto ("mercado 45,90 nubank"); o resto fica como estava. */
    fun withQuickEntry(entry: QuickEntry, today: LocalDate = LocalDate.now()): TransactionDraft {
        val onCard = entry.type == TransactionType.EXPENSE && (entry.onCard || entry.cardId != null)
        val filled = withType(entry.type).copy(
            amountCents = entry.amountCents ?: amountCents,
            description = entry.description,
            categoryId = entry.categoryId,
            accountId = entry.accountId ?: accountId,
            origin = if (onCard) PaymentOrigin.CARD else PaymentOrigin.ACCOUNT,
            cardId = entry.cardId ?: cardId,
        ).withDate(entry.date ?: date, today)
        if (entry.installments <= 1) return filled.withInstallments(false)
        return filled.withInstallments(true).copy(installments = entry.installments)
    }

    /**
     * Troca a data: no futuro ainda não foi pago/recebido, então a chave desliga sozinha
     * (e volta a ligar se a data voltar para hoje ou antes). Dá para mudar a chave depois.
     */
    fun withDate(newDate: LocalDate, today: LocalDate): TransactionDraft = copy(
        date = newDate,
        paid = when {
            newDate > today -> false
            date > today -> true
            else -> paid
        },
    )

    /** À vista no cartão ou parcelada: escolher parcelada já sugere 2 parcelas. */
    fun withInstallments(parcelled: Boolean): TransactionDraft =
        copy(inInstallments = parcelled, installments = if (parcelled) installments.coerceAtLeast(2) else 1)

    /** Parcelado é possível em receita e despesa, na conta ou no cartão (não em transferência). */
    val canBeInstallments: Boolean get() = TransactionRules.canHaveInstallments(type)

    /** Quantidade de parcelas que vai ser gravada (1 = à vista). */
    val installmentCount: Int get() = if (canBeInstallments && inInstallments) installments else 1

    /**
     * Novo lançamento: parcelado vira um lançamento por parcela, uma por mês a partir do vencimento
     * (na conta, as que vencem depois de [today] ficam pendentes; ver [Installments.schedule]).
     */
    fun toNewEntities(categoryName: String?, today: LocalDate, newGroupId: () -> String): List<TransactionEntity> {
        if (canBeInstallments && inInstallments) {
            require(installments in 2..Installments.MAX) { "Informe de 2 a ${Installments.MAX} parcelas." }
        }
        return Installments.schedule(toEntity(original = null, categoryName = categoryName), installmentCount, today, newGroupId)
    }

    companion object {
        fun from(entity: TransactionEntity) = TransactionDraft(
            type = entity.type,
            amountCents = entity.amountCents,
            date = entity.date,
            launchDate = entity.launchDate,
            description = entity.description,
            categoryId = entity.categoryId,
            accountId = entity.accountId,
            destinationAccountId = entity.destinationAccountId,
            paid = entity.paid,
            notes = entity.notes.orEmpty(),
            origin = PaymentOrigin.of(entity.cardId),
            cardId = entity.cardId,
        )
    }
}
