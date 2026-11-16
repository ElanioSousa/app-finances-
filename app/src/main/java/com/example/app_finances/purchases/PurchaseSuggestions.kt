package com.example.app_finances.purchases

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import com.example.app_finances.MainActivity
import com.example.app_finances.R
import com.example.app_finances.core.notifications.canPostNotifications
import com.example.app_finances.core.notifications.createNotificationChannel
import com.example.app_finances.core.format.Money
import com.example.app_finances.domain.CardPurchaseNotice

/**
 * Sugestão de lançamento a partir de uma compra avisada pelo banco: uma notificação do app
 * que, tocada, abre o formulário já preenchido (o usuário confere e salva).
 */
object PurchaseSuggestions {
    private const val CHANNEL_ID = "purchase_suggestions"

    fun createChannel(context: Context) {
        context.createNotificationChannel(CHANNEL_ID, R.string.purchases_channel)
    }

    /** O app tem acesso às notificações (o usuário liga nas configurações do Android)? */
    fun hasAccess(context: Context): Boolean = context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)

    /** Tela do Android onde se liga (ou desliga) o acesso às notificações deste app. */
    fun accessSettingsIntent(context: Context): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, ComponentName(context, PurchaseNotificationListener::class.java).flattenToString())
        } else {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        }

    /** A sugestão desta compra já está na tela (o Nubank e o Google Carteira avisam juntos)? */
    fun isShown(context: Context, notice: CardPurchaseNotice): Boolean =
        context.getSystemService<NotificationManager>()?.activeNotifications.orEmpty().any { it.id == notice.key.hashCode() }

    /** A mesma compra (Nubank e Google Carteira avisam juntos) substitui a mesma sugestão. */
    fun suggest(context: Context, notice: CardPurchaseNotice, quickText: String) {
        if (!context.canPostNotifications()) return
        val id = notice.key.hashCode()
        val title = context.getString(R.string.purchases_title, Money.format(notice.amountCents))
        val open = PendingIntent.getActivity(context, id, formIntent(context, quickText), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(context.getString(R.string.purchases_text, notice.merchant))
            // Na tela de bloqueio, sem valor nem loja.
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion(context))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        @Suppress("MissingPermission") // conferida em canPostNotifications()
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    /** O que aparece na tela de bloqueio: sem valor nem loja. */
    private fun publicVersion(context: Context) = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(context.getString(R.string.purchases_public))
        .build()

    /** Como um "Compartilhar": o app abre o formulário com o texto já interpretado. */
    private fun formIntent(context: Context, quickText: String) = Intent(context, MainActivity::class.java)
        .setAction(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_TEXT, quickText)
}
