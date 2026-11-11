package com.example.app_finances.data.backup

import android.content.ContentResolver
import android.net.Uri
import com.example.app_finances.core.model.Catalog
import com.example.app_finances.core.model.toItems
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDateTime

/**
 * Backup completo (JSON) e exportação de lançamentos (CSV) em arquivos que o
 * usuário escolhe (Downloads, Drive…). Falhas viram mensagens legíveis.
 */
class BackupRepository(
    private val backupDao: BackupDao,
    private val contentResolver: ContentResolver,
) {
    suspend fun exportBackup(uri: Uri) = withContext(Dispatchers.Default) {
        val file = BackupFile(exportedAt = LocalDateTime.now().toString(), content = backupDao.snapshot())
        write(uri, BackupFile.json.encodeToString(BackupFile.serializer(), file))
    }

    /** Substitui TODOS os dados pelos do arquivo (a tela confirma antes). */
    suspend fun restoreBackup(uri: Uri) = withContext(Dispatchers.Default) {
        val file = BackupFile.parse(read(uri))
        backupDao.replaceAll(file.content)
    }

    suspend fun exportCsv(uri: Uri) = withContext(Dispatchers.Default) {
        val content = backupDao.snapshot()
        val catalog = Catalog(content.categories, content.accounts, content.cards)
        val items = content.transactions.sortedWith(compareBy({ it.date }, { it.id })).toItems(catalog)
        write(uri, TransactionsCsv.build(items))
    }

    private suspend fun write(uri: Uri, text: String) = file(WRITE_ERROR) {
        contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { it.write(text) }
    }

    private suspend fun read(uri: Uri): String = file(READ_ERROR) {
        contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
    }

    /** Acesso a arquivo fora da thread da tela; falha (ou arquivo indisponível) vira [message]. */
    private suspend fun <T : Any> file(message: String, block: () -> T?): T = withContext(Dispatchers.IO) {
        try {
            block() ?: throw IllegalArgumentException(message)
        } catch (e: IOException) {
            throw IllegalArgumentException(message, e)
        }
    }

    private companion object {
        const val WRITE_ERROR = "Não foi possível salvar o arquivo."
        const val READ_ERROR = "Não foi possível ler o arquivo."
    }
}
