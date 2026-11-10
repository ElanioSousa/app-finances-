package com.example.app_finances.data.backup

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.app_finances.data.local.entity.AccountEntity
import com.example.app_finances.data.local.entity.CategoryEntity
import com.example.app_finances.data.local.entity.CreditCardEntity
import com.example.app_finances.data.local.entity.InvoicePaymentEntity
import com.example.app_finances.data.local.entity.RecurringEntity
import com.example.app_finances.data.local.entity.TransactionEntity

/** Lê e substitui o banco inteiro, para backup e restauração. */
@Dao
abstract class BackupDao {
    @Query("SELECT * FROM accounts") abstract suspend fun accounts(): List<AccountEntity>
    @Query("SELECT * FROM categories") abstract suspend fun categories(): List<CategoryEntity>
    @Query("SELECT * FROM credit_cards") abstract suspend fun cards(): List<CreditCardEntity>
    @Query("SELECT * FROM recurrings") abstract suspend fun recurrings(): List<RecurringEntity>
    @Query("SELECT * FROM transactions") abstract suspend fun transactions(): List<TransactionEntity>
    @Query("SELECT * FROM invoice_payments") abstract suspend fun invoicePayments(): List<InvoicePaymentEntity>

    /** Uma leitura consistente de tudo (nada muda no meio). */
    @Transaction
    open suspend fun snapshot() = BackupContent(accounts(), categories(), cards(), recurrings(), transactions(), invoicePayments())

    /**
     * Troca todos os dados pelos do backup numa transação só: se qualquer linha
     * falhar (ex.: referência quebrada), nada muda.
     */
    @Transaction
    open suspend fun replaceAll(content: BackupContent) {
        deleteInvoicePayments(); deleteTransactions(); deleteRecurrings(); deleteCards(); deleteCategories(); deleteAccounts()
        insertAccounts(content.accounts)
        insertCategories(content.categories)
        insertCards(content.cards)
        insertRecurrings(content.recurrings)
        insertTransactions(content.transactions)
        insertInvoicePayments(content.invoicePayments)
    }

    @Query("DELETE FROM invoice_payments") protected abstract suspend fun deleteInvoicePayments()
    @Query("DELETE FROM transactions") protected abstract suspend fun deleteTransactions()
    @Query("DELETE FROM recurrings") protected abstract suspend fun deleteRecurrings()
    @Query("DELETE FROM credit_cards") protected abstract suspend fun deleteCards()
    @Query("DELETE FROM categories") protected abstract suspend fun deleteCategories()
    @Query("DELETE FROM accounts") protected abstract suspend fun deleteAccounts()

    @Insert protected abstract suspend fun insertAccounts(list: List<AccountEntity>)
    @Insert protected abstract suspend fun insertCategories(list: List<CategoryEntity>)
    @Insert protected abstract suspend fun insertCards(list: List<CreditCardEntity>)
    @Insert protected abstract suspend fun insertRecurrings(list: List<RecurringEntity>)
    @Insert protected abstract suspend fun insertTransactions(list: List<TransactionEntity>)
    @Insert protected abstract suspend fun insertInvoicePayments(list: List<InvoicePaymentEntity>)
}
