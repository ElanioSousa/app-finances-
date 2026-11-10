package com.example.app_finances.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.app_finances.data.local.entity.InvoicePaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoicePaymentDao {
    @Query("SELECT * FROM invoice_payments ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<InvoicePaymentEntity>>

    @Query("SELECT * FROM invoice_payments WHERE cardId = :cardId")
    suspend fun getByCard(cardId: Long): List<InvoicePaymentEntity>

    @Insert
    suspend fun insert(payment: InvoicePaymentEntity): Long
}
