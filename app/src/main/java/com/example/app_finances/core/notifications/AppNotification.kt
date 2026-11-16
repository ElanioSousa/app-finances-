package com.example.app_finances.core.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.app_finances.MainActivity
import com.example.app_finances.R

/**
 * Notificação simples do app: tocada, abre [intent] (o app, por padrão). Na tela de bloqueio
 * aparece só [publicTitle], sem valores.
 */
data class AppNotification(
    val title: String,
    val text: String,
    val publicTitle: String,
    val intent: Intent? = null,
)

/** Mostra [content] no canal [channelId]; o mesmo [id] substitui a anterior. Sem permissão, não faz nada. */
fun Context.postNotification(channelId: String, id: Int, content: AppNotification) {
    if (!canPostNotifications()) return
    val target = content.intent ?: Intent(this, MainActivity::class.java).setAction(Intent.ACTION_MAIN)
    val open = PendingIntent.getActivity(this, id, target, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    val public = NotificationCompat.Builder(this, channelId).setSmallIcon(R.drawable.ic_notification).setContentTitle(content.publicTitle).build()
    val notification = NotificationCompat.Builder(this, channelId)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(content.title)
        .setContentText(content.text)
        .setStyle(NotificationCompat.BigTextStyle().bigText(content.text))
        .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
        .setPublicVersion(public)
        .setContentIntent(open)
        .setAutoCancel(true)
        .build()
    @Suppress("MissingPermission") // conferida em canPostNotifications()
    NotificationManagerCompat.from(this).notify(id, notification)
}
