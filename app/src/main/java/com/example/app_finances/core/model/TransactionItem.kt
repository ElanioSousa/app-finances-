package com.example.app_finances.core.model

import com.example.app_finances.data.local.entity.AccountEntity
import com.example.app_finances.data.local.entity.CategoryEntity
import com.example.app_finances.data.local.entity.TransactionEntity
import com.example.app_finances.data.local.entity.TransactionType
import com.example.app_finances.data.repository.AccountRepository
import com.example.app_finances.data.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Lançamento pronto para exibir: já com categoria e nomes das contas. */
data class TransactionItem(
    val transaction: TransactionEntity,
    val category: CategoryEntity?,
    val accountName: String?,
    val destinationName: String?,
) {
    val id: Long get() = transaction.id

    /** Texto auxiliar da linha: "Alimentação · Carteira" ou "Carteira → Banco". */
    val subtitle: String
        get() = when (transaction.type) {
            TransactionType.TRANSFER -> "${accountName.orEmpty()} → ${destinationName.orEmpty()}"
            else -> listOfNotNull(category?.name, accountName).joinToString(" · ")
        }
}

/** Categorias e contas ativas, usadas para descrever, filtrar e preencher formulários. */
data class Catalog(
    val categories: List<CategoryEntity> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
)

/** Só cadastros (sem saldos): não refaz o cálculo de saldo a cada lançamento gravado. */
fun observeCatalog(categories: CategoryRepository, accounts: AccountRepository): Flow<Catalog> =
    combine(categories.observeActive(), accounts.observeAccounts(), ::Catalog)

/** Junta cada lançamento à sua categoria e às contas envolvidas. */
fun List<TransactionEntity>.toItems(catalog: Catalog): List<TransactionItem> {
    val categoryById = catalog.categories.associateBy { it.id }
    val accountName = catalog.accounts.associate { it.id to it.name }
    return map { t ->
        TransactionItem(
            transaction = t,
            category = t.categoryId?.let(categoryById::get),
            accountName = t.accountId?.let(accountName::get),
            destinationName = t.destinationAccountId?.let(accountName::get),
        )
    }
}
