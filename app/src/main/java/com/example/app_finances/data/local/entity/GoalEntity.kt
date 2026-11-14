@file:UseSerializers(LocalDateSerializer::class)

package com.example.app_finances.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.app_finances.data.local.LocalDateSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.LocalDate

/** Meta de economia ("Reserva: R$ 5.000 até junho"). O guardado é informado pelo usuário. */
@Entity(tableName = "goals")
@Serializable
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetCents: Long,
    val savedCents: Long = 0,
    /** Até quando quer chegar; nulo = sem prazo. */
    val deadline: LocalDate? = null,
    val colorArgb: Int,
)
