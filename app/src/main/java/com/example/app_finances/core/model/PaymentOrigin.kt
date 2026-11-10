package com.example.app_finances.core.model

/** De onde sai o dinheiro de uma despesa. */
enum class PaymentOrigin {
    ACCOUNT,
    CARD,
    ;

    companion object {
        /** Origem de algo já gravado: tem cartão → cartão; senão, conta. */
        fun of(cardId: Long?): PaymentOrigin = if (cardId != null) CARD else ACCOUNT
    }
}
