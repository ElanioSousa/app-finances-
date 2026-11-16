package com.example.app_finances.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.app_finances.R
import com.example.app_finances.core.designsystem.component.AmountLine
import com.example.app_finances.core.designsystem.component.AmountTone
import com.example.app_finances.core.designsystem.component.MoneyText
import com.example.app_finances.core.designsystem.component.SectionCard
import com.example.app_finances.core.designsystem.component.formatMoney
import com.example.app_finances.core.designsystem.theme.FinanceTheme
import com.example.app_finances.core.format.Dates
import com.example.app_finances.domain.MonthBalance
import com.example.app_finances.domain.SpendingRoom

/** Marcas para os testes acharem cada valor, mesmo quando dois são iguais. */
const val TAG_FORECAST = "home_forecast"
const val TAG_ROOM = "home_room"

/** "Previsto no fim do mês: R$ X" e "Pode gastar: R$ Y · R$ Z por dia". */
@Composable
fun OutlookLines(outlook: Outlook) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        LabeledAmount(stringResource(R.string.home_forecast), outlook.endOfMonthCents, TAG_FORECAST)
        RoomLine(outlook.room)
    }
}

@Composable
private fun LabeledAmount(label: String, cents: Long, tag: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        MoneyText(cents = cents, tone = AmountTone.BySign, style = MaterialTheme.typography.titleSmall, modifier = Modifier.testTag(tag))
    }
}

/** Com folga: quanto e por dia (e o que ficou reservado para as metas). Negativo: o quanto falta. */
@Composable
private fun RoomLine(room: SpendingRoom) {
    if (room.totalCents < 0) {
        Text(
            stringResource(R.string.home_room_none, formatMoney(-room.totalCents)),
            style = MaterialTheme.typography.bodyMedium,
            color = FinanceTheme.colors.expense,
            modifier = Modifier.testTag(TAG_ROOM),
        )
        return
    }
    LabeledAmount(stringResource(R.string.home_room), room.totalCents, TAG_ROOM)
    Text(stringResource(R.string.home_room_per_day, formatMoney(room.perDayCents)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    if (room.reservedForGoalsCents > 0) {
        Text(stringResource(R.string.home_room_goals, formatMoney(room.reservedForGoalsCents)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Saldo previsto no fim de cada um dos próximos meses (vermelho se ficar negativo). */
@Composable
fun NextMonthsCard(months: List<MonthBalance>) {
    SectionCard(title = stringResource(R.string.home_next_months)) {
        Text(stringResource(R.string.home_next_months_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        months.forEach { AmountLine(Dates.month(it.month), it.endCents, AmountTone.BySign) }
    }
}
