package com.example.app_finances.data.repository

import com.example.app_finances.data.local.dao.CategoryDao
import com.example.app_finances.data.local.dao.RecurringDao
import com.example.app_finances.data.local.entity.RecurringEntity
import com.example.app_finances.domain.RecurrenceScheduler
import com.example.app_finances.domain.RecurringRules
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Recorrências e a geração dos seus lançamentos.
 * Mudar, pausar ou excluir só mexe no futuro: o que já aconteceu fica como histórico.
 */
class RecurringRepository(
    private val recurringDao: RecurringDao,
    private val categoryDao: CategoryDao,
    private val today: () -> LocalDate,
) {
    fun observeAll(): Flow<List<RecurringEntity>> = recurringDao.observeAll()

    suspend fun add(recurring: RecurringEntity) {
        requireValid(recurring)
        val created = recurring.copy(generatedUntil = RecurrenceScheduler.initialCursor(recurring.startDate, today()))
        val id = recurringDao.insert(created)
        generate(created.copy(id = id))
    }

    /** Ocorrências futuras ainda não realizadas são refeitas com os novos dados. */
    suspend fun update(recurring: RecurringEntity) {
        requireValid(recurring)
        replaceFuture(recurring)
    }

    /** Pausar apaga o que estava agendado; retomar volta a gerar a partir de hoje (sem lançar meses atrasados). */
    suspend fun setActive(recurring: RecurringEntity, active: Boolean) = replaceFuture(recurring.copy(active = active))

    suspend fun delete(recurring: RecurringEntity) {
        recurringDao.deleteFutureOccurrences(recurring.id, today())
        recurringDao.delete(recurring)
    }

    /** Gera o que falta de todas as recorrências ativas (repetir não duplica nem grava à toa). */
    suspend fun generateDue() {
        recurringDao.getActive().forEach { generate(it) }
    }

    private suspend fun replaceFuture(recurring: RecurringEntity) {
        recurringDao.deleteFutureOccurrences(recurring.id, today())
        val updated = recurring.copy(generatedUntil = RecurrenceScheduler.restartCursor(recurring.startDate, today()))
        recurringDao.update(updated)
        generate(updated)
    }

    private suspend fun generate(recurring: RecurringEntity) {
        val horizon = RecurrenceScheduler.horizon(today())
        val alreadyDone = recurring.generatedUntil?.let { it >= horizon } ?: false
        if (!recurring.active || alreadyDone) return
        recurringDao.saveOccurrences(recurring.id, RecurrenceScheduler.pending(recurring, horizon), horizon)
    }

    private suspend fun requireValid(recurring: RecurringEntity) {
        RecurringRules.requireValid(recurring)
        categoryDao.requireKindMatches(recurring.type, recurring.categoryId)
    }
}
