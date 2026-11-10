package com.example.app_finances.domain

import com.example.app_finances.data.local.entity.CategoryEntity
import com.example.app_finances.data.local.entity.CategoryKind

/** Invariantes de uma categoria. */
object CategoryRules {

    fun violations(category: CategoryEntity): List<String> = buildList {
        if (category.name.isBlank()) add("Informe o nome da categoria.")
        val budget = category.monthlyBudgetCents
        if (budget != null && category.kind != CategoryKind.EXPENSE) add("Só categorias de despesa têm orçamento.")
        if (budget != null && budget <= 0) add("O orçamento deve ser maior que zero.")
    }

    fun requireValid(category: CategoryEntity) {
        val problems = violations(category)
        require(problems.isEmpty()) { problems.joinToString(" ") }
    }
}
