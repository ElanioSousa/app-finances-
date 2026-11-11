@file:UseSerializers(LocalDateSerializer::class, YearMonthSerializer::class)

package com.example.app_finances.data.local.entity

import androidx.room.Entity
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import com.example.app_finances.data.local.LocalDateSerializer
import com.example.app_finances.data.local.YearMonthSerializer
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.YearMonth

/**
 * Pagamento (total ou parcial) da fatura de [referenceMonth] de um cartão.
 * É o que tira dinheiro da conta: as compras do cartão já contam como despesa
 * do mês, então o pagamento não vira outro lançamento de despesa (evita contar duas vezes).
 */
@Entity(
    tableName = "invoice_payments",
    foreignKeys = [
        ForeignKey(CreditCardEntity::class, ["id"], ["cardId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(AccountEntity::class, ["id"], ["accountId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index("cardId"), Index("accountId")],
)
@Serializable
data class InvoicePaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardId: Long,
    val referenceMonth: YearMonth,
    val amountCents: Long,
    val date: LocalDate,
    val accountId: Long,
)
