package com.example.app_finances.feature.recurring

import com.example.app_finances.core.model.Catalog
import com.example.app_finances.data.local.entity.Frequency
import com.example.app_finances.data.local.entity.RecurringEntity
import com.example.app_finances.data.local.entity.TransactionType
import com.example.app_finances.core.model.PaymentOrigin
import java.time.LocalDate

/** Recorrência sendo criada ou editada no diálogo. */
data class RecurringEditor(
    val original: RecurringEntity? = null,
    val type: TransactionType = TransactionType.EXPENSE,
    val amountCents: Long = 0,
    val description: String = "",
    val categoryId: Long? = null,
    val origin: PaymentOrigin = PaymentOrigin.ACCOUNT,
    val accountId: Long? = null,
    val cardId: Long? = null,
    val frequency: Frequency = Frequency.MONTHLY,
    val startDate: LocalDate,
    val isSubscription: Boolean = false,
) {
    val isCard: Boolean get() = type == TransactionType.EXPENSE && origin == PaymentOrigin.CARD

    /** Trocar entre receita e despesa limpa a categoria; receita só cai em conta. */
    fun withType(newType: TransactionType): RecurringEditor =
        if (newType == type) this else copy(type = newType, categoryId = null, origin = PaymentOrigin.ACCOUNT)

    fun toEntity(): RecurringEntity {
        val base = original ?: RecurringEntity(description = "", amountCents = 0, type = type, frequency = frequency, startDate = startDate)
        return base.copy(
            description = description.trim(),
            amountCents = amountCents,
            type = type,
            categoryId = categoryId,
            accountId = accountId.takeUnless { isCard },
            cardId = cardId.takeIf { isCard },
            frequency = frequency,
            startDate = startDate,
            isSubscription = isSubscription,
        )
    }

    companion object {
        fun new(catalog: Catalog, today: LocalDate) = RecurringEditor(
            accountId = catalog.accounts.firstOrNull()?.id,
            cardId = catalog.cards.firstOrNull()?.id,
            startDate = today,
        )

        fun from(r: RecurringEntity) = RecurringEditor(
            original = r,
            type = r.type,
            amountCents = r.amountCents,
            description = r.description,
            categoryId = r.categoryId,
            origin = PaymentOrigin.of(r.cardId),
            accountId = r.accountId,
            cardId = r.cardId,
            frequency = r.frequency,
            startDate = r.startDate,
            isSubscription = r.isSubscription,
        )
    }
}
