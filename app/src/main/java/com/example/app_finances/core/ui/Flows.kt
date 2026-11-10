package com.example.app_finances.core.ui

import kotlinx.coroutines.flow.SharingStarted

/**
 * Mantém as consultas vivas por 5 s depois que a tela some (ex.: girar o celular)
 * e as encerra depois disso.
 */
val WhileUiSubscribed: SharingStarted = SharingStarted.WhileSubscribed(5_000)
