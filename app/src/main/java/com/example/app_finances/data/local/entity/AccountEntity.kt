package com.example.app_finances.data.local.entity

import androidx.room.Entity
import kotlinx.serialization.Serializable
import androidx.room.PrimaryKey

/** Conta corrente, poupança, carteira etc. O saldo atual é calculado, não guardado. */
@Entity(tableName = "accounts")
@Serializable
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: AccountType,
    val initialBalanceCents: Long = 0,
    val colorArgb: Int,
    val archived: Boolean = false,
)
