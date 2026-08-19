package com.springboard.launcher

import android.app.Application
import com.springboard.launcher.di.AppContainer

class SpringboardApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.bootstrap()
    }
}