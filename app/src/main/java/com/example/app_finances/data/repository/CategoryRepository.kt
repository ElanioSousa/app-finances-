package com.example.app_finances.data.repository

import com.example.app_finances.data.local.dao.CategoryDao
import com.example.app_finances.data.local.entity.CategoryEntity
import com.example.app_finances.data.local.entity.CategoryKind
import com.example.app_finances.domain.requireName
import kotlinx.coroutines.flow.Flow

class CategoryRepository(private val categoryDao: CategoryDao) {

    fun observeActive(): Flow<List<CategoryEntity>> = categoryDao.observeActive()

    fun observeActiveByKind(kind: CategoryKind): Flow<List<CategoryEntity>> =
        categoryDao.observeActiveByKind(kind)

    suspend fun add(category: CategoryEntity): Long {
        requireName(category.name, "Informe o nome da categoria.")
        return categoryDao.insert(category)
    }

    suspend fun update(category: CategoryEntity) {
        requireName(category.name, "Informe o nome da categoria.")
        categoryDao.update(category)
    }
}
