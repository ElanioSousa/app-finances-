package com.example.app_finances.data.repository

import com.example.app_finances.data.local.dao.CategoryDao
import com.example.app_finances.data.local.dao.TransactionDao
import com.example.app_finances.data.local.entity.TransactionEntity
import com.example.app_finances.domain.TransactionRules
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
) {
    fun observeMonth(month: YearMonth): Flow<List<TransactionEntity>> = observeMonths(month, month)

    /** Lançamentos de [from] até [to] (meses inteiros), numa consulta só. */
    fun observeMonths(from: YearMonth, to: YearMonth): Flow<List<TransactionEntity>> =
        transactionDao.observeBetween(from.atDay(1), to.atEndOfMonth())

    fun observePendingUntil(until: LocalDate): Flow<List<TransactionEntity>> = transactionDao.observePendingUntil(until)

    /** Confirma que foi pago/recebido. */
    suspend fun markPaid(transaction: TransactionEntity) = update(transaction.copy(paid = true))

    suspend fun getById(id: Long): TransactionEntity? = transactionDao.getById(id)

    /** Grava um lançamento válido; lança [IllegalArgumentException] com o motivo se não for. */
    suspend fun add(transaction: TransactionEntity): Long {
        requireValid(transaction)
        return transactionDao.insert(transaction)
    }

    /** Grava vários de uma vez (ex.: parcelas): ou entram todos, ou nenhum. */
    suspend fun addAll(transactions: List<TransactionEntity>) {
        transactions.forEach { requireValid(it) }
        transactionDao.insertAll(transactions)
    }

    suspend fun update(transaction: TransactionEntity) {
        requireValid(transaction)
        transactionDao.update(transaction)
    }

    /**
     * Exclui o lançamento; se for parcela, exclui a compra inteira (todas as parcelas).
     * Devolve o que foi excluído, para permitir desfazer.
     */
    suspend fun delete(transaction: TransactionEntity): List<TransactionEntity> {
        val group = transaction.installmentGroupId?.let { transactionDao.getInstallmentGroup(it) }
        val deleted = group?.takeIf { it.isNotEmpty() } ?: listOf(transaction)
        transactionDao.delete(deleted)
        return deleted
    }

    private suspend fun requireValid(transaction: TransactionEntity) {
        TransactionRules.requireValid(transaction)
        categoryDao.requireKindMatches(transaction.type, transaction.categoryId)
    }
}
