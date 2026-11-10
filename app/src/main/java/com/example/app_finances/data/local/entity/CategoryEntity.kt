package com.example.app_finances.data.local.entity

import androidx.room.Entity
import kotlinx.serialization.Serializable
import androidx.room.Index
import androidx.room.PrimaryKey

/** [iconKey] é um nome estável (ex.: "food") que a interface traduz para um ícone. */
@Entity(tableName = "categories", indices = [Index("kind")])
@Serializable
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kind: CategoryKind,
    val iconKey: String,
    val colorArgb: Int,
    val archived: Boolean = false,
    /** Teto de gasto por mês (só despesas); nulo = sem orçamento. */
    val monthlyBudgetCents: Long? = null,
)
