package com.example.app_finances.feature.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_finances.core.model.Catalog
import com.example.app_finances.core.model.CatalogSources
import com.example.app_finances.core.model.observeCatalog
import com.example.app_finances.core.ui.WhileUiSubscribed
import com.example.app_finances.data.repository.TransactionRepository
import com.example.app_finances.domain.Report
import com.example.app_finances.domain.ReportPeriod
import com.example.app_finances.domain.ReportRange
import com.example.app_finances.domain.Reports
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.YearMonth

data class ReportsUiState(
    val range: ReportRange,
    val report: Report? = null,
    val catalog: Catalog = Catalog(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModel(
    transactionRepository: TransactionRepository,
    catalogSources: CatalogSources,
    today: () -> LocalDate,
) : ViewModel() {

    private val range = MutableStateFlow(ReportRange(ReportPeriod.MONTH, YearMonth.from(today())))

    private val report = range.flatMapLatest { range ->
        transactionRepository.observeMonths(YearMonth.from(range.start), YearMonth.from(range.end)).map(Reports::build)
    }.flowOn(Dispatchers.Default)

    val state: StateFlow<ReportsUiState> = combine(range, report, observeCatalog(catalogSources), ::ReportsUiState)
        .stateIn(viewModelScope, WhileUiSubscribed, ReportsUiState(range.value))

    fun setPeriod(period: ReportPeriod) = range.update { it.copy(period = period) }

    fun previous() = range.update { it.previous() }

    fun next() = range.update { it.next() }
}
