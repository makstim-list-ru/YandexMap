package ru.netology.yandexmap

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.yandex.mapkit.MapKitFactory

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MapKitFactory.setApiKey("669a46a5-1e74-414f-891d-4656f092498a")

        setContentView(R.layout.activity_main)

//        val navHostFragment =
//            supportFragmentManager.findFragmentById(R.id.container_fragment) as NavHostFragment
//        val navController = navHostFragment.navController
        enableEdgeToEdge()
    }
}

