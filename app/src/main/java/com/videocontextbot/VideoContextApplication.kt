package com.videocontextbot

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class VideoContextApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Setup Timber logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        Timber.d("Application started")
    }
}
