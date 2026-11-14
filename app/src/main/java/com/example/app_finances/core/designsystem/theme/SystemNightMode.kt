package com.example.app_finances.core.designsystem.theme

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import androidx.core.content.getSystemService
import com.example.app_finances.data.settings.ThemeMode

/**
 * Avisa o sistema (Android 12+) do tema escolhido no app. Ele guarda a escolha, e assim a
 * tela de abertura e o fundo da janela já nascem na cor certa (values ou values-night),
 * sem um instante na cor errada. Mudar recria a tela uma vez.
 */
object SystemNightMode {
    fun apply(context: Context, mode: ThemeMode) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val night = when (mode) {
            ThemeMode.DARK -> UiModeManager.MODE_NIGHT_YES
            ThemeMode.LIGHT -> UiModeManager.MODE_NIGHT_NO
            ThemeMode.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
        }
        // Sempre avisa: o "nightMode" do sistema é o do celular, não o deste app (repetir o mesmo valor não recria a tela).
        context.getSystemService<UiModeManager>()?.setApplicationNightMode(night)
    }
}
