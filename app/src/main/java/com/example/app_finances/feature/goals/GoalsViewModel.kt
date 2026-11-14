package com.example.app_finances.feature.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_finances.core.ui.Editing
import com.example.app_finances.core.ui.EditorController
import com.example.app_finances.core.ui.UserMessages
import com.example.app_finances.core.ui.WhileUiSubscribed
import com.example.app_finances.data.local.entity.GoalEntity
import com.example.app_finances.data.repository.GoalRepository
import com.example.app_finances.domain.GoalPlan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate

/** Meta sendo criada ou editada. Sem prazo, [deadline] só guarda a sugestão do seletor. */
data class GoalEditor(
    val original: GoalEntity? = null,
    val name: String = original?.name.orEmpty(),
    val targetCents: Long = original?.targetCents ?: 0,
    val hasDeadline: Boolean = original?.deadline != null,
    val deadline: LocalDate,
    val colorArgb: Int = original?.colorArgb ?: DEFAULT_COLOR,
) {
    fun toEntity(): GoalEntity = (original ?: GoalEntity(name = "", targetCents = 0, colorArgb = colorArgb)).copy(
        name = name.trim(),
        targetCents = targetCents,
        deadline = deadline.takeIf { hasDeadline },
        colorArgb = colorArgb,
    )

    private companion object {
        const val DEFAULT_COLOR = 0xFF34D399.toInt()
    }
}

/** "Guardar" ou "Retirar" um valor de uma meta. */
data class MoveEditor(val goal: GoalEntity, val withdraw: Boolean, val amountCents: Long = 0)

data class GoalRow(val goal: GoalEntity, val plan: GoalPlan)

/** Diálogos abertos na tela (no máximo um de cada). */
data class GoalDialogs(
    val editor: Editing<GoalEditor>? = null,
    val move: Editing<MoveEditor>? = null,
    val confirmDelete: GoalEntity? = null,
)

data class GoalsUiState(
    val rows: List<GoalRow> = emptyList(),
    val dialogs: GoalDialogs = GoalDialogs(),
    val isLoading: Boolean = true,
) {
    val savedTotalCents: Long get() = rows.sumOf { it.goal.savedCents }
}

class GoalsViewModel(private val repository: GoalRepository, private val today: () -> LocalDate) : ViewModel() {

    private val editor = EditorController<GoalEditor>(viewModelScope)
    private val mover = EditorController<MoveEditor>(viewModelScope)
    private val confirmDelete = MutableStateFlow<GoalEntity?>(null)
    private val messages = UserMessages(viewModelScope)

    /** Falhas ao excluir, para a tela mostrar num aviso. */
    val errorFlow: Flow<String> = messages.flow

    private val dialogs = combine(editor.state, mover.state, confirmDelete, ::GoalDialogs)

    val state: StateFlow<GoalsUiState> = combine(repository.observeAll().map(::toRows), dialogs) { rows, open ->
        GoalsUiState(rows, open, isLoading = false)
    }.stateIn(viewModelScope, WhileUiSubscribed, GoalsUiState())

    fun openNew() = editor.open(GoalEditor(deadline = today().plusMonths(DEFAULT_MONTHS)))

    fun openEdit(goal: GoalEntity) = editor.open(GoalEditor(original = goal, deadline = goal.deadline ?: today().plusMonths(DEFAULT_MONTHS)))

    fun edit(transform: (GoalEditor) -> GoalEditor) = editor.edit(transform)

    fun close() = editor.close()

    fun save() = editor.save { draft ->
        val entity = draft.toEntity()
        if (draft.original == null) repository.add(entity) else repository.update(entity)
    }

    fun openMove(goal: GoalEntity, withdraw: Boolean) = mover.open(MoveEditor(goal, withdraw))

    fun editMove(transform: (MoveEditor) -> MoveEditor) = mover.edit(transform)

    fun closeMove() = mover.close()

    fun saveMove() = mover.save { move -> repository.move(move.goal, if (move.withdraw) -move.amountCents else move.amountCents) }

    fun askDelete(goal: GoalEntity) = confirmDelete.update { goal }

    fun cancelDelete() = confirmDelete.update { null }

    fun confirmDelete() {
        val goal = confirmDelete.value ?: return
        confirmDelete.update { null }
        messages.launch { repository.delete(goal) }
    }

    private fun toRows(goals: List<GoalEntity>): List<GoalRow> {
        val now = today()
        return goals.map { GoalRow(it, GoalPlan.of(it, now)) }
    }

    private companion object {
        const val DEFAULT_MONTHS = 6L
    }
}
