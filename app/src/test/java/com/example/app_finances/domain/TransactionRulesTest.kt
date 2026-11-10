package com.example.app_finances.domain

import com.example.app_finances.data.local.entity.CategoryKind
import com.example.app_finances.data.local.entity.TransactionEntity
import com.example.app_finances.data.local.entity.TransactionType
import com.example.app_finances.data.local.entity.TransactionType.EXPENSE
import com.example.app_finances.data.local.entity.TransactionType.INCOME
import com.example.app_finances.data.local.entity.TransactionType.TRANSFER
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionRulesTest {

    private fun assertValid(t: TransactionEntity) =
        assertEquals(emptyList<String>(), TransactionRules.violations(t))

    private fun assertInvalid(t: TransactionEntity) =
        assertTrue(TransactionRules.violations(t).isNotEmpty())

    @Test
    fun `despesa e receita na conta são válidas`() {
        assertValid(transaction(EXPENSE))
        assertValid(transaction(INCOME))
    }

    @Test
    fun `valor precisa ser positivo e descrição preenchida`() {
        assertInvalid(transaction(amount = 0))
        assertInvalid(transaction(amount = -100))
        assertInvalid(transaction().copy(description = "  "))
    }

    @Test
    fun `precisa de conta ou cartão, nunca os dois`() {
        assertInvalid(transaction(accountId = null))
        assertInvalid(transaction().copy(cardId = 1))
        assertValid(transaction(accountId = null).copy(cardId = 1))
    }

    @Test
    fun `cartão só aceita despesa`() {
        assertInvalid(transaction(INCOME, accountId = null).copy(cardId = 1))
    }

    @Test
    fun `transferência exige destino diferente da origem`() {
        assertInvalid(transaction(TRANSFER))
        assertInvalid(transaction(TRANSFER).copy(destinationAccountId = 1))
        assertValid(transaction(TRANSFER).copy(destinationAccountId = 2))
        assertInvalid(transaction(EXPENSE).copy(destinationAccountId = 2))
    }

    @Test
    fun `parcelamento precisa estar completo e coerente`() {
        val base = transaction(accountId = null).copy(cardId = 1)
        assertValid(base.copy(installmentGroupId = "g", installmentNumber = 2, installmentCount = 3))
        assertInvalid(base.copy(installmentGroupId = "g", installmentNumber = 4, installmentCount = 3))
        assertInvalid(base.copy(installmentGroupId = "g", installmentNumber = 1, installmentCount = 1))
        assertInvalid(base.copy(installmentNumber = 1, installmentCount = 3))
    }

    @Test
    fun `requireValid explica o motivo`() {
        val error = runCatching { TransactionRules.requireValid(transaction(amount = 0)) }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException && error.message!!.contains("maior que zero"))
    }

    @Test
    fun `categoria precisa combinar com o tipo`() {
        assertNull(TransactionRules.categoryViolation(TransactionType.EXPENSE, CategoryKind.EXPENSE))
        assertNull(TransactionRules.categoryViolation(TransactionType.INCOME, CategoryKind.INCOME))
        assertNull(TransactionRules.categoryViolation(TransactionType.EXPENSE, null))
        assertNotNull(TransactionRules.categoryViolation(TransactionType.INCOME, CategoryKind.EXPENSE))
        assertNotNull(TransactionRules.categoryViolation(TransactionType.TRANSFER, CategoryKind.EXPENSE))
    }

    @Test
    fun `parcela só no cartão e compra no cartão nunca pendente`() {
        val installment = transaction().copy(installmentGroupId = "g", installmentNumber = 1, installmentCount = 2)
        assertInvalid(installment)
        assertValid(installment.copy(accountId = null, cardId = 1))
        assertInvalid(transaction(accountId = null).copy(cardId = 1, paid = false))
    }
}
