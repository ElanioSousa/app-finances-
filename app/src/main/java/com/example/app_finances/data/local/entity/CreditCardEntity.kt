package com.example.app_finances.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Cartão de crédito. A fatura não é guardada: é calculada a partir das compras
 * e de [closingDay]/[dueDay] (dias 1–31; meses mais curtos usam o último dia).
 */
@Entity(
    tableName = "credit_cards",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["paymentAccountId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("paymentAccountId")],
)
data class CreditCardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val brand: String? = null,
    val limitCents: Long,
    val closingDay: Int,
    val dueDay: Int,
    val paymentAccountId: Long? = null,
    val colorArgb: Int,
    val archived: Boolean = false,
)
