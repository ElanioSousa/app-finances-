package com.example.app_finances.feature.reports

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.app_finances.R
import com.example.app_finances.core.designsystem.component.formatMoney
import com.example.app_finances.core.designsystem.chart.RankedBars
import com.example.app_finances.core.designsystem.component.CategoryIcon
import com.example.app_finances.core.designsystem.component.ListRow
import com.example.app_finances.core.designsystem.component.Option
import com.example.app_finances.core.designsystem.component.OptionSegmentedRow
import com.example.app_finances.core.designsystem.component.RowText
import com.example.app_finances.core.designsystem.component.ScreenColumn
import com.example.app_finances.core.designsystem.component.SectionCard
import com.example.app_finances.core.format.Dates
import com.example.app_finances.core.model.Catalog
import com.example.app_finances.core.designsystem.component.PeriodSelector
import com.example.app_finances.core.ui.MonthTotalsRow
import com.example.app_finances.core.ui.rememberCategoryBars
import com.example.app_finances.core.ui.containerViewModel
import com.example.app_finances.domain.CategorySpend
import com.example.app_finances.domain.OriginTotal
import com.example.app_finances.domain.ReportPeriod
import com.example.app_finances.domain.ReportRange

@Composable
fun ReportsScreen(scaffoldPadding: PaddingValues, onBack: () -> Unit) {
    val viewModel = containerViewModel { ReportsViewModel(it.transactionRepository, it.catalogSources, it.today) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val report = state.report

    ScreenColumn(title = stringResource(R.string.reports_title), scaffoldPadding = scaffoldPadding, onBack = onBack) {
        item { OptionSegmentedRow(periodOptions(), state.range.period, viewModel::setPeriod) }
        item { PeriodSelector(state.range.label(), viewModel::previous, viewModel::next) }
        if (report == null) return@ScreenColumn
        item { MonthTotalsRow(report.totals) }
        if (report.byOrigin.isEmpty()) {
            item { Text(stringResource(R.string.reports_empty), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            return@ScreenColumn
        }
        categorySection(R.string.reports_expenses_by_category, report.expensesByCategory, state.catalog)
        categorySection(R.string.reports_incomes_by_category, report.incomesByCategory, state.catalog)
        item { OriginsCard(report.byOrigin, state.catalog) }
    }
}

@Composable
private fun periodOptions(): List<Option<ReportPeriod>> = listOf(
    Option(ReportPeriod.MONTH, stringResource(R.string.reports_month)),
    Option(ReportPeriod.YEAR, stringResource(R.string.reports_year)),
)

/** "Novembro de 2026" ou "2026". */
private fun ReportRange.label(): String = if (period == ReportPeriod.MONTH) Dates.month(anchor) else anchor.year.toString()

private fun LazyListScope.categorySection(titleRes: Int, totals: List<CategorySpend>, catalog: Catalog) {
    if (totals.isEmpty()) return
    item(key = "section-$titleRes") {
        SectionCard(title = stringResource(titleRes)) { RankedBars(rememberCategoryBars(totals, catalog.categoryById)) }
    }
}

/** Quanto entrou e saiu por conta e por cartão. */
@Composable
private fun OriginsCard(origins: List<OriginTotal>, catalog: Catalog) {
    SectionCard(title = stringResource(R.string.reports_by_origin)) {
        origins.forEach { origin ->
            val card = origin.cardId?.let(catalog.cardById::get)
            val name = card?.name ?: origin.accountId?.let(catalog.accountNames::get).orEmpty()
            ListRow(
                text = RowText(name, stringResource(R.string.reports_origin_line, formatMoney(origin.incomeCents), formatMoney(origin.expenseCents))),
                onClick = null,
                leading = {
                    if (card != null) CategoryIcon(Icons.Outlined.CreditCard, Color(card.colorArgb), size = 32.dp)
                    else CategoryIcon(Icons.Outlined.AccountBalanceWallet, MaterialTheme.colorScheme.primary, size = 32.dp)
                },
            )
        }
    }
}
