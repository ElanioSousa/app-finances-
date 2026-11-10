package com.example.app_finances.domain

import com.example.app_finances.data.local.entity.RecurringEntity
import com.example.app_finances.data.local.entity.TransactionType

/**
 * Uma recorrência é válida se o lançamento que ela gera for válido
 * (mesmas regras de valor, conta/cartão e categoria) e não for transferência.
 */
object RecurringRules {

    fun violations(recurring: RecurringEntity): List<String> = buildList {
        if (recurring.type == TransactionType.TRANSFER) add("Recorrências são receitas ou despesas.")
        val endDate = recurring.endDate
        if (endDate != null && endDate < recurring.startDate) add("O fim não pode ser antes do início.")
        addAll(TransactionRules.violations(RecurrenceScheduler.toTransaction(recurring, recurring.startDate)))
    }

    fun requireValid(recurring: RecurringEntity) {
        val problems = violations(recurring)
        require(problems.isEmpty()) { problems.joinToString(" ") }
    }
}
