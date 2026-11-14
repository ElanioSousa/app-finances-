package com.example.app_finances.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.app_finances.data.local.entity.GoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    /** Com prazo primeiro (o mais próximo antes), depois as sem prazo, por nome. */
    @Query("SELECT * FROM goals ORDER BY deadline IS NULL, deadline, name")
    fun observeAll(): Flow<List<GoalEntity>>

    @Insert
    suspend fun insert(goal: GoalEntity): Long

    @Update
    suspend fun update(goal: GoalEntity)

    @Delete
    suspend fun delete(goal: GoalEntity)

    /** Guarda (positivo) ou retira (negativo) do valor da meta, sem ficar abaixo de zero. */
    @Query("UPDATE goals SET savedCents = MAX(0, savedCents + :deltaCents) WHERE id = :id")
    suspend fun addToSaved(id: Long, deltaCents: Long)
}
