package com.example.app_finances.data.repository

import com.example.app_finances.data.local.dao.CategoryDao
import com.example.app_finances.data.local.entity.CategoryEntity
import com.example.app_finances.domain.CategoryRules
import kotlinx.coroutines.flow.Flow

class CategoryRepository(private val categoryDao: CategoryDao) {

    fun observeActive(): Flow<List<CategoryEntity>> = categoryDao.observeActive()

    fun observeAll(): Flow<List<CategoryEntity>> = categoryDao.observeAll()

    suspend fun add(category: CategoryEntity): Long {
        CategoryRules.requireValid(category)
        return categoryDao.insert(category)
    }

    suspend fun update(category: CategoryEntity) {
        CategoryRules.requireValid(category)
        categoryDao.update(category)
    }

    /** Arquivar some com a categoria das listas; os lançamentos antigos continuam com ela. */
    suspend fun archive(category: CategoryEntity) = categoryDao.update(category.copy(archived = true))
}
