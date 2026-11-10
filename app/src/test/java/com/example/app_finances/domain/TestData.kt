package com.example.app_finances.domain

import com.example.app_finances.data.local.entity.AccountEntity
import com.example.app_finances.data.local.entity.AccountType
import com.example.app_finances.data.local.entity.TransactionEntity
import com.example.app_finances.data.local.entity.TransactionType
import java.time.LocalDate

fun account(id: Long, initial: Long = 0) =
    AccountEntity(id = id, name = "Conta $id", type = AccountType.CHECKING, initialBalanceCents = initial, colorArgb = 0)

fun transaction(
    type: TransactionType = TransactionType.EXPENSE,
    amount: Long = 1_000,
    accountId: Long? = 1,
    date: LocalDate = LocalDate.of(2026, 11, 10),
) = TransactionEntity(type = type, amountCents = amount, date = date, description = "Teste", accountId = accountId)
