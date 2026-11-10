package com.example.app_finances.domain

import com.example.app_finances.data.local.entity.AccountEntity

/** Conta com o saldo atual já calculado. */
data class AccountBalance(
    val account: AccountEntity,
    val balanceCents: Long,
)

/**
 * Saldo atual = saldo inicial + receitas − despesas − transferências enviadas
 * + transferências recebidas − faturas de cartão pagas. Só entra o que está pago.
 */
object BalanceCalculator {

    fun balances(accounts: List<AccountEntity>, movements: List<AccountMovement>): List<AccountBalance> {
        val deltas = movements.groupingBy { it.accountId }
            .fold(0L) { sum, movement -> sum + movement.kind.sign * movement.totalCents }
        return accounts.map { account ->
            AccountBalance(account, account.initialBalanceCents + deltas.getOrDefault(account.id, 0L))
        }
    }

    fun total(balances: List<AccountBalance>): Long = balances.sumOf { it.balanceCents }
}
