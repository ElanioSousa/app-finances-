package com.example.app_finances.domain

import com.example.app_finances.data.local.entity.CreditCardEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BankNotificationTest {

    private val cards = listOf(
        CreditCardEntity(id = 1, name = "Nubank", limitCents = 0, closingDay = 1, dueDay = 7, colorArgb = 0),
        CreditCardEntity(id = 2, name = "Mercado Pago", limitCents = 0, closingDay = 1, dueDay = 7, colorArgb = 0),
    )

    @Test
    fun nubankCompraNoCredito() {
        val notice = BankNotification.parse(
            BankNotification.NUBANK,
            "Compra no crédito aprovada",
            "Compra de R$ 27,00 APROVADA em LIFESTYLEACAI para o cartão com final 1157.",
        )
        assertEquals(CardPurchaseNotice("LIFESTYLEACAI", 2_700, "Nubank"), notice)
        assertEquals("lifestyleacai 27,00 Nubank", notice?.quickText(cards))
    }

    @Test
    fun nubankValorComMilhar() {
        val notice = BankNotification.parse(BankNotification.NUBANK, null, "Compra de R$ 1.234,56 APROVADA em LOJA X para o cartão com final 1157.")
        assertEquals(123_456L, notice?.amountCents)
        assertEquals("LOJA X", notice?.merchant)
    }

    @Test
    fun googleCarteiraUsaOTituloParaAcharOCartao() {
        val notice = BankNotification.parse(BankNotification.GOOGLE_WALLET, "Nubank Mastercard Gold", "LIFESTYLEACAI BRL 27.00")
        assertEquals("lifestyleacai 27,00 Nubank", notice?.quickText(cards))
        // As duas notificações da mesma compra viram uma sugestão só.
        val nubank = BankNotification.parse(BankNotification.NUBANK, null, "Compra de R$ 27,00 APROVADA em LIFESTYLEACAI para o cartão com final 1157.")
        assertEquals(nubank?.key, notice?.key)
    }

    @Test
    fun cartaoQueNaoEstaCadastradoFicaDeFora() {
        val notice = BankNotification.parse(BankNotification.GOOGLE_WALLET, "Visa Infinite", "PADARIA BRL 1,050.90")
        assertEquals("padaria 1050,90", notice?.quickText(cards))
    }

    @Test
    fun ignoraOutrasNotificacoesEOutrosApps() {
        assertNull(BankNotification.parse(BankNotification.NUBANK, "Pix recebido", "Você recebeu R$ 50,00 de Fulano."))
        assertNull(BankNotification.parse("com.whatsapp", "Compra", "Compra de R$ 27,00 APROVADA em X para o cartão com final 1."))
        assertNull(BankNotification.parse(BankNotification.GOOGLE_WALLET, "Cartão adicionado", "Seu cartão está pronto"))
    }
}
