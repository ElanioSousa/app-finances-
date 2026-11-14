package com.example.app_finances.data.repository

import com.example.app_finances.data.local.dao.AccountDao
import com.example.app_finances.data.local.dao.TransactionDao
import com.example.app_finances.data.local.entity.AccountEntity
import com.example.app_finances.domain.AccountBalance
import com.example.app_finances.domain.BalanceCalculator
import com.example.app_finances.domain.currentDay
import com.example.app_finances.domain.requireName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import java.time.LocalDate

class AccountRepository(
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
    private val today: () -> LocalDate = LocalDate::now,
) {
    /** Contas ativas, sem calcular saldo (para listas de escolha e nomes). */
    fun observeAccounts(): Flow<List<AccountEntity>> = accountDao.observeActive()

    /**
     * Contas ativas com saldo de hoje (só o que já foi pago até hoje). Atualiza sozinho quando
     * qualquer movimento muda e quando o dia vira (um lançamento de amanhã passa a contar).
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeBalances(): Flow<List<AccountBalance>> =
        combine(accountDao.observeActive(), currentDay(today).flatMapLatest(transactionDao::observePaidMovements), BalanceCalculator::balances)

    suspend fun add(account: AccountEntity): Long {
        requireName(account.name, "Informe o nome da conta.")
        return accountDao.insert(account)
    }

    suspend fun update(account: AccountEntity) {
        requireName(account.name, "Informe o nome da conta.")
        accountDao.update(account)
    }
}
