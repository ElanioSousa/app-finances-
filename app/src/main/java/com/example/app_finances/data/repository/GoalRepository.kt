package com.example.app_finances.data.repository

import com.example.app_finances.data.local.dao.GoalDao
import com.example.app_finances.data.local.entity.GoalEntity
import com.example.app_finances.domain.requireName
import kotlinx.coroutines.flow.Flow

class GoalRepository(private val goalDao: GoalDao) {

    fun observeAll(): Flow<List<GoalEntity>> = goalDao.observeAll()

    suspend fun add(goal: GoalEntity): Long {
        validate(goal)
        return goalDao.insert(goal)
    }

    suspend fun update(goal: GoalEntity) {
        validate(goal)
        goalDao.update(goal)
    }

    suspend fun delete(goal: GoalEntity) = goalDao.delete(goal)

    /** Guarda (positivo) ou retira (negativo) [deltaCents] da meta. */
    suspend fun move(goal: GoalEntity, deltaCents: Long) {
        require(deltaCents != 0L) { "Informe o valor." }
        goalDao.addToSaved(goal.id, deltaCents)
    }

    private fun validate(goal: GoalEntity) {
        requireName(goal.name, "Informe o nome da meta.")
        require(goal.targetCents > 0) { "Informe quanto quer juntar." }
    }
}
