package com.example.app_finances

import android.app.Application
import com.example.app_finances.di.AppContainer

class FinancesApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
