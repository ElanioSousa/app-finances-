package com.example.app_finances.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * Modelo de lançamento que se repete (salário, aluguel, assinatura).
 * Os lançamentos reais são gerados a partir dele, um por ocorrência.
 * Usa conta **ou** cartão: [accountId] e [cardId] não podem estar ambos preenchidos.
 */
@Entity(
    tableName = "recurrings",
    foreignKeys = [
        ForeignKey(CategoryEntity::class, ["id"], ["categoryId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(AccountEntity::class, ["id"], ["accountId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(CreditCardEntity::class, ["id"], ["cardId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index("categoryId"), Index("accountId"), Index("cardId")],
)
data class RecurringEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val description: String,
    val amountCents: Long,
    val type: TransactionType,
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val cardId: Long? = null,
    val frequency: Frequency,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val isSubscription: Boolean = false,
    val active: Boolean = true,
    /** Até que dia os lançamentos já foram gerados (os seguintes ainda não existem). */
    val generatedUntil: LocalDate? = null,
)
