package com.example.app_finances.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.app_finances.appContainer
import com.example.app_finances.di.AppContainer

/**
 * Cria (ou reaproveita) um ViewModel cujas dependências vêm do [AppContainer].
 * Por padrão ele vive enquanto a tela de navegação viver; passe outro [owner]
 * (ex.: `rememberViewModelStoreOwner()`) para prendê-lo a um trecho da interface.
 */
@Composable
inline fun <reified VM : ViewModel> containerViewModel(
    owner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current),
    crossinline create: (AppContainer) -> VM,
): VM {
    val container = LocalContext.current.appContainer
    return viewModel(viewModelStoreOwner = owner, factory = viewModelFactory { initializer { create(container) } })
}
