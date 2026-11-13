package com.example.app_finances.domain

import com.example.app_finances.data.local.entity.AccountEntity
import com.example.app_finances.data.local.entity.AccountType
import com.example.app_finances.data.local.entity.CreditCardEntity
import com.example.app_finances.data.local.entity.TransactionEntity
import com.example.app_finances.data.local.entity.TransactionType
import java.time.LocalDate

fun account(id: Long, initial: Long = 0) =
    AccountEntity(id = id, name = "Conta $id", type = AccountType.CHECKING, initialBalanceCents = initial, colorArgb = 0)

fun card(id: Long = 1, closing: Int = 5, due: Int = 15) =
    CreditCardEntity(id = id, name = "Cartão $id", limitCents = 500_000, closingDay = closing, dueDay = due, colorArgb = 0)

fun transaction(
    type: TransactionType = TransactionType.EXPENSE,
    amount: Long = 1_000,
    accountId: Long? = 1,
    date: LocalDate = LocalDate.of(2026, 11, 10),
) = TransactionEntity(type = type, amountCents = amount, date = date, description = "Teste", accountId = accountId)
