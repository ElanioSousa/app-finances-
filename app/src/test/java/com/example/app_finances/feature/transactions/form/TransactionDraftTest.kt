package com.example.app_finances.feature.transactions.form

import com.example.app_finances.core.model.PaymentOrigin
import com.example.app_finances.data.local.entity.TransactionType
import com.example.app_finances.domain.transaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

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
        assertEquals(1, card.toNewEntities(null, TODAY) { "g" }.size)
        assertEquals(3, card.withInstallments(true).toNewEntities(null, TODAY) { "g" }.size)
        assertEquals(2, card.copy(installments = 1).withInstallments(true).installments)
        assertEquals(1, card.withInstallments(true).withInstallments(false).installmentCount)
    }

    @Test
    fun `compra parcelada exige de 2 parcelas em diante`() {
        val draft = TransactionDraft(amountCents = 30_000, origin = PaymentOrigin.CARD, cardId = 3).withInstallments(true).copy(installments = 0)
        assertEquals("Informe de 2 a 24 parcelas.", runCatching { draft.toNewEntities(null, TODAY) { "g" } }.exceptionOrNull()?.message)
    }

    @Test
    fun dataFuturaDeixaPendenteEVoltarParaHojeMarcaPagoDeNovo() {
        val today = LocalDate.of(2026, 11, 14)
        val future = TransactionDraft(paid = true, date = today).withDate(LocalDate.of(2027, 2, 17), today)
        assertEquals(false, future.paid)
        assertEquals(true, future.withDate(today, today).paid)
        // Entre datas passadas, a escolha do usuário fica como estava.
        assertEquals(false, TransactionDraft(paid = false, date = today).withDate(today.minusDays(1), today).paid)
    }

    @Test
    fun `receita parcelada na conta gera uma por mes e as futuras pendentes`() {
        val draft = TransactionDraft(
            type = TransactionType.INCOME, amountCents = 600_000, date = TODAY, launchDate = TODAY,
            description = "Projeto", accountId = 1, paid = true, installments = 3,
        ).withInstallments(true)
        val parcels = draft.toNewEntities(null, TODAY) { "g" }
        assertEquals(listOf(TODAY, TODAY.plusMonths(1), TODAY.plusMonths(2)), parcels.map { it.date })
        assertEquals(listOf(true, false, false), parcels.map { it.paid })
        assertEquals(listOf(200_000L, 200_000L, 200_000L), parcels.map { it.amountCents })
        assertEquals(setOf(TODAY), parcels.map { it.launchDate }.toSet())
    }

    @Test
    fun `transferencia nao parcela`() {
        val transfer = TransactionDraft(type = TransactionType.TRANSFER, amountCents = 1_000, accountId = 1, destinationAccountId = 2, installments = 3).withInstallments(true)
        assertEquals(1, transfer.installmentCount)
    }

    private companion object {
        val TODAY: LocalDate = LocalDate.of(2026, 11, 23)
    }
}
