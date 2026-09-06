package com.bigbrother.mobile.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bigbrother.mobile.BigBrotherApp
import com.bigbrother.mobile.R
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: MainViewModel = viewModel(factory = MainViewModelFactory(application as BigBrotherApp))
            val settings by vm.settings.collectAsStateWithLifecycle()

            val splashAlpha = remember { Animatable(1f) }
            var splashVisible by rememberSaveable { mutableStateOf(true) }

            LaunchedEffect(Unit) {
                // Pre-warm Room database data loading, layout composition & AGSL shaders under splash screen
                delay(900)
                // Smooth GPU alpha fade-out transition over 300ms
                splashAlpha.animateTo(0f, tween(300))
                splashVisible = false
            }

            BigBrotherTheme(settings = settings) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AppRoot(viewModel = vm)

                    if (splashVisible) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { alpha = splashAlpha.value }
                        ) {
                            StartupSplashScreen()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StartupSplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Image(
                painter = painterResource(R.drawable.splash_icon),
                contentDescription = null,
                modifier = Modifier.size(220.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Powered by Panrvisian\nGPT 5.5",
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}


