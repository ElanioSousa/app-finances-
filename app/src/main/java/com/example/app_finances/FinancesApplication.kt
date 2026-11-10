package com.example.app_finances

import android.app.Application
import com.example.app_finances.di.AppContainer

class FinancesApplication : Application() {
    /** Trocável só nos testes, para cada teste começar com um banco limpo. */
    lateinit var container: AppContainer
        internal set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
