package com.bigbrother.mobile.ui

import android.app.Activity
import android.graphics.Color as AndroidColor
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.core.view.WindowCompat
import com.bigbrother.mobile.data.AppSettings
import com.bigbrother.mobile.data.FontScaleMode
import com.bigbrother.mobile.data.ThemeMode
import com.bigbrother.mobile.data.UiStyle

val LocalIsDarkTheme = compositionLocalOf { false }
val LocalUiStyle = compositionLocalOf { UiStyle.Miuix }

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
    val colorScheme = remember(context, dark, settings.uiStyle) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            if (dark) androidx.compose.material3.darkColorScheme() else androidx.compose.material3.lightColorScheme()
        }
    }
    val activity = context as? Activity
    SideEffect {
        activity?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = AndroidColor.TRANSPARENT
            window.navigationBarColor = AndroidColor.TRANSPARENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.navigationBarDividerColor = AndroidColor.TRANSPARENT
            }
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    val density = LocalDensity.current
    val fontScale = when (settings.fontScaleMode) {
        FontScaleMode.System -> density.fontScale
        FontScaleMode.Small -> 0.88f
        FontScaleMode.Medium -> 1.0f
        FontScaleMode.Large -> 1.12f
        FontScaleMode.XLarge -> 1.25f
    }
    val baseTypography = Typography()
    val typography = if (settings.uiStyle == UiStyle.Miuix) {
        baseTypography.copy(
            displaySmall = baseTypography.displaySmall.copy(fontWeight = FontWeight.Normal),
            headlineMedium = baseTypography.headlineMedium.copy(fontWeight = FontWeight.Medium),
            titleLarge = baseTypography.titleLarge.copy(fontWeight = FontWeight.Medium),
            titleMedium = baseTypography.titleMedium.copy(fontWeight = FontWeight.Medium)
        )
    } else {
        baseTypography
    }
    val shapes = if (settings.uiStyle == UiStyle.Miuix) {
        Shapes(
            small = RoundedCornerShape(16.dp),
            medium = RoundedCornerShape(24.dp),
            large = RoundedCornerShape(32.dp)
        )
    } else {
        Shapes(
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(28.dp)
        )
    }
    CompositionLocalProvider(
        LocalDensity provides Density(density.density, fontScale),
        LocalIsDarkTheme provides dark,
        LocalUiStyle provides settings.uiStyle
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = shapes,
            content = content
        )
    }
}
