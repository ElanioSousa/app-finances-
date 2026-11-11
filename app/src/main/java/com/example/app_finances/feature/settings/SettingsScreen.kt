package com.example.app_finances.feature.settings

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.TableView
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.app_finances.BuildConfig
import com.example.app_finances.R
import com.example.app_finances.core.designsystem.component.CategoryIcon
import com.example.app_finances.core.designsystem.component.ConfirmDialog
import com.example.app_finances.core.designsystem.component.ConfirmTexts
import com.example.app_finances.core.designsystem.component.DialogActions
import com.example.app_finances.core.designsystem.component.LabeledSwitch
import com.example.app_finances.core.designsystem.component.ListRow
import com.example.app_finances.core.designsystem.component.RowText
import com.example.app_finances.core.designsystem.component.ScreenColumn
import com.example.app_finances.core.designsystem.component.SectionCard
import com.example.app_finances.core.security.AppLock
import com.example.app_finances.core.ui.LocalSnackbarHostState
import com.example.app_finances.core.ui.containerViewModel
import com.example.app_finances.reminders.BillReminders
import java.time.LocalDate

@Composable
fun SettingsScreen(scaffoldPadding: PaddingValues, onBack: () -> Unit) {
    val viewModel = containerViewModel { SettingsViewModel(it.settingsRepository, it.backupRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    SettingsEvents(viewModel)

    ScreenColumn(title = stringResource(R.string.settings_title), scaffoldPadding = scaffoldPadding, onBack = onBack) {
        item { PrivacySection(state, viewModel) }
        item { RemindersSection(state.settings.billReminders, viewModel::setBillReminders) }
        item { DataSection(state.isWorking, viewModel) }
        item {
            Text(stringResource(R.string.settings_version, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    state.pendingRestore?.let {
        val texts = ConfirmTexts(stringResource(R.string.settings_restore_title), stringResource(R.string.settings_restore_body), stringResource(R.string.settings_restore_confirm))
        ConfirmDialog(texts, DialogActions(viewModel::confirmRestore, viewModel::cancelRestore))
    }
}

@Composable
private fun SettingsEvents(viewModel: SettingsViewModel) {
    val snackbar = LocalSnackbarHostState.current
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            val message = when (event) {
                is SettingsEvent.Failed -> event.message
                is SettingsEvent.Done -> context.getString(
                    when (event.task) {
                        FileTask.BACKUP -> R.string.settings_backup_done
                        FileTask.RESTORE -> R.string.settings_restore_done
                        FileTask.CSV -> R.string.settings_csv_done
                    },
                )
            }
            snackbar.showSnackbar(message)
        }
    }
}

@Composable
private fun PrivacySection(state: SettingsUiState, viewModel: SettingsViewModel) {
    val context = LocalContext.current
    val unavailable = stringResource(R.string.settings_app_lock_unavailable)
    SectionCard(title = stringResource(R.string.settings_privacy)) {
        LabeledSwitch(stringResource(R.string.settings_hide_values), state.settings.hideValues, stringResource(R.string.settings_hide_values_hint), viewModel::setHideValues)
        LabeledSwitch(stringResource(R.string.settings_app_lock), state.settings.appLock, stringResource(R.string.settings_app_lock_hint)) { on ->
            // Sem bloqueio de tela no aparelho não há como exigir biometria: avisa em vez de ligar.
            if (!on || AppLock.isAvailable(context)) viewModel.setAppLock(on) else viewModel.warn(unavailable)
        }
    }
}

/** Pede a permissão de notificação (Android 13+) antes de ligar os lembretes. */
@Composable
private fun RemindersSection(enabled: Boolean, onChange: (Boolean) -> Unit) {
    val context = LocalContext.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> onChange(granted) }
    SectionCard(title = stringResource(R.string.settings_notifications)) {
        LabeledSwitch(stringResource(R.string.settings_bill_reminders), enabled, stringResource(R.string.settings_bill_reminders_hint)) { on ->
            if (on && !BillReminders.canNotify(context)) permission.launch(Manifest.permission.POST_NOTIFICATIONS) else onChange(on)
        }
    }
}

/** Backup, restauração e CSV em arquivos que o usuário escolhe (sem permissão de armazenamento). */
@Composable
private fun DataSection(isWorking: Boolean, viewModel: SettingsViewModel) {
    val today = LocalDate.now()
    val backup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let(viewModel::exportBackup) }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(viewModel::askRestore) }
    val csv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri -> uri?.let(viewModel::exportCsv) }
    SectionCard(title = stringResource(R.string.settings_data)) {
        val enabled = !isWorking
        ActionRow(Icons.Outlined.SaveAlt, RowText(stringResource(R.string.settings_export_backup), stringResource(R.string.settings_export_backup_hint)), enabled) { backup.launch("financas-backup-$today.json") }
        ActionRow(Icons.Outlined.Restore, RowText(stringResource(R.string.settings_restore_backup), stringResource(R.string.settings_restore_backup_hint)), enabled) { restore.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) }
        ActionRow(Icons.Outlined.TableView, RowText(stringResource(R.string.settings_export_csv), stringResource(R.string.settings_export_csv_hint)), enabled) { csv.launch("lancamentos-$today.csv") }
    }
}

@Composable
private fun ActionRow(icon: ImageVector, text: RowText, enabled: Boolean, onClick: () -> Unit) {
    ListRow(
        text = text,
        onClick = onClick.takeIf { enabled },
        modifier = Modifier.clip(MaterialTheme.shapes.medium),
        leading = { CategoryIcon(icon, MaterialTheme.colorScheme.primary, size = 36.dp) },
    )
}
