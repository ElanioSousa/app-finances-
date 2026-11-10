package com.example.app_finances.data

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.app_finances.data.local.AppDatabase
import com.example.app_finances.data.local.SeedCallback
import com.example.app_finances.data.local.entity.AccountEntity
import com.example.app_finances.data.local.entity.AccountType
import com.example.app_finances.data.local.entity.CategoryKind
import com.example.app_finances.data.local.entity.CreditCardEntity
import com.example.app_finances.data.local.entity.Frequency
import com.example.app_finances.data.local.entity.InvoicePaymentEntity
import com.example.app_finances.data.local.entity.RecurringEntity
import com.example.app_finances.data.local.entity.TransactionEntity
import com.example.app_finances.data.local.entity.TransactionType
import com.example.app_finances.data.repository.AccountRepository
import com.example.app_finances.data.repository.CardRepository
import com.example.app_finances.domain.Installments
import com.example.app_finances.data.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class DatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var accounts: AccountRepository
    private lateinit var transactions: TransactionRepository
    private val nov10: LocalDate = LocalDate.of(2026, 11, 10)

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .addCallback(SeedCallback)
            .build()
        accounts = AccountRepository(db.accountDao(), db.transactionDao())
        transactions = TransactionRepository(db.transactionDao(), db.categoryDao())
    }

    @After
    fun tearDown() = db.close()

    private fun expense(amount: Long, date: LocalDate = nov10, accountId: Long? = WALLET) =
        TransactionEntity(type = TransactionType.EXPENSE, amountCents = amount, date = date, description = "Despesa", accountId = accountId)

    /** Executa [block] e devolve a exceção que ele lançou (falha se não lançar nenhuma). */
    private suspend fun failureOf(block: suspend () -> Unit): Throwable =
        runCatching { block() }.exceptionOrNull() ?: throw AssertionError("Era esperado um erro")

    @Test
    fun bancoNasceComCarteiraECategorias() = runTest {
        val balances = accounts.observeBalances().first()
        assertEquals(listOf("Carteira"), balances.map { it.account.name })
        assertEquals(10, db.categoryDao().observeActiveByKind(CategoryKind.EXPENSE).first().size)
        assertEquals(5, db.categoryDao().observeActiveByKind(CategoryKind.INCOME).first().size)
    }

    @Test
    fun saldoConsideraReceitasDespesasPendentesCartaoTransferenciaEFatura() = runTest {
        val bank = accounts.add(AccountEntity(name = "Banco", type = AccountType.CHECKING, initialBalanceCents = 100_000, colorArgb = 0))
        val card = db.creditCardDao().insert(CreditCardEntity(name = "Cartão", limitCents = 500_000, closingDay = 5, dueDay = 12, colorArgb = 0))

        transactions.add(expense(30_000, accountId = bank))
        transactions.add(expense(5_000, accountId = bank).copy(paid = false))
        transactions.add(expense(20_000, accountId = null).copy(cardId = card))
        transactions.add(
            TransactionEntity(type = TransactionType.TRANSFER, amountCents = 10_000, date = nov10, description = "Saque", accountId = bank, destinationAccountId = WALLET),
        )
        db.invoicePaymentDao().insert(InvoicePaymentEntity(cardId = card, referenceMonth = YearMonth.of(2026, 11), amountCents = 20_000, date = nov10, accountId = bank))

        val byName = accounts.observeBalances().first().associate { it.account.name to it.balanceCents }
        // Banco: 1.000 − 300 (despesa paga) − 100 (transferência) − 200 (fatura); pendente e compra no cartão não contam.
        assertEquals(mapOf("Banco" to 40_000L, "Carteira" to 10_000L), byName)
    }

    @Test
    fun totaisDoMesRespeitamOsLimitesDoMes() = runTest {
        transactions.add(expense(1_000, date = LocalDate.of(2026, 10, 31)))
        transactions.add(expense(2_000, date = LocalDate.of(2026, 11, 1)))
        transactions.add(expense(3_000, date = LocalDate.of(2026, 11, 30)))
        transactions.add(expense(4_000, date = LocalDate.of(2026, 12, 1)))

        assertEquals(5_000, transactions.observeMonthTotals(YearMonth.of(2026, 11)).first().expenseCents)
    }

    @Test
    fun repositorioRecusaLancamentoInvalido() = runTest {
        assertTrue(failureOf { transactions.add(expense(0)) } is IllegalArgumentException)
    }

    @Test
    fun repositorioRecusaCategoriaDeDespesaEmReceita() = runTest {
        val expenseCategory = db.categoryDao().observeActiveByKind(CategoryKind.EXPENSE).first().first().id
        val income = expense(1_000).copy(type = TransactionType.INCOME, categoryId = expenseCategory)
        assertTrue(failureOf { transactions.add(income) } is IllegalArgumentException)
        transactions.add(expense(1_000).copy(categoryId = expenseCategory))
    }

    @Test
    fun recorrenciaNaoGeraDoisLancamentosNoMesmoDia() = runTest {
        val recurring = db.recurringDao().insert(
            RecurringEntity(description = "Aluguel", amountCents = 150_000, type = TransactionType.EXPENSE, accountId = WALLET, frequency = Frequency.MONTHLY, startDate = nov10),
        )
        transactions.add(expense(150_000).copy(recurringId = recurring))
        assertTrue(failureOf { transactions.add(expense(150_000).copy(recurringId = recurring)) } is SQLiteConstraintException)
    }

    @Test
    fun contaComLancamentosNaoPodeSerApagada() = runTest {
        transactions.add(expense(1_000))
        assertThrows(SQLiteConstraintException::class.java) {
            db.openHelper.writableDatabase.execSQL("DELETE FROM accounts WHERE id = $WALLET")
        }
    }

    @Test
    fun excluirParcelaExcluiACompraInteiraEDesfazRestaura() = runTest {
        val card = db.creditCardDao().insert(CreditCardEntity(name = "Cartão", limitCents = 500_000, closingDay = 5, dueDay = 12, colorArgb = 0))
        val purchase = expense(30_000, accountId = null).copy(cardId = card)
        transactions.addAll(Installments.schedule(purchase, 3) { "grupo" })
        val parcels = db.transactionDao().getInstallmentGroup("grupo")
        assertEquals(3, parcels.size)

        val deleted = transactions.delete(parcels[1])
        assertEquals(3, deleted.size)
        assertEquals(0, db.transactionDao().getInstallmentGroup("grupo").size)

        transactions.addAll(deleted)
        assertEquals(parcels, db.transactionDao().getInstallmentGroup("grupo"))
    }

    @Test
    fun pagamentoDeFaturaConfereRestanteESaiDaConta() = runTest {
        val cards = CardRepository(db.creditCardDao(), db.transactionDao(), db.invoicePaymentDao())
        val card = cards.add(CreditCardEntity(name = "Cartão", limitCents = 500_000, closingDay = 5, dueDay = 12, colorArgb = 0))
        transactions.add(expense(20_000, date = LocalDate.of(2026, 11, 1), accountId = null).copy(cardId = card))
        val month = YearMonth.of(2026, 11)
        fun payment(cents: Long) = InvoicePaymentEntity(cardId = card, referenceMonth = month, amountCents = cents, date = nov10, accountId = WALLET)

        assertTrue(failureOf { cards.payInvoice(payment(20_001)) } is IllegalArgumentException)
        cards.payInvoice(payment(15_000))
        cards.payInvoice(payment(5_000))
        assertTrue(failureOf { cards.payInvoice(payment(1)) } is IllegalArgumentException)
        assertEquals(-20_000L, accounts.observeBalances().first().single().balanceCents)
    }

    private companion object {
        const val WALLET = 1L
    }
}
