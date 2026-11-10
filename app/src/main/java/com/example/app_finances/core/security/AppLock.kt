package com.example.app_finances.core.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

/**
 * Bloqueio do app por biometria (ou o PIN/padrão do aparelho como alternativa).
 * Desbloqueia uma vez por uso e volta a bloquear quando o app vai para segundo plano
 * — girar a tela não conta como sair.
 */
class AppLock {
    var unlocked by mutableStateOf(false)
        private set

    /**
     * O aparelho tem bloqueio de tela? Sem ele não há como autenticar, então o app não
     * trava (senão o usuário ficaria preso fora). Conferido de novo a cada volta ao app.
     */
    var available by mutableStateOf(false)
        private set

    /** Bloqueia ao sair do app e reconfere o bloqueio do aparelho ao voltar; chamado uma vez. */
    fun watchAppBackground(context: Context) {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                available = isAvailable(context)
            }

            override fun onStop(owner: LifecycleOwner) {
                unlocked = false
            }
        })
    }

    fun prompt(activity: FragmentActivity, title: String) {
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                unlocked = true
            }
        }
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setAllowedAuthenticators(AUTHENTICATORS)
            .build()
        BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback).authenticate(info)
    }

    companion object {
        /** Digital/rosto ou, se preferir, o bloqueio de tela do aparelho. */
        const val AUTHENTICATORS = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

        /** O aparelho tem algum bloqueio configurado? Sem isso não dá para ligar a opção. */
        fun isAvailable(context: Context): Boolean =
            BiometricManager.from(context).canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS
    }
}
