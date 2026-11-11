package com.example.app_finances.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.app_finances.data.local.entity.InvoicePaymentEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface InvoicePaymentDao {
    @Query("SELECT * FROM invoice_payments ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<InvoicePaymentEntity>>

    @Query("SELECT * FROM invoice_payments WHERE cardId = :cardId")
    suspend fun getByCard(cardId: Long): List<InvoicePaymentEntity>

    @Query("SELECT * FROM invoice_payments WHERE date BETWEEN :from AND :to ORDER BY date DESC, id DESC")
    fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<InvoicePaymentEntity>>

    @Insert
    suspend fun insert(payment: InvoicePaymentEntity): Long

    @Delete
    suspend fun delete(payment: InvoicePaymentEntity)
}
