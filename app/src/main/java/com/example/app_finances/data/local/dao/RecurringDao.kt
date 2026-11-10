package com.example.app_finances.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.app_finances.data.local.entity.RecurringEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringDao {
    @Query("SELECT * FROM recurrings ORDER BY active DESC, description")
    fun observeAll(): Flow<List<RecurringEntity>>

    @Query("SELECT * FROM recurrings WHERE active = 1")
    suspend fun getActive(): List<RecurringEntity>

    @Insert
    suspend fun insert(recurring: RecurringEntity): Long

    @Update
    suspend fun update(recurring: RecurringEntity)
}
