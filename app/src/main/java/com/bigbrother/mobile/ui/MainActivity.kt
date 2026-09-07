package com.bigbrother.mobile.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bigbrother.mobile.BigBrotherApp

class MainActivity : ComponentActivity() {
    @Volatile
    private var startupContentReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !startupContentReady }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: MainViewModel = viewModel(factory = MainViewModelFactory(application as BigBrotherApp))
            val settings by vm.settings.collectAsStateWithLifecycle()

            BigBrotherTheme(settings = settings) {
                AppRoot(
                    viewModel = vm,
                    onStartupContentReady = { startupContentReady = true }
                )
            }
        }
    }
}

