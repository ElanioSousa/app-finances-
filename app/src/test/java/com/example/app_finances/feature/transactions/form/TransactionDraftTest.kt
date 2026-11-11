package com.example.app_finances.feature.transactions.form

import com.example.app_finances.core.model.PaymentOrigin
import com.example.app_finances.data.local.entity.TransactionType
import com.example.app_finances.domain.transaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TransactionDraftTest {

    @Test
    fun `sem descrição usa o nome da categoria e limpa espaços`() {
        val entity = TransactionDraft(amountCents = 1_000, accountId = 1, categoryId = 3, notes = "  ").toEntity(null, "Mercado")
        assertEquals("Mercado", entity.description)
        assertNull(entity.notes)
        assertEquals(0L, entity.id)
    }

    @Test
    fun `transferência não leva categoria e só ela leva destino`() {
        val transfer = TransactionDraft(type = TransactionType.TRANSFER, amountCents = 1, accountId = 1, destinationAccountId = 2, categoryId = 3)
            .toEntity(null, null)
        assertNull(transfer.categoryId)
        assertEquals(2L, transfer.destinationAccountId)

        val expense = TransactionDraft(accountId = 1, destinationAccountId = 2).toEntity(null, null)
        assertNull(expense.destinationAccountId)
    }

    @Test
    fun `trocar o tipo limpa categoria e destino`() {
        val draft = TransactionDraft(categoryId = 3, destinationAccountId = 2).withType(TransactionType.INCOME)
        assertNull(draft.categoryId)
        assertNull(draft.destinationAccountId)
        assertEquals(draft, draft.withType(TransactionType.INCOME))
    }

    @Test
    fun `editar preserva id, parcelas e recorrência`() {
        val original = transaction().copy(id = 7, installmentGroupId = "g", installmentNumber = 1, installmentCount = 3, recurringId = 4)
        val edited = TransactionDraft.from(original).copy(amountCents = 5_000).toEntity(original, null)
        assertEquals(original.copy(amountCents = 5_000), edited)
    }

    @Test
    fun `no cartão, à vista gera um lançamento e parcelada gera um por parcela`() {
        val card = TransactionDraft(amountCents = 30_000, origin = PaymentOrigin.CARD, cardId = 3, installments = 3)
        assertEquals(1, card.toNewEntities(null) { "g" }.size)
        assertEquals(3, card.withInstallments(true).toNewEntities(null) { "g" }.size)
        assertEquals(2, card.copy(installments = 1).withInstallments(true).installments)
        assertEquals(1, card.withInstallments(true).withInstallments(false).installmentCount)
    }

    @Test
    fun `compra parcelada exige de 2 parcelas em diante`() {
        val draft = TransactionDraft(amountCents = 30_000, origin = PaymentOrigin.CARD, cardId = 3).withInstallments(true).copy(installments = 0)
        assertEquals("Informe de 2 a 24 parcelas.", runCatching { draft.toNewEntities(null) { "g" } }.exceptionOrNull()?.message)
    }
}
