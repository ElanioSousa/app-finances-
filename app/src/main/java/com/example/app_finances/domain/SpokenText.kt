package com.example.app_finances.domain

/**
 * Passa uma frase falada para o jeito que se escreve um lançamento:
 * "quarenta e cinco reais e noventa centavos no nubank em duas vezes"
 * → "45,90 no nubank em 2 vezes". Só a voz passa por aqui: no texto digitado,
 * "coca zero" ou "Sete Lagoas" ficam como estão.
 */
object SpokenText {

    fun toWritten(spoken: String): String {
        val withAmounts = AMOUNT_RULES.fold(NumberWords.toDigits(spoken)) { text, (regex, amount) -> text.replace(regex, amount) }
        return withBareAmount(withAmounts).replace(CASH, " ").replace(SPACES, " ").trim()
    }

    private fun amount(units: String, cents: String): String = "$units,${cents.ifEmpty { "0" }.padStart(2, '0')}"

    /** "45 reais e 90 centavos", "45 reais", "45 e 90 centavos", "90 centavos". */
    private val AMOUNT_RULES: List<Pair<Regex, (MatchResult) -> CharSequence>> = listOf(
        Regex("(\\d+(?:\\.\\d{3})*)(?:,(\\d{1,2}))? (?:reais|real)(?: e (\\d{1,2})(?: centavos?)?)?", RegexOption.IGNORE_CASE) to
            { m -> amount(m.groupValues[1], m.groupValues[2].ifEmpty { m.groupValues[3] }) },
        Regex("(\\d+) e (\\d{1,2}) centavos?", RegexOption.IGNORE_CASE) to { m -> amount(m.groupValues[1], m.groupValues[2]) },
        Regex("(?<![\\d,])(\\d{1,2}) centavos?", RegexOption.IGNORE_CASE) to { m -> amount("0", m.groupValues[1]) },
    )

    /** "45 e 90" só vira valor se a frase ainda não tem um valor com centavos. */
    private fun withBareAmount(text: String): String {
        if (HAS_CENTS.containsMatchIn(text)) return text
        val match = BARE_AMOUNT.find(text) ?: return text
        return text.replaceRange(match.range, amount(match.groupValues[1], match.groupValues[2]))
    }

    private val BARE_AMOUNT = Regex("(?<![\\d,])(\\d+) e (\\d{2})(?![\\d,])")
    private val HAS_CENTS = Regex("\\d,\\d{2}(?!\\d)")
    private val CASH = Regex("(?i)(?<!\\S)[àa] vista(?!\\S)")
    private val SPACES = Regex("\\s+")
}

/** Números por extenso em português ("três mil e seiscentos" → "3600"). */
private object NumberWords {

    fun toDigits(text: String): String {
        val words = text.split(' ').filter { it.isNotEmpty() }
        val plain = words.map(::normalize)
        val out = mutableListOf<String>()
        var i = 0
        while (i < words.size) {
            val run = readNumber(plain, i)
            if (run == null) out += words[i++] else { out += run.value.toString(); i = run.end }
        }
        return out.joinToString(" ")
    }

    private class Run(val value: Long, val end: Int)

    /**
     * Lê um número a partir de [start] ([words] já sem acentos): "quarenta e cinco", "3 mil e 600".
     * O "e" só emenda uma parte menor que a casa anterior ("quarenta e cinco", mas não "cinco e noventa").
     */
    private fun readNumber(words: List<String>, start: Int): Run? {
        val first = valueOf(words[start]) ?: return null
        if (words[start] in ARTICLES && words.getOrNull(start + 1) !in NUMBER_FOLLOWERS) return null
        var total = 0L
        var group = first.value
        var place = first.place
        var end = start + 1
        while (end < words.size) {
            val next = if (words[end] == "e") words.getOrNull(end + 1)?.let(::valueOf) else null
            when {
                words[end] == "mil" -> { total += (if (group == 0L) 1 else group) * 1000; group = 0; place = 1000; end++ }
                next != null && next.value < place -> { group += next.value; place = next.place; end += 2 }
                else -> break
            }
        }
        return Run(total + group, end)
    }

    private class Value(val value: Long, val place: Long)

    private fun valueOf(word: String): Value? {
        if (word.length in 1..6 && word.all(Char::isDigit)) return word.toLong().let { Value(it, placeOf(it)) }
        if (word == "mil") return Value(1000, 1000)
        UNITS[word]?.let { return Value(it, if (it < 10) 1 else 10) }
        TENS[word]?.let { return Value(it, 10) }
        HUNDREDS[word]?.let { return Value(it, 100) }
        return null
    }

    /** Casa decimal mais baixa preenchida: 600 → 100 (aceita "e 20"), 45 → 1 (não aceita mais nada). */
    private fun placeOf(n: Long): Long = when {
        n % 1000 == 0L && n > 0 -> 1000
        n % 100 == 0L && n > 0 -> 100
        n % 10 == 0L && n >= 20 -> 10
        else -> 1
    }

    /** "um"/"uma" sozinhos são artigo ("um tênis"), salvo antes destas palavras. */
    private val ARTICLES = setOf("um", "uma")
    private val NUMBER_FOLLOWERS = setOf("real", "reais", "mil", "vez", "vezes", "centavo", "centavos", "e")

    private val UNITS = mapOf(
        "zero" to 0L, "um" to 1L, "uma" to 1L, "dois" to 2L, "duas" to 2L, "tres" to 3L, "quatro" to 4L,
        "cinco" to 5L, "seis" to 6L, "sete" to 7L, "oito" to 8L, "nove" to 9L, "dez" to 10L, "onze" to 11L,
        "doze" to 12L, "treze" to 13L, "quatorze" to 14L, "catorze" to 14L, "quinze" to 15L, "dezesseis" to 16L,
        "dezessete" to 17L, "dezoito" to 18L, "dezenove" to 19L,
    )
    private val TENS = mapOf(
        "vinte" to 20L, "trinta" to 30L, "quarenta" to 40L, "cinquenta" to 50L,
        "sessenta" to 60L, "setenta" to 70L, "oitenta" to 80L, "noventa" to 90L,
    )
    private val HUNDREDS = mapOf(
        "cem" to 100L, "cento" to 100L, "duzentos" to 200L, "duzentas" to 200L, "trezentos" to 300L, "trezentas" to 300L,
        "quatrocentos" to 400L, "quatrocentas" to 400L, "quinhentos" to 500L, "quinhentas" to 500L,
        "seiscentos" to 600L, "seiscentas" to 600L, "setecentos" to 700L, "setecentas" to 700L,
        "oitocentos" to 800L, "oitocentas" to 800L, "novecentos" to 900L, "novecentas" to 900L,
    )
}
