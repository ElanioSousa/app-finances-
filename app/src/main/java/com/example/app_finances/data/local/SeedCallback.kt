package com.example.app_finances.data.local

import android.database.sqlite.SQLiteDatabase.CONFLICT_ABORT
import androidx.core.content.contentValuesOf
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.app_finances.data.local.entity.AccountType
import com.example.app_finances.data.local.entity.CategoryKind

/** Dados com que o app já nasce: uma carteira e as categorias mais comuns. */
object SeedCallback : RoomDatabase.Callback() {

    private data class DefaultCategory(val name: String, val kind: CategoryKind, val iconKey: String, val color: Long)

    private val defaultCategories = listOf(
        DefaultCategory("Alimentação", CategoryKind.EXPENSE, "food", 0xFFFB923C),
        DefaultCategory("Moradia", CategoryKind.EXPENSE, "home", 0xFF60A5FA),
        DefaultCategory("Transporte", CategoryKind.EXPENSE, "transport", 0xFFFBBF24),
        DefaultCategory("Saúde", CategoryKind.EXPENSE, "health", 0xFFF87171),
        DefaultCategory("Educação", CategoryKind.EXPENSE, "education", 0xFFA78BFA),
        DefaultCategory("Lazer", CategoryKind.EXPENSE, "leisure", 0xFFF472B6),
        DefaultCategory("Compras", CategoryKind.EXPENSE, "shopping", 0xFF2DD4BF),
        DefaultCategory("Assinaturas", CategoryKind.EXPENSE, "subscriptions", 0xFF8B5CF6),
        DefaultCategory("Contas e serviços", CategoryKind.EXPENSE, "bills", 0xFF0EA5E9),
        DefaultCategory("Outras despesas", CategoryKind.EXPENSE, "other", 0xFF94A3B8),
        DefaultCategory("Salário", CategoryKind.INCOME, "salary", 0xFF34D399),
        DefaultCategory("Freelance", CategoryKind.INCOME, "freelance", 0xFF2DD4BF),
        DefaultCategory("Investimentos", CategoryKind.INCOME, "investments", 0xFF60A5FA),
        DefaultCategory("Presentes", CategoryKind.INCOME, "gift", 0xFFF472B6),
        DefaultCategory("Outras receitas", CategoryKind.INCOME, "other", 0xFF94A3B8),
    )

    override fun onCreate(db: SupportSQLiteDatabase) {
        db.insert("accounts", CONFLICT_ABORT, contentValuesOf(
            "name" to "Carteira",
            "type" to AccountType.CASH.name,
            "initialBalanceCents" to 0L,
            "colorArgb" to 0xFF34D399.toInt(),
            "archived" to false,
        ))
        defaultCategories.forEach { category ->
            db.insert("categories", CONFLICT_ABORT, contentValuesOf(
                "name" to category.name,
                "kind" to category.kind.name,
                "iconKey" to category.iconKey,
                "colorArgb" to category.color.toInt(),
                "archived" to false,
            ))
        }
    }
}
