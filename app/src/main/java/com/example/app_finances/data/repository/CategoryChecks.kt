package com.example.app_finances.data.repository

import com.example.app_finances.data.local.dao.CategoryDao
import com.example.app_finances.data.local.entity.TransactionType
import com.example.app_finances.domain.TransactionRules

/** Confere no banco se a categoria escolhida é do tipo certo (receita/despesa). */
internal suspend fun CategoryDao.requireKindMatches(type: TransactionType, categoryId: Long?) {
    val kind = categoryId?.let { getById(it)?.kind }
    TransactionRules.categoryViolation(type, kind)?.let { throw IllegalArgumentException(it) }
}
