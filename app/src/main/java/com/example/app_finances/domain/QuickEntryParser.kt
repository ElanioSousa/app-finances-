package com.example.app_finances.domain

import com.example.app_finances.data.local.entity.AccountEntity
import com.example.app_finances.data.local.entity.CategoryEntity
import com.example.app_finances.data.local.entity.CategoryKind
import com.example.app_finances.data.local.entity.CreditCardEntity
import com.example.app_finances.data.local.entity.TransactionType
import java.text.Normalizer
import java.time.LocalDate

/** O que deu para entender de um texto livre como "mercado 45,90 nubank". Nulo = não encontrado. */
data class QuickEntry(
    val type: TransactionType = TransactionType.EXPENSE,
    val amountCents: Long? = null,
    val description: String = "",
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val cardId: Long? = null,
    /** Disse "cartão"/"crédito" sem nomear o cartão: usa o cartão padrão. */
    val onCard: Boolean = false,
    val installments: Int = 1,
    val date: LocalDate? = null,
)

/** Cadastros com que o texto é comparado. */
data class QuickEntryCatalog(
    val categories: List<CategoryEntity>,
    val accounts: List<AccountEntity>,
    val cards: List<CreditCardEntity>,
)

/**
 * Lê um lançamento escrito como numa mensagem: "uber 23,50", "mercado 120 nubank 3x",
 * "recebi salário 2.760,14", "farmácia 35 ontem" (a voz chega aqui já escrita por [SpokenText]). Nada é gravado: o resultado só
 * preenche o formulário, que o usuário confere antes de salvar.
 */
object QuickEntryParser {

    fun parse(text: String, catalog: QuickEntryCatalog, today: LocalDate): QuickEntry {
        val tokens = Tokens(text)
        val date = tokens.takeDate(today)
        val installments = tokens.takeInstallments()
        val amount = tokens.takeAmount()
        val card = tokens.takeNamed(catalog.cards) { it.name }
        val account = tokens.takeNamed(catalog.accounts) { it.name }
        val onCard = card != null || tokens.takeAny(CARD_WORDS)
        val income = tokens.takeAny(INCOME_WORDS)
        val category = CategoryGuess.find(tokens.normalizedText(), catalog.categories, if (income) TransactionType.INCOME else null)
        val type = if (income || category?.kind == CategoryKind.INCOME) TransactionType.INCOME else TransactionType.EXPENSE
        tokens.takeAny(FILLER_WORDS)
        return QuickEntry(
            type = type,
            amountCents = amount,
            description = tokens.remaining(),
            categoryId = category?.id,
            accountId = account?.id,
            cardId = card?.id.takeIf { type == TransactionType.EXPENSE },
            onCard = onCard && type == TransactionType.EXPENSE,
            installments = if (onCard && type == TransactionType.EXPENSE) installments else 1,
            date = date,
        )
    }

    private val CARD_WORDS = setOf("cartao", "credito")
    private val INCOME_WORDS = setOf("recebi", "ganhei", "receita", "entrada")
    private val FILLER_WORDS = setOf(
        "no", "na", "de", "do", "da", "em", "com", "pelo", "pela", "para", "pra", "r$",
        "gastei", "paguei", "comprei", "pix", "debito", "dinheiro", "a", "o",
        "parcelado", "parcelada",
    )
}

/** Palavras do texto, que vão sendo "consumidas" à medida que cada parte é reconhecida. */
private class Tokens(text: String) {
    private val words: MutableList<String> = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.toMutableList()

    fun normalizedText(): String = words.joinToString(" ") { normalize(it) }

    /** "hoje", "ontem", "anteontem" ou "12/11" (ano atual) e "12/11/2026". */
    fun takeDate(today: LocalDate): LocalDate? {
        val index = words.indexOfFirst { DATE_WORD.containsKey(normalize(it)) || DATE.matches(it) }
        if (index < 0) return null
        val word = words.removeAt(index)
        DATE_WORD[normalize(word)]?.let { return today.minusDays(it) }
        val (day, month, year) = DATE.matchEntire(word)!!.destructured
        val fullYear = year.toIntOrNull()?.let { if (it < 100) 2000 + it else it } ?: today.year
        return runCatching { LocalDate.of(fullYear, month.toInt(), day.toInt()) }.getOrNull()
    }

    /** "3x", "3 x", "em 3 vezes", "3 vezes". */
    fun takeInstallments(): Int {
        val single = words.indexOfFirst { INSTALLMENTS.matches(it) }
        if (single >= 0) return INSTALLMENTS.matchEntire(words.removeAt(single))!!.groupValues[1].toInt().coerceIn(1, Installments.MAX)
        val times = words.indexOfFirst { normalize(it) in setOf("x", "vezes", "parcelas") }
        val count = words.getOrNull(times - 1)?.toIntOrNull()
        if (times < 1 || count == null) return 1
        words.removeAt(times)
        words.removeAt(times - 1)
        return count.coerceIn(1, Installments.MAX)
    }

    /** Valor: prefere o número com centavos ou com "R$"; senão, o último número do texto. */
    fun takeAmount(): Long? {
        val candidates = words.indices.filter { AMOUNT.matches(words[it]) }
        val index = candidates.firstOrNull { words[it].contains(',') || words[it].contains('.') || words[it].startsWith("R$", ignoreCase = true) }
            ?: candidates.lastOrNull()
            ?: return null
        return parseCents(words.removeAt(index).removePrefix("R$").removePrefix("r$"))
    }

    /** Remove e devolve o cadastro cujo nome aparece no texto (o nome mais longo primeiro). */
    fun <T> takeNamed(items: List<T>, name: (T) -> String): T? {
        for (item in items.sortedByDescending { name(it).length }) {
            val parts = name(item).split(Regex("\\s+")).map(::normalize).filter { it.isNotEmpty() }
            val start = indexOfSequence(parts)
            if (start >= 0) {
                repeat(parts.size) { words.removeAt(start) }
                return item
            }
        }
        return null
    }

    fun takeAny(set: Set<String>): Boolean {
        val found = words.any { normalize(it) in set }
        words.removeAll { normalize(it) in set }
        return found
    }

    /** O que sobrou vira a descrição: "mercado" → "Mercado". */
    fun remaining(): String = words.joinToString(" ").replaceFirstChar { it.uppercase() }

    private fun indexOfSequence(parts: List<String>): Int {
        if (parts.isEmpty()) return -1
        return (0..words.size - parts.size).firstOrNull { start ->
            parts.indices.all { normalize(words[start + it]) == parts[it] }
        } ?: -1
    }

    private companion object {
        val DATE = Regex("(\\d{1,2})/(\\d{1,2})(?:/(\\d{2,4}))?")
        val DATE_WORD = mapOf("hoje" to 0L, "ontem" to 1L, "anteontem" to 2L)
        val INSTALLMENTS = Regex("(\\d{1,2})x", RegexOption.IGNORE_CASE)
        val AMOUNT = Regex("(?:[Rr]\\$)?(\\d{1,3}(?:\\.\\d{3})+(?:,\\d{1,2})?|\\d+(?:[.,]\\d{1,2})?)")
    }
}

/** "1.234,56", "45,9", "45.90", "45" → centavos. */
internal fun parseCents(text: String): Long? {
    val normalized = when {
        text.contains(',') -> text.replace(".", "").replace(',', '.')
        Regex("\\d{1,3}(\\.\\d{3})+").matches(text) -> text.replace(".", "")
        else -> text
    }
    return normalized.toBigDecimalOrNull()?.movePointRight(2)?.toLong()?.takeIf { it > 0 }
}

/** Sem acentos e em minúsculas: "Farmácia" e "farmacia" são a mesma palavra. */
internal fun normalize(text: String): String =
    Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD).replace(ACCENTS, "").trim(',', '.', '!', '?', ';', ':')

private val ACCENTS = Regex("\\p{M}+")

/**
 * Categoria pelo nome que aparece no texto ou por palavras comuns ("uber" → Transporte).
 * As palavras ficam ligadas ao ícone da categoria, que não muda se o usuário renomeá-la.
 */
private object CategoryGuess {

    fun find(text: String, categories: List<CategoryEntity>, kind: TransactionType?): CategoryEntity? {
        val words = text.split(' ').toSet()
        val usable = categories.filterNot { it.archived }.filter { kind == null || it.kind == kind.categoryKind }
        usable.firstOrNull { category -> normalize(category.name).let { name -> text.contains(name) && name.isNotEmpty() } }?.let { return it }
        val iconKey = KEYWORDS.entries.firstOrNull { (_, keywords) -> keywords.any { it in words || (' ' in it && text.contains(it)) } }?.key
        return iconKey?.let { key -> usable.firstOrNull { it.iconKey == key } }
    }

    // "mercado livre" vem antes de "mercado" (Compras, não Alimentação).
    private val KEYWORDS = linkedMapOf(
        "shopping" to setOf("mercado livre", "shopee", "amazon", "loja", "roupa", "roupas", "magalu", "shein"),
        "food" to setOf("mercado", "supermercado", "padaria", "restaurante", "lanche", "ifood", "almoco", "jantar", "pizza", "acougue", "feira", "cafe", "hortifruti"),
        "transport" to setOf("uber", "99", "gasolina", "combustivel", "posto", "onibus", "metro", "estacionamento", "pedagio", "taxi"),
        "health" to setOf("farmacia", "remedio", "medico", "consulta", "dentista", "exame", "hospital"),
        "education" to setOf("curso", "faculdade", "escola", "livro", "livros"),
        "leisure" to setOf("cinema", "show", "bar", "viagem", "passeio", "jogo", "ingresso"),
        "subscriptions" to setOf("netflix", "spotify", "assinatura", "youtube", "prime", "disney", "hbo"),
        "home" to setOf("aluguel", "condominio"),
        "bills" to setOf("luz", "agua", "energia", "internet", "telefone", "celular", "gas"),
        "salary" to setOf("salario"),
        "freelance" to setOf("freela", "freelance"),
        "investments" to setOf("rendimento", "rendimentos", "dividendos", "juros"),
    )
}
