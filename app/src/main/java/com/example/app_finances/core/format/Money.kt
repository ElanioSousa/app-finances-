package com.example.app_finances.core.format

import kotlin.math.absoluteValue

/**
 * Valores monetários do app são sempre centavos em [Long] — nunca Double,
 * para não haver erro de arredondamento em somas e parcelamentos.
 */
object Money {

    /** Maior valor aceito na digitação: R$ 9.999.999.999,99. */
    const val MAX_INPUT_CENTS = 999_999_999_999L

    /** O que aparece no lugar dos valores quando estão ocultos. */
    const val HIDDEN = "R$ ••••"

    private const val CURRENCY = "R$"
    private const val NBSP = ' '

    /**
     * Formata no padrão brasileiro: `R$ 1.234,56`, `-R$ 10,00`.
     * Com [showPlusSign], positivos ganham `+` (útil para receitas em listas).
     */
    fun format(cents: Long, showPlusSign: Boolean = false): String {
        val sign = when {
            cents < 0 -> "-"
            showPlusSign && cents > 0 -> "+"
            else -> ""
        }
        val abs = cents.absoluteValue
        val reais = groupThousands(abs / 100)
        val centavos = (abs % 100).toString().padStart(2, '0')
        return "$sign$CURRENCY$NBSP$reais,$centavos"
    }

    /**
     * Converte o texto digitado num campo de valor em centavos, no estilo
     * "caixa eletrônico": só os dígitos importam, e cada novo dígito empurra
     * os anteriores para a esquerda (digitar 1, 2, 3 → R$ 1,23).
     */
    fun parseTypedDigits(text: String): Long {
        val digits = text.filter(Char::isDigit).trimStart('0')
        if (digits.isEmpty()) return 0
        return digits.toLongOrNull()?.coerceAtMost(MAX_INPUT_CENTS) ?: MAX_INPUT_CENTS
    }

    private fun groupThousands(value: Long): String =
        value.toString()
            .reversed()
            .chunked(3)
            .joinToString(".")
            .reversed()
}
