package com.codeninjavik.myra

import android.app.Application

class MyraApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.initialize(this)
    }
}
