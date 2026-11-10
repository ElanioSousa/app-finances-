package com.example.app_finances.data.repository

import com.example.app_finances.data.local.dao.CategoryDao
import com.example.app_finances.data.local.dao.TransactionDao
import com.example.app_finances.data.local.entity.TransactionEntity
import com.example.app_finances.domain.MonthTotals
import com.example.app_finances.domain.TransactionRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.YearMonth

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
) {
    fun observeMonth(month: YearMonth): Flow<List<TransactionEntity>> =
        transactionDao.observeBetween(month.atDay(1), month.atEndOfMonth())

    fun observeMonthTotals(month: YearMonth): Flow<MonthTotals> =
        observeMonth(month).map(MonthTotals::of)

    suspend fun getById(id: Long): TransactionEntity? = transactionDao.getById(id)

    /** Grava um lançamento válido; lança [IllegalArgumentException] com o motivo se não for. */
    suspend fun add(transaction: TransactionEntity): Long {
        requireValid(transaction)
        return transactionDao.insert(transaction)
    }

    suspend fun update(transaction: TransactionEntity) {
        requireValid(transaction)
        transactionDao.update(transaction)
    }

    suspend fun delete(transaction: TransactionEntity) = transactionDao.delete(transaction)

    private suspend fun requireValid(transaction: TransactionEntity) {
        TransactionRules.requireValid(transaction)
        val categoryKind = transaction.categoryId?.let { categoryDao.getById(it)?.kind }
        TransactionRules.categoryViolation(transaction.type, categoryKind)?.let { throw IllegalArgumentException(it) }
    }
}
