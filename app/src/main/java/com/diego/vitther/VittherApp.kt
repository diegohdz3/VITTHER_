package com.diego.vitther

import android.app.Application
import com.diego.vitther.di.AppContainer

class VittherApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        com.diego.vitther.data.notifications.programarAvisos(this)
    }
}
