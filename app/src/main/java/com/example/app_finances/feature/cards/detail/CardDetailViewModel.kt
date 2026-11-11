package com.example.app_finances.feature.cards.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_finances.core.designsystem.component.Option
import com.example.app_finances.core.model.Catalog
import com.example.app_finances.core.model.CatalogSources
import com.example.app_finances.core.model.TransactionItem
import com.example.app_finances.core.model.observeCatalog
import com.example.app_finances.core.model.toItems
import com.example.app_finances.core.model.toOptions
import com.example.app_finances.core.ui.EditorController
import com.example.app_finances.core.ui.UserMessages
import com.example.app_finances.core.ui.Editing
import com.example.app_finances.core.ui.WhileUiSubscribed
import com.example.app_finances.data.local.entity.InvoicePaymentEntity
import com.example.app_finances.data.repository.CardData
import com.example.app_finances.data.repository.CardRepository
import com.example.app_finances.domain.CardSummary
import com.example.app_finances.domain.Invoice
import com.example.app_finances.domain.InvoiceCalculator
import com.example.app_finances.domain.InvoiceStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.YearMonth

/** Pagamento sendo preenchido no diálogo. */
data class PayEditor(
    val amountCents: Long,
    val accountId: Long?,
    val date: LocalDate,
)

/** Pagamento já feito, com o nome da conta usada. */
data class PaymentLine(val payment: InvoicePaymentEntity, val accountName: String)

/** Cartão carregado com a fatura escolhida. */
data class CardDetail(
    val summary: CardSummary,
    val invoice: Invoice,
    val invoiceStatus: InvoiceStatus,
    val purchases: List<TransactionItem>,
    val payments: List<PaymentLine>,
    val accountOptions: List<Option<Long>>,
)

data class CardDetailUiState(
    val detail: CardDetail? = null,
    val pay: Editing<PayEditor>? = null,
)

class CardDetailViewModel(
    private val cardId: Long,
    private val cardRepository: CardRepository,
    catalogSources: CatalogSources,
    private val today: () -> LocalDate,
) : ViewModel() {

    /** Fatura escolhida; nula = a fatura atual do cartão. */
    private val month = MutableStateFlow<YearMonth?>(null)
    private val pay = EditorController<PayEditor>(viewModelScope)
    private val messages = UserMessages(viewModelScope)

    /** Falhas ao excluir um pagamento, para a tela mostrar num aviso. */
    val errorFlow: Flow<String> = messages.flow

    /** Só refeito quando os dados ou o mês mudam (não a cada tecla no pagamento). */
    private val detail: Flow<CardDetail?> =
        combine(cardRepository.observeCardData(), observeCatalog(catalogSources), month, ::buildDetail)

    val state: StateFlow<CardDetailUiState> = combine(detail, pay.state, ::CardDetailUiState)
        .stateIn(viewModelScope, WhileUiSubscribed, CardDetailUiState())

    fun setMonth(value: YearMonth) = month.update { value }

    /** Abre o pagamento já com o que falta pagar e a conta padrão do cartão. */
    fun openPay() {
        val detail = state.value.detail ?: return
        val account = detail.summary.card.paymentAccountId ?: detail.accountOptions.firstOrNull()?.value
        pay.open(PayEditor(amountCents = detail.invoice.remainingCents, accountId = account, date = today()))
    }

    fun deletePayment(payment: InvoicePaymentEntity) = messages.launch { cardRepository.deletePayment(payment) }

    fun editPay(transform: (PayEditor) -> PayEditor) = pay.edit(transform)

    fun closePay() = pay.close()

    fun confirmPay() {
        val month = state.value.detail?.invoice?.month ?: return
        pay.save { editor ->
            val accountId = requireNotNull(editor.accountId) { "Escolha a conta que paga a fatura." }
            cardRepository.payInvoice(InvoicePaymentEntity(cardId = cardId, referenceMonth = month, amountCents = editor.amountCents, date = editor.date, accountId = accountId))
        }
    }

    private fun buildDetail(data: CardData, catalog: Catalog, chosenMonth: YearMonth?): CardDetail? {
        val card = data.cards.firstOrNull { it.id == cardId } ?: return null
        val purchases = data.purchases.filter { it.cardId == cardId }
        val payments = data.payments.filter { it.cardId == cardId }
        val summary = CardSummary.of(card, purchases, payments, today())
        val invoice = chosenMonth?.let { InvoiceCalculator.invoice(card, it, purchases, payments) } ?: summary.current
        val accountNames = catalog.accounts.associate { it.id to it.name }
        return CardDetail(
            summary = summary,
            invoice = invoice,
            invoiceStatus = invoice.status(today()),
            purchases = invoice.purchases.toItems(catalog),
            payments = payments.filter { it.referenceMonth == invoice.month }.map { PaymentLine(it, accountNames[it.accountId].orEmpty()) },
            accountOptions = catalog.accounts.toOptions(),
        )
    }
}
