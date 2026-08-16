package com.bigbrother.mobile.ui

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.bigbrother.mobile.data.AppSettings
import com.bigbrother.mobile.data.FontScaleMode
import com.bigbrother.mobile.data.ThemeMode

val LocalIsDarkTheme = compositionLocalOf { false }

@Composable
fun BigBrotherTheme(
    settings: AppSettings,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val dark = when (settings.themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val colorScheme = remember(context, dark) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            if (dark) androidx.compose.material3.darkColorScheme() else androidx.compose.material3.lightColorScheme()
        }
    }
    val activity = context as? Activity
    SideEffect {
        activity?.window?.statusBarColor = colorScheme.background.toArgb()
        activity?.window?.navigationBarColor = colorScheme.surface.toArgb()
    }
    val density = LocalDensity.current
    val fontScale = when (settings.fontScaleMode) {
        FontScaleMode.System -> density.fontScale
        FontScaleMode.Small -> 0.88f
        FontScaleMode.Medium -> 1.0f
        FontScaleMode.Large -> 1.12f
        FontScaleMode.XLarge -> 1.25f
    }
    CompositionLocalProvider(
        LocalDensity provides Density(density.density, fontScale),
        LocalIsDarkTheme provides dark
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography(),
            content = content
        )
    }
}

