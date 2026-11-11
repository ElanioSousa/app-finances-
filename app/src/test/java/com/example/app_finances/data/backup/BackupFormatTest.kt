package com.example.app_finances.data.backup

import com.example.app_finances.core.model.Catalog
import com.example.app_finances.core.model.toItems
import com.example.app_finances.data.local.entity.CategoryEntity
import com.example.app_finances.data.local.entity.CategoryKind
import com.example.app_finances.data.local.entity.Frequency
import com.example.app_finances.data.local.entity.InvoicePaymentEntity
import com.example.app_finances.data.local.entity.RecurringEntity
import com.example.app_finances.data.local.entity.TransactionType
import com.example.app_finances.domain.account
import com.example.app_finances.domain.transaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class BackupFormatTest {

    private val content = BackupContent(
        accounts = listOf(account(1, initial = 1_000)),
        categories = listOf(CategoryEntity(id = 3, name = "Mercado", kind = CategoryKind.EXPENSE, iconKey = "food", colorArgb = -1, monthlyBudgetCents = 50_000)),
        recurrings = listOf(RecurringEntity(id = 2, description = "Aluguel", amountCents = 150_000, type = TransactionType.EXPENSE, accountId = 1, frequency = Frequency.MONTHLY, startDate = LocalDate.of(2026, 1, 31), generatedUntil = LocalDate.of(2026, 11, 30))),
        transactions = listOf(transaction(amount = 12_345).copy(id = 9, categoryId = 3, notes = "feira")),
        invoicePayments = listOf(InvoicePaymentEntity(id = 4, cardId = 7, referenceMonth = YearMonth.of(2026, 11), amountCents = 100, date = LocalDate.of(2026, 11, 10), accountId = 1)),
    )

    @Test
    fun `ida e volta preserva tudo, inclusive datas e ids`() {
        val text = BackupFile.json.encodeToString(BackupFile.serializer(), BackupFile(exportedAt = "2026-11-10T10:00", content = content))
        assertTrue(text.contains("\"2026-01-31\"") && text.contains("\"2026-11\""))
        assertEquals(content, BackupFile.parse(text).content)
    }

    /** Backup gravado pela versão 1: mudanças nas entidades não podem impedir de restaurá-lo. */
    @Test
    fun `backups da versão 1 continuam legíveis`() {
        val text = javaClass.getResource("/backup-v1.json")!!.readText()
        assertEquals(content, BackupFile.parse(text).content)
    }

    @Test
    fun `recusa arquivo que não é backup ou é de versão mais nova`() {
        assertEquals("Este arquivo não é um backup válido do app.", runCatching { BackupFile.parse("{ nada") }.exceptionOrNull()?.message)
        assertTrue(runCatching { BackupFile.parse("""{"format":"outro","version":1,"exportedAt":"x","content":{}}""") }.isFailure)
        assertTrue(runCatching { BackupFile.parse("""{"format":"financas-backup","version":99,"exportedAt":"x","content":{}}""") }.isFailure)
    }

    @Test
    fun `csv em formato de planilha brasileira`() {
        val catalog = Catalog(content.categories, content.accounts)
        val items = listOf(
            transaction(amount = 12_345).copy(categoryId = 3, notes = "feira; hortifruti"),
            transaction(TransactionType.INCOME, 100_000).copy(paid = false, description = "Salário \"novembro\""),
        ).toItems(catalog)
        val lines = TransactionsCsv.build(items).trim().lines()
        assertEquals("Data;Tipo;Descrição;Categoria;Conta/Cartão;Valor;Situação;Parcela;Observação", lines[0])
        assertEquals("2026-11-10;Despesa;Teste;Mercado;Conta 1;-123,45;Pago;;\"feira; hortifruti\"", lines[1])
        assertEquals("2026-11-10;Receita;\"Salário \"\"novembro\"\"\";;Conta 1;1000,00;Pendente;;", lines[2])
        assertEquals("0,05", TransactionsCsv.decimal(5))
    }
}
