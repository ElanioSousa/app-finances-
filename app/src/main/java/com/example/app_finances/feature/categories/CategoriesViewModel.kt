package com.example.app_finances.feature.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_finances.core.ui.EditorController
import com.example.app_finances.core.ui.Editing
import com.example.app_finances.core.ui.UserMessages
import com.example.app_finances.core.ui.WhileUiSubscribed
import com.example.app_finances.data.local.entity.CategoryEntity
import com.example.app_finances.data.local.entity.CategoryKind
import com.example.app_finances.data.repository.CategoryRepository
import com.example.app_finances.data.repository.TransactionRepository
import com.example.app_finances.domain.BudgetStatus
import com.example.app_finances.domain.Budgets
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.YearMonth

/** Categoria sendo criada ou editada no diálogo. */
data class CategoryEditor(
    val original: CategoryEntity? = null,
    val kind: CategoryKind = original?.kind ?: CategoryKind.EXPENSE,
    val name: String = original?.name.orEmpty(),
    val iconKey: String = original?.iconKey ?: "other",
    val colorArgb: Int = original?.colorArgb ?: DEFAULT_COLOR,
    /** 0 = sem orçamento. */
    val budgetCents: Long = original?.monthlyBudgetCents ?: 0,
) {
    fun toEntity() = CategoryEntity(
        id = original?.id ?: 0,
        name = name.trim(),
        kind = kind,
        iconKey = iconKey,
        colorArgb = colorArgb,
        archived = original?.archived ?: false,
        monthlyBudgetCents = budgetCents.takeIf { it > 0 && kind == CategoryKind.EXPENSE },
    )

    private companion object {
        const val DEFAULT_COLOR = 0xFF94A3B8.toInt()
    }
}

/** Uma categoria na lista, com o orçamento do mês quando houver. */
data class CategoryRow(val category: CategoryEntity, val budget: BudgetStatus?)

data class CategoriesUiState(
    val kind: CategoryKind = CategoryKind.EXPENSE,
    val rows: List<CategoryRow> = emptyList(),
    val editor: Editing<CategoryEditor>? = null,
    val confirmArchive: CategoryEntity? = null,
)

class CategoriesViewModel(
    private val categoryRepository: CategoryRepository,
    transactionRepository: TransactionRepository,
    today: () -> LocalDate,
) : ViewModel() {

    private val kind = MutableStateFlow(CategoryKind.EXPENSE)
    private val editor = EditorController<CategoryEditor>(viewModelScope)
    private val confirmArchive = MutableStateFlow<CategoryEntity?>(null)
    private val messages = UserMessages(viewModelScope)

    val errorFlow: Flow<String> = messages.flow

    private val rows: Flow<List<CategoryRow>> = combine(
        categoryRepository.observeActive(),
        transactionRepository.observeMonth(YearMonth.from(today())),
        kind,
    ) { categories, month, kind ->
        val budgets = Budgets.statuses(categories, month).associateBy { it.category.id }
        categories.filter { it.kind == kind }.map { CategoryRow(it, budgets[it.id]) }
    }

    val state: StateFlow<CategoriesUiState> = combine(kind, rows, editor.state, confirmArchive, ::CategoriesUiState)
        .stateIn(viewModelScope, WhileUiSubscribed, CategoriesUiState())

    fun setKind(value: CategoryKind) = kind.update { value }

    fun openNew() = editor.open(CategoryEditor(kind = kind.value))

    fun openEdit(category: CategoryEntity) = editor.open(CategoryEditor(original = category))

    fun edit(transform: (CategoryEditor) -> CategoryEditor) = editor.edit(transform)

    fun close() = editor.close()

    fun save() = editor.save { draft ->
        val entity = draft.toEntity()
        if (draft.original == null) categoryRepository.add(entity) else categoryRepository.update(entity)
    }

    fun askArchive(category: CategoryEntity) = confirmArchive.update { category }

    fun cancelArchive() = confirmArchive.update { null }

    fun confirmArchive() {
        val category = confirmArchive.value ?: return
        confirmArchive.update { null }
        messages.launch { categoryRepository.archive(category) }
    }
}
