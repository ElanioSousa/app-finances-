package com.example.app_finances.data.backup

import com.example.app_finances.core.model.TransactionItem
import com.example.app_finances.data.local.entity.TransactionType
import kotlin.math.absoluteValue

/**
 * Lançamentos em CSV para planilhas em português: separador ";", vírgula decimal,
 * despesas negativas e texto entre aspas quando precisa.
 */
object TransactionsCsv {
    private val HEADER = listOf("Data", "Tipo", "Descrição", "Categoria", "Conta/Cartão", "Valor", "Situação", "Parcela", "Observação")

    fun build(items: List<TransactionItem>): String = buildString {
        appendLine(HEADER.joinToString(";"))
        items.forEach { appendLine(row(it).joinToString(";") { cell -> quote(cell) }) }
    }

    private fun row(item: TransactionItem): List<String> {
        val t = item.transaction
        val origin = when (t.type) {
            TransactionType.TRANSFER -> "${item.accountName.orEmpty()} → ${item.destinationName.orEmpty()}"
            else -> item.cardName ?: item.accountName.orEmpty()
        }
        return listOf(
            t.date.toString(),
            typeLabel(t.type),
            t.description,
            item.category?.name.orEmpty(),
            origin,
            decimal(if (t.type == TransactionType.EXPENSE) -t.amountCents else t.amountCents),
            if (t.paid) "Pago" else "Pendente",
            item.installmentLabel.orEmpty(),
            t.notes.orEmpty(),
        )
    }

    private fun typeLabel(type: TransactionType) = when (type) {
        TransactionType.INCOME -> "Receita"
        TransactionType.EXPENSE -> "Despesa"
        TransactionType.TRANSFER -> "Transferência"
    }

    /** -12345 → "-123,45" (sem separador de milhar, que confunde planilhas). */
    internal fun decimal(cents: Long): String {
        val sign = if (cents < 0) "-" else ""
        val abs = cents.absoluteValue
        return "$sign${abs / 100},${(abs % 100).toString().padStart(2, '0')}"
    }

    private fun quote(cell: String): String =
        if (cell.any { it == ';' || it == '"' || it == '\n' }) "\"${cell.replace("\"", "\"\"")}\"" else cell
}
