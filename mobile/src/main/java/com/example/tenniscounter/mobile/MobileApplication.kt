package com.example.tenniscounter.mobile

import android.app.Application
import com.example.tenniscounter.mobile.di.MobileServiceLocator

class MobileApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MobileServiceLocator.garminConnectivityManager(this).initialize()
    }
}
