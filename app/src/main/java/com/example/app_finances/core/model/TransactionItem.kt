package com.example.app_finances.core.model

import com.example.app_finances.core.designsystem.component.Option
import com.example.app_finances.data.local.entity.AccountEntity
import com.example.app_finances.data.local.entity.CategoryEntity
import com.example.app_finances.data.local.entity.CreditCardEntity
import com.example.app_finances.data.local.entity.TransactionEntity
import com.example.app_finances.data.local.entity.TransactionType
import com.example.app_finances.data.repository.AccountRepository
import com.example.app_finances.data.repository.CardRepository
import com.example.app_finances.data.repository.CategoryRepository
import com.example.app_finances.domain.categoryKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Lançamento pronto para exibir: já com categoria e nomes das contas. */
data class TransactionItem(
    val transaction: TransactionEntity,
    val category: CategoryEntity?,
    val accountName: String?,
    val destinationName: String?,
    val cardName: String? = null,
) {
    val id: Long get() = transaction.id

    /** "2/10" para parcelas; nulo para compras à vista. */
    val installmentLabel: String?
        get() = transaction.installmentNumber?.let { "$it/${transaction.installmentCount}" }

    /** Texto auxiliar: "Alimentação · Carteira", "Compras · Nubank · 2/10" ou "Carteira → Banco". */
    val subtitle: String
        get() = when (transaction.type) {
            TransactionType.TRANSFER -> "${accountName.orEmpty()} → ${destinationName.orEmpty()}"
            else -> listOfNotNull(category?.name, accountName ?: cardName, installmentLabel).joinToString(" · ")
        }
}

/** Categorias, contas e cartões ativos, usados para descrever, filtrar e preencher formulários. */
data class Catalog(
    val categories: List<CategoryEntity> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    val cards: List<CreditCardEntity> = emptyList(),
) {
    val categoryById: Map<Long, CategoryEntity> by lazy { categories.associateBy { it.id } }
    val accountNames: Map<Long, String> by lazy { accounts.associate { it.id to it.name } }
    val cardNames: Map<Long, String> by lazy { cards.associate { it.id to it.name } }

    /** Só as categorias que o tipo de lançamento aceita (regra do domínio). */
    fun categoriesFor(type: TransactionType): List<CategoryEntity> = categories.filter { it.kind == type.categoryKind }
}

/** Repositórios de onde vem o [Catalog]. */
data class CatalogSources(
    val categories: CategoryRepository,
    val accounts: AccountRepository,
    val cards: CardRepository,
)

/** Só cadastros (sem saldos): não refaz o cálculo de saldo a cada lançamento gravado. */
fun observeCatalog(sources: CatalogSources): Flow<Catalog> =
    combine(sources.categories.observeActive(), sources.accounts.observeAccounts(), sources.cards.observeCards(), ::Catalog)

/** Junta cada lançamento à sua categoria e às contas envolvidas. */
fun List<TransactionEntity>.toItems(catalog: Catalog): List<TransactionItem> = map { t ->
    TransactionItem(
        transaction = t,
        category = t.categoryId?.let(catalog.categoryById::get),
        accountName = t.accountId?.let(catalog.accountNames::get),
        destinationName = t.destinationAccountId?.let(catalog.accountNames::get),
        cardName = t.cardId?.let(catalog.cardNames::get),
    )
}

/** Contas como opções de escolha (nome visível, id como valor). */
fun List<AccountEntity>.toOptions(): List<Option<Long>> = map { Option(it.id, it.name) }

/** Cartões como opções de escolha. */
@JvmName("cardsToOptions")
fun List<CreditCardEntity>.toOptions(): List<Option<Long>> = map { Option(it.id, it.name) }
