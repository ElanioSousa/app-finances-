package com.example.app_finances.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.app_finances.data.local.entity.TransactionEntity
import com.example.app_finances.domain.AccountMovement
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface TransactionDao {
    /** Lançamentos entre [start] e [end] (inclusive), mais recentes primeiro. */
    @Query("SELECT * FROM transactions WHERE date BETWEEN :start AND :end ORDER BY date DESC, id DESC")
    fun observeBetween(start: LocalDate, end: LocalDate): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    /** Todas as compras no cartão (a fatura de cada mês é calculada no domínio). */
    @Query("SELECT * FROM transactions WHERE cardId IS NOT NULL ORDER BY date DESC, id DESC")
    fun observeCardPurchases(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE cardId = :cardId")
    suspend fun getCardPurchases(cardId: Long): List<TransactionEntity>

    /** Parcelas de uma mesma compra. */
    @Query("SELECT * FROM transactions WHERE installmentGroupId = :groupId ORDER BY installmentNumber")
    suspend fun getInstallmentGroup(groupId: String): List<TransactionEntity>

    /**
     * Movimentos pagos que mexem no saldo de cada conta, numa consulta só
     * (uma atualização por gravação). Compras no cartão ficam de fora: só o
     * pagamento da fatura sai da conta. O sinal de cada tipo fica em `MovementKind`.
     */
    @Query(
        """
        SELECT accountId, type AS kind, SUM(amountCents) AS totalCents FROM transactions
        WHERE paid = 1 AND accountId IS NOT NULL AND type IN ('INCOME', 'EXPENSE')
        GROUP BY accountId, type
        UNION ALL
        SELECT accountId, 'TRANSFER_OUT' AS kind, SUM(amountCents) AS totalCents FROM transactions
        WHERE paid = 1 AND type = 'TRANSFER'
        GROUP BY accountId
        UNION ALL
        SELECT destinationAccountId AS accountId, 'TRANSFER_IN' AS kind, SUM(amountCents) AS totalCents FROM transactions
        WHERE paid = 1 AND type = 'TRANSFER'
        GROUP BY destinationAccountId
        UNION ALL
        SELECT accountId, 'INVOICE_PAYMENT' AS kind, SUM(amountCents) AS totalCents FROM invoice_payments
        GROUP BY accountId
        """,
    )
    fun observePaidMovements(): Flow<List<AccountMovement>>

    @Insert
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert
    suspend fun insertAll(transactions: List<TransactionEntity>): List<Long>

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transactions: List<TransactionEntity>)
}
