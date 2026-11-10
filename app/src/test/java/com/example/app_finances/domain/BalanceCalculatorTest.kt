package com.example.app_finances.domain

import com.example.app_finances.domain.MovementKind.EXPENSE
import com.example.app_finances.domain.MovementKind.INCOME
import com.example.app_finances.domain.MovementKind.INVOICE_PAYMENT
import com.example.app_finances.domain.MovementKind.TRANSFER_IN
import com.example.app_finances.domain.MovementKind.TRANSFER_OUT
import org.junit.Assert.assertEquals
import org.junit.Test

class BalanceCalculatorTest {

    @Test
    fun `sem movimentos o saldo é o inicial`() {
        val balances = BalanceCalculator.balances(listOf(account(1, initial = 5_000)), emptyList())
        assertEquals(5_000, balances.single().balanceCents)
    }

    @Test
    fun `receitas somam, despesas e transferências enviadas subtraem`() {
        val movements = listOf(
            AccountMovement(1, INCOME, 10_000),
            AccountMovement(1, EXPENSE, 3_000),
            AccountMovement(1, TRANSFER_OUT, 2_000),
            AccountMovement(2, TRANSFER_IN, 2_000),
        )
        val balances = BalanceCalculator.balances(listOf(account(1, 1_000), account(2)), movements)
        assertEquals(listOf(6_000L, 2_000L), balances.map { it.balanceCents })
    }

    @Test
    fun `transferência não muda o total entre contas`() {
        val movements = listOf(AccountMovement(1, TRANSFER_OUT, 2_500), AccountMovement(2, TRANSFER_IN, 2_500))
        val balances = BalanceCalculator.balances(listOf(account(1, 10_000), account(2)), movements)
        assertEquals(10_000, BalanceCalculator.total(balances))
    }

    @Test
    fun `pagamento de fatura sai da conta`() {
        val balances = BalanceCalculator.balances(listOf(account(1, 10_000)), listOf(AccountMovement(1, INVOICE_PAYMENT, 4_000)))
        assertEquals(6_000, balances.single().balanceCents)
    }

    @Test
    fun `movimento de conta desconhecida (arquivada) é ignorado`() {
        val balances = BalanceCalculator.balances(listOf(account(1)), listOf(AccountMovement(99, INCOME, 1_000)))
        assertEquals(0, BalanceCalculator.total(balances))
    }
}
