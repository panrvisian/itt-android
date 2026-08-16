package com.bigbrother.mobile

import android.app.Application

class BigBrotherApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
