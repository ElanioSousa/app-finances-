@file:UseSerializers(LocalDateSerializer::class, YearMonthSerializer::class)

package com.example.app_finances.data.local.entity

import androidx.room.Entity
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import com.example.app_finances.data.backup.LocalDateSerializer
import com.example.app_finances.data.backup.YearMonthSerializer
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * Um lançamento: receita, despesa (na conta ou no cartão) ou transferência.
 *
 * - [amountCents] é sempre positivo; [type] decide o sentido.
 * - Compra no cartão: [cardId] preenchido e [accountId] nulo — só afeta a conta
 *   quando a fatura é paga (ver [InvoicePaymentEntity]).
 * - Parcelas de uma mesma compra compartilham [installmentGroupId].
 * - Gerado por recorrência: ([recurringId], [date]) é único, para não duplicar.
 */
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(CategoryEntity::class, ["id"], ["categoryId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(AccountEntity::class, ["id"], ["accountId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(AccountEntity::class, ["id"], ["destinationAccountId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(CreditCardEntity::class, ["id"], ["cardId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(RecurringEntity::class, ["id"], ["recurringId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [
        Index("date"),
        Index("categoryId"),
        Index("accountId"),
        Index("destinationAccountId"),
        Index("cardId"),
        Index("installmentGroupId"),
        Index(value = ["recurringId", "date"], unique = true),
    ],
)
@Serializable
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TransactionType,
    val amountCents: Long,
    val date: LocalDate,
    val description: String,
    val notes: String? = null,
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val destinationAccountId: Long? = null,
    val cardId: Long? = null,
    val paid: Boolean = true,
    val installmentGroupId: String? = null,
    val installmentNumber: Int? = null,
    val installmentCount: Int? = null,
    val recurringId: Long? = null,
)
