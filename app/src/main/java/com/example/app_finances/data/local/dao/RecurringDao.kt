package com.example.app_finances.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.app_finances.data.local.entity.RecurringEntity
import com.example.app_finances.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
abstract class RecurringDao {
    @Query("SELECT * FROM recurrings ORDER BY active DESC, description")
    abstract fun observeAll(): Flow<List<RecurringEntity>>

    @Query("SELECT * FROM recurrings WHERE active = 1")
    abstract suspend fun getActive(): List<RecurringEntity>

    @Insert
    abstract suspend fun insert(recurring: RecurringEntity): Long

    @Update
    abstract suspend fun update(recurring: RecurringEntity)

    @Delete
    abstract suspend fun delete(recurring: RecurringEntity)

    /** Ignora ocorrência já existente (mesma recorrência e data): gerar de novo nunca duplica. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertOccurrences(transactions: List<TransactionEntity>)

    @Query("UPDATE recurrings SET generatedUntil = :until WHERE id = :id")
    protected abstract suspend fun setGeneratedUntil(id: Long, until: LocalDate)

    /**
     * Apaga ocorrências futuras que ainda não aconteceram de fato: pendentes na conta
     * ou agendadas no cartão. As passadas e as já pagas ficam como histórico.
     * (Espelha `RecurrenceScheduler.toTransaction`: na conta nasce pendente, no cartão já "paga".)
     */
    @Query("DELETE FROM transactions WHERE recurringId = :id AND date > :today AND (paid = 0 OR cardId IS NOT NULL)")
    abstract suspend fun deleteFutureOccurrences(id: Long, today: LocalDate)

    /** Grava as ocorrências e marca até onde foi, numa transação só. */
    @Transaction
    open suspend fun saveOccurrences(id: Long, occurrences: List<TransactionEntity>, until: LocalDate) {
        insertOccurrences(occurrences)
        setGeneratedUntil(id, until)
    }
}
