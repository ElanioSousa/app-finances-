package com.example.app_finances.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.example.app_finances.appContainer
import com.example.app_finances.MainActivity
import com.example.app_finances.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/** Widget "Resumo das finanças": saldo, previsto no fim do mês, lançar por voz e novo lançamento. */
class SummaryWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        // Lê o banco fora da thread principal; o Android espera até finish().
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                // O Android só espera uns 10 s por goAsync(): desiste antes disso.
                val summary = withTimeout(LOAD_TIMEOUT_MS) { WidgetSummary.load(context.appContainer) }
                manager.updateAppWidget(ids, views(context, summary))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Falha ao atualizar o widget", e)
            } finally {
                pending.finish()
            }
        }
    }

    private fun views(context: Context, summary: WidgetSummary) = RemoteViews(context.packageName, R.layout.widget_summary).apply {
        setTextViewText(R.id.widget_balance, summary.balance)
        setTextViewText(R.id.widget_forecast, context.getString(R.string.widget_forecast, summary.forecast))
        setOnClickPendingIntent(R.id.widget_root, openApp(context, Intent.ACTION_MAIN, REQUEST_OPEN))
        setOnClickPendingIntent(R.id.widget_voice, openApp(context, MainActivity.ACTION_VOICE_ENTRY, REQUEST_VOICE))
        setOnClickPendingIntent(R.id.widget_add, openApp(context, MainActivity.ACTION_NEW_ENTRY, REQUEST_ADD))
    }

    private fun openApp(context: Context, action: String, requestCode: Int): PendingIntent = PendingIntent.getActivity(
        context,
        requestCode,
        Intent(context, MainActivity::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    companion object {
        private const val TAG = "SummaryWidget"
        private const val LOAD_TIMEOUT_MS = 8_000L
        private const val REQUEST_OPEN = 0
        private const val REQUEST_VOICE = 1
        private const val REQUEST_ADD = 2

        /** O Android (o launcher) aceita adicionar o widget por um pedido do app? */
        fun canPin(context: Context): Boolean = AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported

        /** Pede ao launcher para pôr o widget na tela inicial (ele mostra a confirmação). */
        fun requestPin(context: Context) {
            AppWidgetManager.getInstance(context).requestPinAppWidget(ComponentName(context, SummaryWidget::class.java), null, null)
        }

        /** Redesenha os widgets já colocados na tela inicial (sem nenhum, não faz nada). */
        fun refresh(context: Context) {
            val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, SummaryWidget::class.java))
            if (ids.isEmpty()) return
            context.sendBroadcast(
                Intent(context, SummaryWidget::class.java)
                    .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids),
            )
        }
    }
}
