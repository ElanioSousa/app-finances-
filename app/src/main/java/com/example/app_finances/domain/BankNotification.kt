package com.example.app_finances.domain

import com.example.app_finances.data.local.entity.CreditCardEntity

/** Compra no cartão entendida de uma notificação: onde, quanto e uma pista de qual cartão. */
data class CardPurchaseNotice(val merchant: String, val amountCents: Long, val cardHint: String) {

    /** Chave para não sugerir duas vezes a mesma compra (o Nubank e o Google Carteira avisam juntos). */
    val key: String get() = "${normalize(merchant)}|$amountCents"

    /** Texto para o lançamento rápido: "lifestyleacai 27,00 Nubank" (cartão só se for um dos cadastrados). */
    fun quickText(cards: List<CreditCardEntity>): String {
        val card = cards.firstOrNull { normalize(cardHint).contains(normalize(it.name)) }
        val amount = "${amountCents / 100},${(amountCents % 100).toString().padStart(2, '0')}"
        return listOfNotNull(merchant.lowercase(), amount, card?.name).joinToString(" ")
    }
}

/**
 * Lê as notificações de compra no crédito de apps conhecidos. Só estes formatos; qualquer
 * outra notificação (inclusive desses apps) é ignorada.
 */
object BankNotification {
    const val NUBANK = "com.nu.production"
    const val GOOGLE_WALLET = "com.google.android.apps.walletnfcrel"

    val packages: Set<String> = setOf(NUBANK, GOOGLE_WALLET)

    /** Nubank: "Compra de R$ 27,00 APROVADA em LIFESTYLEACAI para o cartão com final 1157." */
    private val NUBANK_CREDIT = Regex("""Compra de R\$\s?([\d.]+,\d{2}) APROVADA em (.+?) para o cart[aã]o""", RegexOption.IGNORE_CASE)

    /** Google Carteira: título "Nubank Mastercard Gold", texto "LIFESTYLEACAI BRL 27.00". */
    private val WALLET_BRL = Regex("""^(.+?)\s+BRL\s+([\d,]*\d\.\d{2})$""")

    fun parse(packageName: String, title: String?, text: String?): CardPurchaseNotice? {
        val body = text?.trim().orEmpty()
        return when (packageName) {
            NUBANK -> NUBANK_CREDIT.find(body)?.let { m ->
                parseCents(m.groupValues[1])?.let { CardPurchaseNotice(m.groupValues[2].trim(), it, cardHint = "Nubank") }
            }
            // "1,234.56" (padrão americano): sem as vírgulas de milhar, o parseCents entende.
            GOOGLE_WALLET -> WALLET_BRL.find(body)?.let { m ->
                parseCents(m.groupValues[2].replace(",", ""))?.let { CardPurchaseNotice(m.groupValues[1].trim(), it, cardHint = title.orEmpty()) }
            }
            else -> null
        }
    }
}
