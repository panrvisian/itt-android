package com.bigbrother.mobile.data

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

private val Context.settingsDataStore by preferencesDataStore("settings")

class SettingsStore(private val context: Context) {
    private val ds = context.settingsDataStore

    val flow: Flow<AppSettings> = ds.data.map { pref -> pref.toSettings() }
    val onboardingCompleted: Flow<Boolean> = ds.data.map { pref ->
        pref[booleanPreferencesKey(KEY_ONBOARDING_COMPLETED)] ?: false
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        ds.edit { pref ->
            pref[booleanPreferencesKey(KEY_ONBOARDING_COMPLETED)] = completed
        }
    }

    suspend fun update(block: (AppSettings) -> AppSettings) {
        ds.edit { pref ->
            val next = block(pref.toSettings())
            pref.putSettings(next)
        }
    }

    suspend fun set(settings: AppSettings) {
        ds.edit { it.putSettings(settings) }
    }

    private fun Preferences.toSettings(): AppSettings {
        val year = this[intPreferencesKey(KEY_SEMESTER_YEAR)] ?: LocalDate.now().year
        val month = (this[intPreferencesKey(KEY_SEMESTER_MONTH)] ?: 9).coerceIn(1, 12)
        val day = (this[intPreferencesKey(KEY_SEMESTER_DAY)] ?: 1).coerceIn(1, YearMonth.of(year, month).lengthOfMonth())
        return AppSettings(
            themeMode = when (this[stringPreferencesKey(KEY_THEME_MODE)]) {
                "light" -> ThemeMode.Light
                "dark" -> ThemeMode.Dark
                else -> ThemeMode.System
            },
            fontScaleMode = when (this[stringPreferencesKey(KEY_FONT_MODE)]) {
                "small" -> FontScaleMode.Small
                "medium" -> FontScaleMode.Medium
                "large" -> FontScaleMode.Large
                "xlarge" -> FontScaleMode.XLarge
                else -> FontScaleMode.System
            },
            showClockSection = this[booleanPreferencesKey(KEY_SHOW_CLOCK)] ?: true,
            showRunningSection = this[booleanPreferencesKey(KEY_SHOW_RUNNING)] ?: true,
            showFavoriteSection = this[booleanPreferencesKey(KEY_SHOW_FAVORITE)] ?: true,
            showGroupedSection = this[booleanPreferencesKey(KEY_SHOW_GROUPED)] ?: true,
            eventGridColumns = when (this[stringPreferencesKey(KEY_GRID_COLUMNS)]) {
                "2" -> EventGridColumns.Two
                "3" -> EventGridColumns.Three
                "4" -> EventGridColumns.Four
                else -> EventGridColumns.Auto
            },
            vibrationEnabled = this[booleanPreferencesKey(KEY_VIBRATION)] ?: true,
            favoriteAutoFillCount = this[intPreferencesKey(KEY_FAVORITE_FILL)] ?: 6,
            showDateInClock = this[booleanPreferencesKey(KEY_SHOW_DATE)] ?: true,
            use24Hour = this[booleanPreferencesKey(KEY_USE_24H)] ?: true,
            totalDurationMode = when (this[stringPreferencesKey(KEY_DURATION_MODE)]) {
                "unique" -> TotalDurationMode.Unique
                else -> TotalDurationMode.Sum
            },
            semesterStartDate = LocalDate.of(year, month, day),
            weekStartDay = DayOfWeek.of(this[intPreferencesKey(KEY_WEEK_START)] ?: 1),
            semesterWeeks = this[intPreferencesKey(KEY_SEMESTER_WEEKS)] ?: 18,
            wallpaperMode = when (this[stringPreferencesKey(KEY_WALLPAPER_MODE)]) {
                "image" -> WallpaperMode.Image
                "solid" -> WallpaperMode.Solid
                else -> WallpaperMode.Default
            },
            wallpaperUri = this[stringPreferencesKey(KEY_WALLPAPER_URI)]?.takeIf { it.isNotBlank() },
            wallpaperSolidColorArgb = this[intPreferencesKey(KEY_WALLPAPER_SOLID_COLOR)] ?: 0xFFFFFFFF.toInt(),
            wallpaperPortraitScale = (this[floatPreferencesKey(KEY_WALLPAPER_PORTRAIT_SCALE)] ?: 1f).coerceIn(1f, 3f),
            wallpaperPortraitOffsetX = this[floatPreferencesKey(KEY_WALLPAPER_PORTRAIT_OFFSET_X)] ?: 0f,
            wallpaperPortraitOffsetY = this[floatPreferencesKey(KEY_WALLPAPER_PORTRAIT_OFFSET_Y)] ?: 0.08f,
            wallpaperLandscapeScale = (this[floatPreferencesKey(KEY_WALLPAPER_LANDSCAPE_SCALE)] ?: 1f).coerceIn(1f, 3f),
            wallpaperLandscapeOffsetX = this[floatPreferencesKey(KEY_WALLPAPER_LANDSCAPE_OFFSET_X)] ?: 0f,
            wallpaperLandscapeOffsetY = this[floatPreferencesKey(KEY_WALLPAPER_LANDSCAPE_OFFSET_Y)] ?: 0f,
            componentAlpha = (this[floatPreferencesKey(KEY_COMPONENT_ALPHA)] ?: 0.88f).coerceIn(0f, 1f),
            glassEffectEnabled = this[booleanPreferencesKey(KEY_GLASS_EFFECT)] ?: false,
            wallpaperBlurRadius = (this[floatPreferencesKey(KEY_WALLPAPER_BLUR_RADIUS)] ?: 22f).coerceIn(0f, 40f)
        )
    }

    private fun MutablePreferences.putSettings(settings: AppSettings) {
        this[stringPreferencesKey(KEY_THEME_MODE)] = when (settings.themeMode) {
            ThemeMode.Light -> "light"
            ThemeMode.Dark -> "dark"
            ThemeMode.System -> "system"
        }
        this[stringPreferencesKey(KEY_FONT_MODE)] = when (settings.fontScaleMode) {
            FontScaleMode.Small -> "small"
            FontScaleMode.Medium -> "medium"
            FontScaleMode.Large -> "large"
            FontScaleMode.XLarge -> "xlarge"
            FontScaleMode.System -> "system"
        }
        this[booleanPreferencesKey(KEY_SHOW_CLOCK)] = settings.showClockSection
        this[booleanPreferencesKey(KEY_SHOW_RUNNING)] = settings.showRunningSection
        this[booleanPreferencesKey(KEY_SHOW_FAVORITE)] = settings.showFavoriteSection
        this[booleanPreferencesKey(KEY_SHOW_GROUPED)] = settings.showGroupedSection
        this[stringPreferencesKey(KEY_GRID_COLUMNS)] = when (settings.eventGridColumns) {
            EventGridColumns.Two -> "2"
            EventGridColumns.Three -> "3"
            EventGridColumns.Four -> "4"
            EventGridColumns.Auto -> "auto"
        }
        this[booleanPreferencesKey(KEY_VIBRATION)] = settings.vibrationEnabled
        this[intPreferencesKey(KEY_FAVORITE_FILL)] = settings.favoriteAutoFillCount
        this[booleanPreferencesKey(KEY_SHOW_DATE)] = settings.showDateInClock
        this[booleanPreferencesKey(KEY_USE_24H)] = settings.use24Hour
        this[stringPreferencesKey(KEY_DURATION_MODE)] = when (settings.totalDurationMode) {
            TotalDurationMode.Sum -> "sum"
            TotalDurationMode.Unique -> "unique"
        }
        this[intPreferencesKey(KEY_SEMESTER_YEAR)] = settings.semesterStartDate.year
        this[intPreferencesKey(KEY_SEMESTER_MONTH)] = settings.semesterStartDate.monthValue
        this[intPreferencesKey(KEY_SEMESTER_DAY)] = settings.semesterStartDate.dayOfMonth
        this[intPreferencesKey(KEY_WEEK_START)] = settings.weekStartDay.value
        this[intPreferencesKey(KEY_SEMESTER_WEEKS)] = settings.semesterWeeks
        this[stringPreferencesKey(KEY_WALLPAPER_MODE)] = when (settings.wallpaperMode) {
            WallpaperMode.Default -> "default"
            WallpaperMode.Image -> "image"
            WallpaperMode.Solid -> "solid"
        }
        this[stringPreferencesKey(KEY_WALLPAPER_URI)] = settings.wallpaperUri.orEmpty()
        this[intPreferencesKey(KEY_WALLPAPER_SOLID_COLOR)] = settings.wallpaperSolidColorArgb
        this[floatPreferencesKey(KEY_WALLPAPER_PORTRAIT_SCALE)] = settings.wallpaperPortraitScale
        this[floatPreferencesKey(KEY_WALLPAPER_PORTRAIT_OFFSET_X)] = settings.wallpaperPortraitOffsetX
        this[floatPreferencesKey(KEY_WALLPAPER_PORTRAIT_OFFSET_Y)] = settings.wallpaperPortraitOffsetY
        this[floatPreferencesKey(KEY_WALLPAPER_LANDSCAPE_SCALE)] = settings.wallpaperLandscapeScale
        this[floatPreferencesKey(KEY_WALLPAPER_LANDSCAPE_OFFSET_X)] = settings.wallpaperLandscapeOffsetX
        this[floatPreferencesKey(KEY_WALLPAPER_LANDSCAPE_OFFSET_Y)] = settings.wallpaperLandscapeOffsetY
        this[floatPreferencesKey(KEY_COMPONENT_ALPHA)] = settings.componentAlpha.coerceIn(0f, 1f)
        this[booleanPreferencesKey(KEY_GLASS_EFFECT)] = settings.glassEffectEnabled
        this[floatPreferencesKey(KEY_WALLPAPER_BLUR_RADIUS)] = settings.wallpaperBlurRadius.coerceIn(0f, 40f)
    }

    companion object {
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_FONT_MODE = "font_mode"
        private const val KEY_SHOW_CLOCK = "show_clock"
        private const val KEY_SHOW_RUNNING = "show_running"
        private const val KEY_SHOW_FAVORITE = "show_favorite"
        private const val KEY_SHOW_GROUPED = "show_grouped"
        private const val KEY_GRID_COLUMNS = "grid_columns"
        private const val KEY_VIBRATION = "vibration"
        private const val KEY_FAVORITE_FILL = "favorite_fill"
        private const val KEY_SHOW_DATE = "show_date"
        private const val KEY_USE_24H = "use_24h"
        private const val KEY_DURATION_MODE = "duration_mode"
        private const val KEY_SEMESTER_YEAR = "semester_year"
        private const val KEY_SEMESTER_MONTH = "semester_month"
        private const val KEY_SEMESTER_DAY = "semester_day"
        private const val KEY_WEEK_START = "week_start"
        private const val KEY_SEMESTER_WEEKS = "semester_weeks"
        private const val KEY_WALLPAPER_MODE = "wallpaper_mode"
        private const val KEY_WALLPAPER_URI = "wallpaper_uri"
        private const val KEY_WALLPAPER_SOLID_COLOR = "wallpaper_solid_color"
        private const val KEY_WALLPAPER_PORTRAIT_SCALE = "wallpaper_portrait_scale"
        private const val KEY_WALLPAPER_PORTRAIT_OFFSET_X = "wallpaper_portrait_offset_x"
        private const val KEY_WALLPAPER_PORTRAIT_OFFSET_Y = "wallpaper_portrait_offset_y"
        private const val KEY_WALLPAPER_LANDSCAPE_SCALE = "wallpaper_landscape_scale"
        private const val KEY_WALLPAPER_LANDSCAPE_OFFSET_X = "wallpaper_landscape_offset_x"
        private const val KEY_WALLPAPER_LANDSCAPE_OFFSET_Y = "wallpaper_landscape_offset_y"
        private const val KEY_COMPONENT_ALPHA = "component_alpha"
        private const val KEY_GLASS_EFFECT = "glass_effect"
        private const val KEY_WALLPAPER_BLUR_RADIUS = "wallpaper_blur_radius"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    }
}
