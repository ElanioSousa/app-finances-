package com.example.app_finances.purchases

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.app_finances.appContainer
import com.example.app_finances.domain.BankNotification
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Recebe as notificações do celular (com o acesso que o usuário liga nas configurações do Android)
 * e só olha as dos apps de [BankNotification.packages]. Nada é guardado nem sai do aparelho:
 * uma compra reconhecida vira só uma sugestão de lançamento.
 */
class PurchaseNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in BankNotification.packages) return
        val (title, text) = titleAndText(sbn.notification)
        val notice = BankNotification.parse(sbn.packageName, title, text) ?: return
        if (PurchaseSuggestions.isShown(this, notice)) return
        scope.launch {
            try {
                val cards = applicationContext.appContainer.cardRepository.observeCards().first()
                PurchaseSuggestions.suggest(applicationContext, notice, notice.quickText(cards))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Falha ao sugerir o lançamento da compra", e)
            }
        }
    }

    /** Título e texto (o expandido, se houver) da notificação. */
    private fun titleAndText(notification: Notification): Pair<String?, String?> {
        val extras = notification.extras
        val text = extras.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: extras.getCharSequence(Notification.EXTRA_TEXT)
        return extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() to text?.toString()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val TAG = "PurchaseListener"
    }
}
