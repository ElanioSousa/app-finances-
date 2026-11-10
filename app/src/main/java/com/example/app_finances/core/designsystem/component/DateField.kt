package com.example.app_finances.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.app_finances.R
import com.example.app_finances.core.format.Dates
import java.time.LocalDate

/** Atalhos "Hoje"/"Ontem" e um chip com a data que abre o calendário. */
@Composable
fun DateField(date: LocalDate, onDateChange: (LocalDate) -> Unit, modifier: Modifier = Modifier) {
    val today = LocalDate.now()
    var pickerOpen by remember { mutableStateOf(false) }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilterChip(date == today, { onDateChange(today) }, { Text(stringResource(R.string.form_today)) })
        FilterChip(date == today.minusDays(1), { onDateChange(today.minusDays(1)) }, { Text(stringResource(R.string.form_yesterday)) })
        val isShortcut = Dates.relative(date, today) != null
        FilterChip(
            selected = !isShortcut,
            onClick = { pickerOpen = true },
            label = { Text(if (isShortcut) stringResource(R.string.form_other_date) else Dates.short(date, today)) },
            leadingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = stringResource(R.string.form_date)) },
        )
    }
    if (pickerOpen) {
        DatePickerPopup(date, onConfirm = { onDateChange(it); pickerOpen = false }, onDismiss = { pickerOpen = false })
    }
}

@Composable
private fun DatePickerPopup(date: LocalDate, onConfirm: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = Dates.toPickerMillis(date))
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { state.selectedDateMillis?.let { onConfirm(Dates.fromPickerMillis(it)) } }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    ) {
        DatePicker(state = state)
    }
}
