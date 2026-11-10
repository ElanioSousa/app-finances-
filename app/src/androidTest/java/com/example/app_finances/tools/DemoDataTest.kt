package com.example.app_finances.tools

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.app_finances.TestApp
import com.example.app_finances.data.local.entity.CategoryKind
import com.example.app_finances.data.local.entity.CreditCardEntity
import com.example.app_finances.data.local.entity.Frequency
import com.example.app_finances.data.local.entity.RecurringEntity
import com.example.app_finances.data.local.entity.TransactionEntity
import com.example.app_finances.data.local.entity.TransactionType
import com.example.app_finances.domain.Installments
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.YearMonth

/**
 * Ferramenta, não teste: preenche o banco REAL do app com dados de demonstração
 * (6 meses de lançamentos, cartão, recorrências) para capturas de tela.
 * Só roda com o argumento `demo=true`; nos testes normais é pulada. Rode esta classe
 * sozinha (outros testes trocam o banco do app por um em memória) e com o app limpo:
 *   adb shell pm clear com.example.app_finances
 *   adb shell am instrument -w -e demo true -e class com.example.app_finances.tools.DemoDataTest \
 *     com.example.app_finances.test/androidx.test.runner.AndroidJUnitRunner
 */
@RunWith(AndroidJUnit4::class)
class DemoDataTest {

    @Test
    fun preencheDadosDeDemonstracao() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("demo") == "true")
        val c = TestApp.context.container
        val today = LocalDate.now()
        val categories = c.categoryRepository.observeActive().first().associateBy { it.name }
        val wallet = c.accountRepository.observeAccounts().first().first().id
        // Não duplica: se já há lançamentos, o banco já foi preenchido.
        val month = YearMonth.from(today)
        assumeTrue(c.transactionRepository.observeMonths(month.minusYears(1), month).first().isEmpty())
        fun cat(name: String) = categories.getValue(name).id
        fun tx(type: TransactionType, cents: Long, date: LocalDate, description: String, category: String) =
            TransactionEntity(type = type, amountCents = cents, date = date, description = description, categoryId = cat(category), accountId = wallet)

        (5 downTo 0).forEach { back ->
            val month = today.minusMonths(back.toLong()).withDayOfMonth(1)
            val factor = 1 + (back % 3) * 0.12
            c.transactionRepository.addAll(
                listOfNotNull(
                    tx(TransactionType.INCOME, 650_000, month.plusDays(4), "Salário", "Salário").takeIf { back > 0 },
                    tx(TransactionType.EXPENSE, 180_000, month.plusDays(9), "Aluguel", "Moradia").takeIf { back > 0 },
                    tx(TransactionType.EXPENSE, (82_000 * factor).toLong(), month.plusDays(2), "Mercado", "Alimentação"),
                    tx(TransactionType.EXPENSE, (24_000 * factor).toLong(), month.plusDays(6), "Combustível", "Transporte"),
                    tx(TransactionType.EXPENSE, 15_990, month.plusDays(8), "Farmácia", "Saúde").takeIf { back % 2 == 0 },
                    tx(TransactionType.INCOME, 120_000, month.plusDays(12), "Projeto", "Freelance").takeIf { back % 2 == 1 },
                    tx(TransactionType.EXPENSE, (31_000 * factor).toLong(), month.plusDays(14), "Cinema e jantar", "Lazer"),
                ).filter { it.date <= today },
            )
        }
        val card = c.cardRepository.add(CreditCardEntity(name = "Nubank", limitCents = 800_000, closingDay = 5, dueDay = 12, paymentAccountId = wallet, colorArgb = 0xFF8B5CF6.toInt()))
        val notebook = TransactionEntity(type = TransactionType.EXPENSE, amountCents = 359_880, date = today.minusDays(3), description = "Notebook", categoryId = cat("Compras"), cardId = card)
        c.transactionRepository.addAll(Installments.schedule(notebook, 10) { "demo-notebook" })
        listOf(
            RecurringEntity(description = "Salário", amountCents = 650_000, type = TransactionType.INCOME, categoryId = cat("Salário"), accountId = wallet, frequency = Frequency.MONTHLY, startDate = today.withDayOfMonth(5)),
            RecurringEntity(description = "Aluguel", amountCents = 180_000, type = TransactionType.EXPENSE, categoryId = cat("Moradia"), accountId = wallet, frequency = Frequency.MONTHLY, startDate = today.plusDays(2)),
            RecurringEntity(description = "Internet", amountCents = 11_990, type = TransactionType.EXPENSE, categoryId = cat("Contas e serviços"), accountId = wallet, frequency = Frequency.MONTHLY, startDate = today.plusDays(5)),
            RecurringEntity(description = "Streaming", amountCents = 3_990, type = TransactionType.EXPENSE, categoryId = cat("Assinaturas"), cardId = card, frequency = Frequency.MONTHLY, startDate = today.minusDays(1), isSubscription = true),
        ).forEach { c.recurringRepository.add(it) }
        check(categories.values.any { it.kind == CategoryKind.EXPENSE })
    }
}
