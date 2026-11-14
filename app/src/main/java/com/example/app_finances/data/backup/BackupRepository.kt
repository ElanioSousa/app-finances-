package com.example.app_finances.data.backup

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import com.example.app_finances.core.model.Catalog
import com.example.app_finances.core.model.toItems
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Backup completo (JSON) e exportação de lançamentos (CSV) em arquivos que o
 * usuário escolhe (Downloads, Drive…). Falhas viram mensagens legíveis.
 */
class BackupRepository(
    private val backupDao: BackupDao,
    private val contentResolver: ContentResolver,
) {
    suspend fun exportBackup(uri: Uri) = write(uri, backupJson())

    /**
     * Backup automático: o arquivo do dia em [folder] (o mesmo, se rodar duas vezes no dia)
     * e só os [AutoBackupFiles.KEEP] mais recentes ficam.
     */
    suspend fun exportToFolder(folder: Uri, date: LocalDate) {
        // Lista a pasta antes de montar o backup: sem acesso a ela, falha logo.
        val children = file(READ_ERROR) { childrenOf(folder) }
        val name = AutoBackupFiles.nameFor(date)
        val document = children[name] ?: file(WRITE_ERROR) {
            DocumentsContract.createDocument(contentResolver, folder.asDocument(), JSON_TYPE, name)
        }
        write(document, backupJson())
        file(WRITE_ERROR) { deleteOld(children - name, keepingToday = name) }
    }

    /** Apaga os backups automáticos antigos de [children] (o de hoje conta entre os que ficam). */
    private fun deleteOld(children: Map<String, Uri>, keepingToday: String) {
        AutoBackupFiles.toDelete(children.keys + keepingToday)
            .mapNotNull(children::get)
            .forEach { DocumentsContract.deleteDocument(contentResolver, it) }
    }

    /** Guarda o acesso à pasta escolhida (vale depois de reiniciar o celular) e devolve o nome dela. */
    suspend fun keepFolderAccess(folder: Uri): String = file(READ_ERROR) {
        contentResolver.takePersistableUriPermission(folder, FOLDER_ACCESS)
        displayName(folder.asDocument()) ?: folder.lastPathSegment.orEmpty()
    }

    fun releaseFolderAccess(folder: Uri) {
        runCatching { contentResolver.releasePersistableUriPermission(folder, FOLDER_ACCESS) }
    }

    private suspend fun backupJson(): String = withContext(Dispatchers.Default) {
        val file = BackupFile(exportedAt = LocalDateTime.now().toString(), content = backupDao.snapshot())
        BackupFile.json.encodeToString(BackupFile.serializer(), file)
    }

    private fun Uri.asDocument(): Uri = DocumentsContract.buildDocumentUriUsingTree(this, DocumentsContract.getTreeDocumentId(this))

    /** Arquivos da pasta: nome → endereço do documento. */
    private fun childrenOf(folder: Uri): Map<String, Uri> {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(folder, DocumentsContract.getDocumentId(folder.asDocument()))
        val columns = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        return contentResolver.query(children, columns, null, null, null)?.use { cursor ->
            buildMap {
                while (cursor.moveToNext()) put(cursor.getString(1), DocumentsContract.buildDocumentUriUsingTree(folder, cursor.getString(0)))
            }
        }.orEmpty()
    }

    private fun displayName(document: Uri): String? =
        contentResolver.query(document, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)
            ?.use { if (it.moveToFirst()) it.getString(0) else null }

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
        const val JSON_TYPE = "application/json"
        const val FOLDER_ACCESS = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    }
}
