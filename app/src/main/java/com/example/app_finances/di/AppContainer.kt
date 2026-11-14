package com.example.app_finances.di

import android.content.Context
import java.time.LocalDate
import com.example.app_finances.core.model.CatalogSources
import com.example.app_finances.data.backup.BackupRepository
import com.example.app_finances.data.local.AppDatabase
import com.example.app_finances.data.settings.SettingsRepository
import com.example.app_finances.data.repository.AccountRepository
import com.example.app_finances.data.repository.CardRepository
import com.example.app_finances.data.repository.CategoryRepository
import com.example.app_finances.data.repository.GoalRepository
import com.example.app_finances.data.repository.RecurringRepository
import com.example.app_finances.data.repository.TransactionRepository

/**
 * Injeção de dependência manual: um único lugar cria o banco e os repositórios.
 * O banco fica privado — as telas só falam com repositórios, que validam tudo.
 * Tudo é preguiçoso: o banco só abre quando alguma tela pede dados.
 * Testes passam outro [createDatabase] (ex.: banco em memória).
 */
class AppContainer(
    context: Context,
    createDatabase: (Context) -> AppDatabase = AppDatabase::create,
    /** "Hoje" para as regras que dependem da data (faturas); testes podem fixar. */
    val today: () -> LocalDate = LocalDate::now,
) {
    private val appContext: Context = context.applicationContext
    private val database: AppDatabase by lazy { createDatabase(appContext) }

    val accountRepository by lazy { AccountRepository(database.accountDao(), database.transactionDao(), today) }
    val categoryRepository by lazy { CategoryRepository(database.categoryDao()) }
    val transactionRepository by lazy { TransactionRepository(database.transactionDao(), database.categoryDao(), database.creditCardDao()) }
    val cardRepository by lazy {
        CardRepository(database.creditCardDao(), database.transactionDao(), database.invoicePaymentDao())
    }

    val recurringRepository by lazy { RecurringRepository(database.recurringDao(), database.categoryDao(), today) }

    val goalRepository by lazy { GoalRepository(database.goalDao()) }
    val backupRepository by lazy { BackupRepository(database.backupDao(), appContext.contentResolver) }
    val settingsRepository by lazy { SettingsRepository(appContext) }

    /** De onde as telas tiram categorias, contas e cartões. */
    val catalogSources by lazy { CatalogSources(categoryRepository, accountRepository, cardRepository) }
}
