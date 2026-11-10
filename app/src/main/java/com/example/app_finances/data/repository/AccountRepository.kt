package com.example.app_finances.data.repository

import com.example.app_finances.data.local.dao.AccountDao
import com.example.app_finances.data.local.dao.TransactionDao
import com.example.app_finances.data.local.entity.AccountEntity
import com.example.app_finances.domain.AccountBalance
import com.example.app_finances.domain.BalanceCalculator
import com.example.app_finances.domain.requireName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class AccountRepository(
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
) {
    /** Contas ativas com saldo atual; atualiza sozinho quando qualquer movimento muda. */
    fun observeBalances(): Flow<List<AccountBalance>> =
        combine(accountDao.observeActive(), transactionDao.observePaidMovements(), BalanceCalculator::balances)

    suspend fun add(account: AccountEntity): Long {
        requireName(account.name, "Informe o nome da conta.")
        return accountDao.insert(account)
    }

    suspend fun update(account: AccountEntity) {
        requireName(account.name, "Informe o nome da conta.")
        accountDao.update(account)
    }
}
