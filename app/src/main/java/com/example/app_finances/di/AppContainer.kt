package com.example.app_finances.di

import android.content.Context
import com.example.app_finances.data.local.AppDatabase
import com.example.app_finances.data.repository.AccountRepository
import com.example.app_finances.data.repository.CategoryRepository
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
) {
    private val database: AppDatabase by lazy { createDatabase(context.applicationContext) }

    val accountRepository by lazy { AccountRepository(database.accountDao(), database.transactionDao()) }
    val categoryRepository by lazy { CategoryRepository(database.categoryDao()) }
    val transactionRepository by lazy { TransactionRepository(database.transactionDao(), database.categoryDao()) }
}
