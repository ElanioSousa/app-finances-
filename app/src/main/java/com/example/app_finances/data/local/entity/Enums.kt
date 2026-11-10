package com.example.app_finances.data.local.entity

/** Tipo de conta onde o dinheiro está. */
enum class AccountType { CHECKING, SAVINGS, CASH, INVESTMENT }

/** Uma categoria classifica só receitas ou só despesas. */
enum class CategoryKind { INCOME, EXPENSE }

/**
 * Natureza de um lançamento. O valor é sempre positivo; o tipo decide se ele
 * entra ou sai. Transferência sai de `accountId` e entra em `destinationAccountId`.
 */
enum class TransactionType { INCOME, EXPENSE, TRANSFER }

/** Com que frequência uma recorrência/assinatura se repete. */
enum class Frequency { WEEKLY, MONTHLY, YEARLY }
