package com.example.app_finances.domain

import com.example.app_finances.data.local.entity.TransactionEntity

/** Parcelamento de compras no cartão. */
object Installments {
    const val MAX = 24

    /** Divide em [count] parcelas; os centavos que sobram vão na primeira. */
    fun split(totalCents: Long, count: Int): List<Long> {
        require(count in 1..MAX) { "Parcelas devem ficar entre 1 e $MAX." }
        val base = totalCents / count
        val remainder = totalCents - base * count
        return List(count) { index -> if (index == 0) base + remainder else base }
    }

    /**
     * Gera um lançamento por parcela, um mês após o outro, todos ligados pelo mesmo grupo.
     * [purchase] traz o valor total; cada parcela recebe sua fração.
     */
    fun schedule(purchase: TransactionEntity, count: Int, newGroupId: () -> String): List<TransactionEntity> {
        if (count == 1) return listOf(purchase)
        val groupId = newGroupId()
        return split(purchase.amountCents, count).mapIndexed { index, cents ->
            purchase.copy(
                amountCents = cents,
                date = purchase.date.plusMonths(index.toLong()),
                installmentGroupId = groupId,
                installmentNumber = index + 1,
                installmentCount = count,
            )
        }
    }
}
