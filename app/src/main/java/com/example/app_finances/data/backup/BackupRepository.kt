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
    suspend fun exportBackup(uri: Uri) {
        val file = BackupFile(exportedAt = LocalDateTime.now().toString(), content = backupDao.snapshot())
        write(uri, BackupFile.json.encodeToString(BackupFile.serializer(), file))
    }

    /** Substitui TODOS os dados pelos do arquivo (a tela confirma antes). */
    suspend fun restoreBackup(uri: Uri) {
        val file = BackupFile.parse(read(uri))
        backupDao.replaceAll(file.content)
    }

    suspend fun exportCsv(uri: Uri) {
        val content = backupDao.snapshot()
        val catalog = Catalog(content.categories, content.accounts, content.cards)
        val items = content.transactions.sortedWith(compareBy({ it.date }, { it.id })).toItems(catalog)
        write(uri, TransactionsCsv.build(items))
    }

    private suspend fun write(uri: Uri, text: String) = withContext(Dispatchers.IO) {
        try {
            contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { it.write(text) }
                ?: throw IllegalArgumentException(WRITE_ERROR)
        } catch (e: IOException) {
            throw IllegalArgumentException(WRITE_ERROR, e)
        }
    }

    private suspend fun read(uri: Uri): String = withContext(Dispatchers.IO) {
        try {
            contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: throw IllegalArgumentException(READ_ERROR)
        } catch (e: IOException) {
            throw IllegalArgumentException(READ_ERROR, e)
        }
    }

    private companion object {
        const val WRITE_ERROR = "Não foi possível salvar o arquivo."
        const val READ_ERROR = "Não foi possível ler o arquivo."
    }
}
