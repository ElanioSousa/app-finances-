package com.example.app_finances.domain

import com.example.app_finances.data.local.entity.CategoryKind
import com.example.app_finances.data.local.entity.TransactionEntity
import com.example.app_finances.data.local.entity.TransactionType

/**
 * Invariantes de um lançamento. Rodam antes de qualquer gravação, para o banco
 * nunca guardar algo que bagunce saldos ou faturas.
 */
object TransactionRules {

    /** Lista de problemas encontrados; vazia quando o lançamento é válido. */
    fun violations(t: TransactionEntity): List<String> = buildList {
        if (t.amountCents <= 0) add("O valor deve ser maior que zero.")
        if (t.description.isBlank()) add("Informe uma descrição.")
        addAll(originViolations(t))
        addAll(installmentViolations(t))
    }

    fun requireValid(t: TransactionEntity) {
        val problems = violations(t)
        require(problems.isEmpty()) { problems.joinToString(" ") }
    }

    private fun originViolations(t: TransactionEntity): List<String> = buildList {
        val hasAccount = t.accountId != null
        val hasCard = t.cardId != null
        if (hasAccount == hasCard) add("Escolha uma conta ou um cartão.")
        if (hasCard && t.type != TransactionType.EXPENSE) add("Cartão de crédito só recebe despesas.")
        if (t.type == TransactionType.TRANSFER) {
            if (t.destinationAccountId == null) add("Escolha a conta de destino.")
            if (t.destinationAccountId == t.accountId) add("Origem e destino devem ser contas diferentes.")
        } else if (t.destinationAccountId != null) {
            add("Só transferências têm conta de destino.")
        }
    }

    /**
     * A categoria (quando há) precisa ser do mesmo tipo do lançamento:
     * nada de categoria de despesa numa receita. Transferências não têm categoria.
     */
    fun categoryViolation(type: TransactionType, categoryKind: CategoryKind?): String? = when {
        categoryKind == null -> null
        type == TransactionType.TRANSFER -> "Transferências não têm categoria."
        type.name != categoryKind.name -> "A categoria não combina com o tipo do lançamento."
        else -> null
    }

    private fun installmentViolations(t: TransactionEntity): List<String> {
        val group = t.installmentGroupId
        val number = t.installmentNumber
        val count = t.installmentCount
        if (group == null && number == null && count == null) return emptyList()
        val invalid = group == null || number == null || count == null || count < 2 || number !in 1..count
        return if (invalid) listOf("Parcelamento inválido.") else emptyList()
    }
}
