package com.sw.muzikbulucu

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Store.init(this)
    }
}
