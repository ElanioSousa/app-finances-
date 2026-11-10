package com.example.app_finances.core.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.staticCompositionLocalOf

/** Snackbar do Scaffold principal, para qualquer tela avisar algo (ex.: "Desfazer"). */
val LocalSnackbarHostState = staticCompositionLocalOf<SnackbarHostState> {
    error("LocalSnackbarHostState não foi fornecido")
}
