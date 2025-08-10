package ru.netology.yandexmap

import android.app.Application
import com.yandex.mapkit.MapKitFactory

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MapKitFactory.setApiKey("669a46a5-1e74-414f-891d-4656f092498a")
    }
}