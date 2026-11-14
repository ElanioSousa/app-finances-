package com.example.app_finances.data.backup

import com.example.app_finances.data.local.entity.AccountEntity
import com.example.app_finances.data.local.entity.CategoryEntity
import com.example.app_finances.data.local.entity.CreditCardEntity
import com.example.app_finances.data.local.entity.GoalEntity
import com.example.app_finances.data.local.entity.InvoicePaymentEntity
import com.example.app_finances.data.local.entity.RecurringEntity
import com.example.app_finances.data.local.entity.TransactionEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Todos os dados do app. */
@Serializable
data class BackupContent(
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val cards: List<CreditCardEntity> = emptyList(),
    val recurrings: List<RecurringEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val invoicePayments: List<InvoicePaymentEntity> = emptyList(),
    /** Backups de antes das metas não têm a lista: lê como vazia. */
    val goals: List<GoalEntity> = emptyList(),
)

/** Arquivo de backup: formato versionado para poder evoluir sem quebrar backups antigos. */
@Serializable
data class BackupFile(
    val format: String = FORMAT,
    val version: Int = VERSION,
    val exportedAt: String,
    val content: BackupContent,
) {
    companion object {
        const val FORMAT = "financas-backup"
        const val VERSION = 1

        val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }

        /** Lê e confere o arquivo; lança [IllegalArgumentException] com motivo legível se não servir. */
        fun parse(text: String): BackupFile {
            val file = runCatching { json.decodeFromString(serializer(), text) }
                .getOrElse { throw IllegalArgumentException("Este arquivo não é um backup válido do app.") }
            require(file.format == FORMAT) { "Este arquivo não é um backup do app." }
            require(file.version <= VERSION) { "Backup de uma versão mais nova do app. Atualize o app para restaurar." }
            return file
        }
    }
}
