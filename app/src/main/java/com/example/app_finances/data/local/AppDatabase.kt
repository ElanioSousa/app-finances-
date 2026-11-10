package com.example.app_finances.data.local

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.app_finances.data.local.dao.AccountDao
import com.example.app_finances.data.local.dao.CategoryDao
import com.example.app_finances.data.local.dao.CreditCardDao
import com.example.app_finances.data.local.dao.InvoicePaymentDao
import com.example.app_finances.data.local.dao.RecurringDao
import com.example.app_finances.data.local.dao.TransactionDao
import com.example.app_finances.data.local.entity.AccountEntity
import com.example.app_finances.data.local.entity.CategoryEntity
import com.example.app_finances.data.local.entity.CreditCardEntity
import com.example.app_finances.data.local.entity.InvoicePaymentEntity
import com.example.app_finances.data.local.entity.RecurringEntity
import com.example.app_finances.data.local.entity.TransactionEntity

/**
 * Banco local do app. Mudanças de esquema exigem subir [version] e escrever uma
 * migração; os esquemas exportados ficam em `app/schemas` para servir de base.
 */
@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        CreditCardEntity::class,
        RecurringEntity::class,
        TransactionEntity::class,
        InvoicePaymentEntity::class,
    ],
    version = 3,
    exportSchema = true,
    autoMigrations = [
        // v2: recorrências guardam até onde já geraram lançamentos.
        AutoMigration(from = 1, to = 2),
        // v3: categorias de despesa podem ter orçamento mensal.
        AutoMigration(from = 2, to = 3),
    ],
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun creditCardDao(): CreditCardDao
    abstract fun recurringDao(): RecurringDao
    abstract fun transactionDao(): TransactionDao
    abstract fun invoicePaymentDao(): InvoicePaymentDao

    companion object {
        private const val FILE_NAME = "financas.db"

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, FILE_NAME)
                .addCallback(SeedCallback)
                .build()
    }
}
