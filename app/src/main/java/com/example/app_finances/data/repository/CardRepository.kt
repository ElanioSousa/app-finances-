package com.example.app_finances.data.repository

import com.example.app_finances.data.local.dao.CreditCardDao
import com.example.app_finances.data.local.dao.InvoicePaymentDao
import com.example.app_finances.data.local.dao.TransactionDao
import com.example.app_finances.data.local.entity.CreditCardEntity
import com.example.app_finances.data.local.entity.InvoicePaymentEntity
import com.example.app_finances.data.local.entity.TransactionEntity
import com.example.app_finances.domain.CardRules
import com.example.app_finances.domain.CardSummary
import com.example.app_finances.domain.InvoiceCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate

/** Cartões com tudo o que é preciso para montar faturas e limite. */
data class CardData(
    val cards: List<CreditCardEntity> = emptyList(),
    val purchases: List<TransactionEntity> = emptyList(),
    val payments: List<InvoicePaymentEntity> = emptyList(),
) {
    /** Fatura atual, limite e status de cada cartão em [today]. */
    fun summaries(today: LocalDate): List<CardSummary> = CardSummary.ofAll(cards, purchases, payments, today)
}

class CardRepository(
    private val cardDao: CreditCardDao,
    private val transactionDao: TransactionDao,
    private val invoicePaymentDao: InvoicePaymentDao,
) {
    fun observeCards(): Flow<List<CreditCardEntity>> = cardDao.observeActive()

    fun observeCardData(): Flow<CardData> = combine(
        cardDao.observeActive(),
        transactionDao.observeCardPurchases(),
        invoicePaymentDao.observeAll(),
        ::CardData,
    )

    suspend fun add(card: CreditCardEntity): Long {
        CardRules.requireNoViolations(CardRules.cardViolations(card))
        return cardDao.insert(card)
    }

    suspend fun update(card: CreditCardEntity) {
        CardRules.requireNoViolations(CardRules.cardViolations(card))
        cardDao.update(card)
    }

    /** Registra o pagamento conferindo, com os dados atuais do banco, quanto falta pagar da fatura. */
    suspend fun payInvoice(payment: InvoicePaymentEntity) {
        val card = requireNotNull(cardDao.getById(payment.cardId)) { "Cartão não encontrado." }
        val purchases = transactionDao.getCardPurchases(card.id)
        val payments = invoicePaymentDao.getByCard(card.id)
        val invoice = InvoiceCalculator.invoice(card, payment.referenceMonth, purchases, payments)
        CardRules.requireNoViolations(CardRules.paymentViolations(payment, invoice.remainingCents))
        invoicePaymentDao.insert(payment)
    }
}
