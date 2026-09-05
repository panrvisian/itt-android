package com.bigbrother.mobile.ui

import android.app.Activity
import android.graphics.Color as AndroidColor
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.core.view.WindowCompat
import com.bigbrother.mobile.data.AppSettings
import com.bigbrother.mobile.data.FontScaleMode
import com.bigbrother.mobile.data.ThemeMode
import com.bigbrother.mobile.data.UiStyle
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.Colors as MiuixColors
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

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
    val accentColor = settings.accentColorArgb?.let(::Color)
    val miuixController = remember(dark, settings.monetEnabled, settings.accentColorArgb) {
        ThemeController(
            colorSchemeMode = when {
                settings.monetEnabled && dark -> ColorSchemeMode.MonetDark
                settings.monetEnabled -> ColorSchemeMode.MonetLight
                dark -> ColorSchemeMode.Dark
                else -> ColorSchemeMode.Light
            },
            keyColor = accentColor.takeIf { settings.monetEnabled },
            isDark = dark,
        )
    }
    val miuixColors = miuixController.currentColors()
    val colorScheme = remember(context, dark, settings.monetEnabled, settings.accentColorArgb, miuixColors) {
        if (settings.monetEnabled && accentColor == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            miuixColors.toMaterialColorScheme(dark)
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
    val relativeFontScale = when (settings.fontScaleMode) {
        FontScaleMode.ExtraSmall -> 0.82f
        FontScaleMode.Small -> 0.88f
        FontScaleMode.Compact -> 0.94f
        FontScaleMode.System -> 1f
        FontScaleMode.Large -> 1.08f
        FontScaleMode.XLarge -> 1.16f
        FontScaleMode.ExtraLarge -> 1.24f
    }
    val fontScale = density.fontScale * relativeFontScale
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
            small = RoundedCornerShape(14.dp),
            medium = RoundedCornerShape(22.dp),
            large = RoundedCornerShape(30.dp)
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
        MiuixTheme(controller = miuixController) {
            MaterialTheme(
                colorScheme = colorScheme,
                typography = typography,
                shapes = shapes,
                content = content
            )
        }
    }
}

private fun MiuixColors.toMaterialColorScheme(dark: Boolean) =
    (if (dark) darkColorScheme() else lightColorScheme()).copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = primaryVariant,
        onTertiary = onPrimaryVariant,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceContainerVariant,
        outline = outline,
        outlineVariant = dividerLine,
        surfaceContainerLowest = surface,
        surfaceContainerLow = surfaceContainer,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerHighest = surfaceContainerHighest,
    )
