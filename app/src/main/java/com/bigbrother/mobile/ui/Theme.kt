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

private val MaterialLightColorScheme = lightColorScheme(
    primary = Color(0xFF0B57D0),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD3E3FD),
    onPrimaryContainer = Color(0xFF041E49),
    secondary = Color(0xFF535F70),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD7E3F7),
    onSecondaryContainer = Color(0xFF101C2B),
    background = Color(0xFFF8F9FA),
    onBackground = Color(0xFF1F1F1F),
    surface = Color(0xFFF8F9FA),
    onSurface = Color(0xFF1F1F1F),
    surfaceVariant = Color(0xFFE1E2EC),
    onSurfaceVariant = Color(0xFF44474E),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF0F4F9),
    surfaceContainer = Color(0xFFEAEFF5),
    surfaceContainerHigh = Color(0xFFE1E6ED),
    surfaceContainerHighest = Color(0xFFD8DFE8),
    outline = Color(0xFF74777F),
    outlineVariant = Color(0xFFC4C6D0),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF)
)

private val MaterialDarkColorScheme = darkColorScheme(
    primary = Color(0xFFA8C7FA),
    onPrimary = Color(0xFF003062),
    primaryContainer = Color(0xFF00468A),
    onPrimaryContainer = Color(0xFFD3E3FD),
    secondary = Color(0xFFB9C8DA),
    onSecondary = Color(0xFF253140),
    secondaryContainer = Color(0xFF3B4758),
    onSecondaryContainer = Color(0xFFD7E3F7),
    background = Color(0xFF111318),
    onBackground = Color(0xFFE2E2E9),
    surface = Color(0xFF111318),
    onSurface = Color(0xFFE2E2E9),
    surfaceVariant = Color(0xFF44474E),
    onSurfaceVariant = Color(0xFFC4C6D0),
    surfaceContainerLowest = Color(0xFF0C0E13),
    surfaceContainerLow = Color(0xFF1D2024),
    surfaceContainer = Color(0xFF212328),
    surfaceContainerHigh = Color(0xFF2B2D33),
    surfaceContainerHighest = Color(0xFF36383E),
    outline = Color(0xFF8E9099),
    outlineVariant = Color(0xFF44474E),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

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

    val miuixLightColors = remember(accentColor) {
        top.yukonga.miuix.kmp.theme.lightColorScheme(
            background = Color(0xFFF2F2F7),
            surface = Color(0xFFFFFFFF),
            surfaceContainer = Color(0xFFFFFFFF),
            surfaceContainerHigh = Color(0xFFE8E8ED),
            surfaceContainerHighest = Color(0xFFE0E0E6),
            onBackground = Color(0xFF000000),
            onSurface = Color(0xFF000000),
            onSurfaceContainer = Color(0xFF000000),
            onSurfaceContainerVariant = Color(0xFF8C8C8E),
            secondaryContainer = Color(0xFFE5E5EA),
            onSecondaryContainer = Color(0xFF000000),
            primaryContainer = Color(0xFFD3E3FD),
            onPrimaryContainer = Color(0xFF041E49),
            outline = Color(0xFFE5E5EA),
            dividerLine = Color(0xFFE0E0E6),
            primary = accentColor ?: Color(0xFF3482FF),
            primaryVariant = accentColor ?: Color(0xFF3482FF)
        )
    }

    val miuixDarkColors = remember(accentColor) {
        top.yukonga.miuix.kmp.theme.darkColorScheme(
            background = Color(0xFF121212),
            surface = Color(0xFF1C1C1E),
            surfaceContainer = Color(0xFF1C1C1E),
            surfaceContainerHigh = Color(0xFF2C2C2E),
            surfaceContainerHighest = Color(0xFF3A3A3C),
            onBackground = Color(0xFFF2F2F2),
            onSurface = Color(0xFFF2F2F2),
            onSurfaceContainer = Color(0xFFF2F2F2),
            onSurfaceContainerVariant = Color(0xFF8E8E93),
            secondaryContainer = Color(0xFF2C2C2E),
            onSecondaryContainer = Color(0xFFF2F2F2),
            primaryContainer = Color(0xFF1E3A5F),
            onPrimaryContainer = Color(0xFFD3E3FD),
            tertiaryContainer = Color(0xFF2C2C2E),
            onTertiaryContainer = Color(0xFFF2F2F2),
            outline = Color(0xFF2C2C2E),
            dividerLine = Color(0xFF2C2C2E),
            primary = accentColor ?: Color(0xFF277AF7),
            primaryVariant = accentColor ?: Color(0xFF0073DD)
        )
    }

    val miuixController = remember(dark, settings.monetEnabled, accentColor, miuixLightColors, miuixDarkColors) {
        ThemeController(
            colorSchemeMode = when {
                settings.monetEnabled && dark -> ColorSchemeMode.MonetDark
                settings.monetEnabled -> ColorSchemeMode.MonetLight
                dark -> ColorSchemeMode.Dark
                else -> ColorSchemeMode.Light
            },
            lightColors = miuixLightColors,
            darkColors = miuixDarkColors,
            keyColor = accentColor.takeIf { settings.monetEnabled },
            isDark = dark,
        )
    }

    val miuixColors = miuixController.currentColors()
    val colorScheme = remember(context, dark, settings.monetEnabled, settings.uiStyle, accentColor, miuixColors) {
        if (settings.uiStyle == UiStyle.Material) {
            if (settings.monetEnabled) {
                if (accentColor == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                } else {
                    miuixColors.toMaterialColorScheme(dark)
                }
            } else {
                if (dark) MaterialDarkColorScheme else MaterialLightColorScheme
            }
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
            medium = RoundedCornerShape(20.dp),
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
                shapes = shapes
            ) {
                CompositionLocalProvider(
                    androidx.compose.material3.LocalContentColor provides colorScheme.onSurface,
                    content = content
                )
            }
        }
    }
}

private fun MiuixColors.toMaterialColorScheme(dark: Boolean) =
    (if (dark) MaterialDarkColorScheme else MaterialLightColorScheme).copy(
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
        surfaceContainerLowest = background,
        surfaceContainerLow = surfaceContainer,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerHighest = surfaceContainerHighest,
    )
