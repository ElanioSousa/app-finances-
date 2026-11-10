package com.example.app_finances.core.ui

import androidx.annotation.StringRes
import com.example.app_finances.R
import com.example.app_finances.core.designsystem.component.AmountTone
import com.example.app_finances.data.local.entity.TransactionType

/** Textos e cores de cada tipo de lançamento, num lugar só. */
private data class TypeUi(
    @param:StringRes val label: Int,
    @param:StringRes val plural: Int,
    @param:StringRes val paidLabel: Int,
    val tone: AmountTone,
)

private fun TransactionType.ui(): TypeUi = when (this) {
    TransactionType.EXPENSE -> TypeUi(R.string.type_expense, R.string.filter_expenses, R.string.form_paid_expense, AmountTone.Expense)
    TransactionType.INCOME -> TypeUi(R.string.type_income, R.string.filter_incomes, R.string.form_paid_income, AmountTone.Income)
    TransactionType.TRANSFER -> TypeUi(R.string.type_transfer, R.string.filter_transfers, R.string.form_paid_transfer, AmountTone.Neutral)
}

/** "Despesa", "Receita", "Transferência". */
val TransactionType.labelRes: Int get() = ui().label

/** "Despesas", "Receitas", "Transferências" (filtros). */
val TransactionType.pluralLabelRes: Int get() = ui().plural

/** "Já foi pago" / "Já foi recebido" / "Já foi feita". */
val TransactionType.paidLabelRes: Int get() = ui().paidLabel

val TransactionType.tone: AmountTone get() = ui().tone

/** Ordem de exibição: despesa primeiro, por ser o lançamento mais comum. */
val TransactionTypesInDisplayOrder: List<TransactionType> =
    listOf(TransactionType.EXPENSE, TransactionType.INCOME, TransactionType.TRANSFER)
