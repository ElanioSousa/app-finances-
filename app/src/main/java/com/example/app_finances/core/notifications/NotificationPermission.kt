package com.example.app_finances.core.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService

/** O app pode mostrar notificações? (Android 13+ exige a permissão; antes disso, sempre.) */
fun Context.canPostNotifications(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

/** Cria (ou mantém) um canal de notificação do app; criar de novo não faz nada. */
fun Context.createNotificationChannel(id: String, @StringRes nameRes: Int) {
    val channel = NotificationChannel(id, getString(nameRes), NotificationManager.IMPORTANCE_DEFAULT)
    getSystemService<NotificationManager>()?.createNotificationChannel(channel)
}
