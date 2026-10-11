package com.lumenui.app

import android.app.Application
import com.lumenui.app.di.AppContainer

class LumenApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
