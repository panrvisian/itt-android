package com.bigbrother.mobile.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bigbrother.mobile.BigBrotherApp
import com.bigbrother.mobile.data.AppSettings
import com.bigbrother.mobile.data.AppRepository
import com.bigbrother.mobile.data.EventEntity
import com.bigbrother.mobile.data.EventGridColumns
import com.bigbrother.mobile.data.FontScaleMode
import com.bigbrother.mobile.data.GroupEntity
import com.bigbrother.mobile.data.NoteEditorState
import com.bigbrother.mobile.data.NoteViewState
import com.bigbrother.mobile.data.RecordEntity
import com.bigbrother.mobile.data.ThemeMode
import com.bigbrother.mobile.data.TotalDurationMode
import com.bigbrother.mobile.data.UiStyle
import com.bigbrother.mobile.data.WallpaperMode
import com.bigbrother.mobile.domain.StatsRangeKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class AppTab {
    Home,
    Timeline,
    Stats,
    Notes,
    Settings
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AppRepository = (application as BigBrotherApp).container.repository

    val groups = repository.groups.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val events = repository.events.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val records = repository.records.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val noteImages = repository.noteImages.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val settings = repository.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())
    val onboardingCompleted = repository.onboardingCompleted
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _selectedTab = MutableStateFlow(AppTab.Home)
    val selectedTab: StateFlow<AppTab> = _selectedTab.asStateFlow()

    private val _statsRange = MutableStateFlow(StatsRangeKind.Today)
    val statsRange: StateFlow<StatsRangeKind> = _statsRange.asStateFlow()

    private val _statsDate = MutableStateFlow(LocalDate.now())
    val statsDate: StateFlow<LocalDate> = _statsDate.asStateFlow()

    private val _timelineDate = MutableStateFlow(LocalDate.now())
    val timelineDate: StateFlow<LocalDate> = _timelineDate.asStateFlow()

    private val _notesDate = MutableStateFlow(LocalDate.now())
    val notesDate: StateFlow<LocalDate> = _notesDate.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initialize()
        }
    }

    fun selectTab(tab: AppTab) {
        _selectedTab.value = tab
    }

    fun completeOnboarding() {
        viewModelScope.launch { repository.setOnboardingCompleted(true) }
    }

    fun setStatsRange(range: StatsRangeKind) {
        _statsRange.value = range
    }

    fun setStatsDate(date: LocalDate) {
        _statsDate.value = date
    }

    fun setTimelineDate(date: LocalDate) {
        _timelineDate.value = date
    }

    fun setNotesDate(date: LocalDate) {
        _notesDate.value = date
    }

    fun addGroup(name: String, colorArgb: Int) {
        viewModelScope.launch { repository.addGroup(name.trim(), colorArgb) }
    }

    fun addEvent(groupId: String, name: String) {
        viewModelScope.launch { repository.addEvent(groupId, name.trim()) }
    }

    fun renameGroup(groupId: String, name: String) {
        viewModelScope.launch { repository.renameGroup(groupId, name.trim()) }
    }

    fun changeGroupColor(groupId: String, colorArgb: Int) {
        viewModelScope.launch { repository.changeGroupColor(groupId, colorArgb) }
    }

    fun moveGroup(groupId: String, direction: Int) {
        viewModelScope.launch { repository.moveGroup(groupId, direction) }
    }

    fun renameEvent(eventId: String, name: String) {
        viewModelScope.launch { repository.renameEvent(eventId, name.trim()) }
    }

    fun moveEvent(eventId: String, groupId: String) {
        viewModelScope.launch { repository.moveEvent(eventId, groupId) }
    }

    fun toggleFavorite(eventId: String, favorite: Boolean) {
        viewModelScope.launch { repository.toggleFavorite(eventId, favorite) }
    }

    fun deleteGroup(groupId: String) {
        viewModelScope.launch { repository.deleteGroup(groupId) }
    }

    fun deleteEvent(eventId: String) {
        viewModelScope.launch { repository.deleteEvent(eventId) }
    }

    fun startEvent(eventId: String) {
        viewModelScope.launch { repository.startEvent(eventId) }
    }

    fun addManualRecord(eventId: String, startTime: Long, endTime: Long) {
        viewModelScope.launch { repository.addManualRecord(eventId, startTime, endTime) }
    }

    fun cloneRecord(recordId: String, eventId: String) {
        viewModelScope.launch { repository.cloneRecord(recordId, eventId) }
    }

    fun endRecord(recordId: String) {
        viewModelScope.launch { repository.endRecord(recordId) }
    }

    fun deleteRunningRecord(recordId: String) {
        viewModelScope.launch { repository.deleteRunningRecord(recordId) }
    }

    fun updateRecord(recordId: String, eventId: String, startTime: Long, endTime: Long?) {
        viewModelScope.launch { repository.updateRecord(recordId, eventId, startTime, endTime) }
    }

    fun deleteHistoryRecord(recordId: String) {
        viewModelScope.launch { repository.deleteHistoryRecord(recordId) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.setSettings { it.copy(themeMode = mode) } }
    }

    fun setUiStyle(style: UiStyle) {
        viewModelScope.launch { repository.setSettings { it.copy(uiStyle = style) } }
    }

    fun setMonetEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setSettings { it.copy(monetEnabled = enabled) } }
    }

    fun setAccentColor(colorArgb: Int?) {
        viewModelScope.launch { repository.setSettings { it.copy(accentColorArgb = colorArgb) } }
    }

    fun setFloatingBottomBarEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setSettings { it.copy(floatingBottomBarEnabled = enabled) } }
    }

    fun setLiquidGlassBottomBarEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setSettings { it.copy(liquidGlassBottomBarEnabled = enabled) } }
    }

    fun setFontScaleMode(mode: FontScaleMode) {
        viewModelScope.launch { repository.setSettings { it.copy(fontScaleMode = mode) } }
    }

    fun setShowClockSection(show: Boolean) {
        viewModelScope.launch { repository.setSettings { it.copy(showClockSection = show) } }
    }

    fun setShowRunningSection(show: Boolean) {
        viewModelScope.launch { repository.setSettings { it.copy(showRunningSection = show) } }
    }

    fun setShowFavoriteSection(show: Boolean) {
        viewModelScope.launch { repository.setSettings { it.copy(showFavoriteSection = show) } }
    }

    fun setShowGroupedSection(show: Boolean) {
        viewModelScope.launch { repository.setSettings { it.copy(showGroupedSection = show) } }
    }

    fun setEventGridColumns(columns: EventGridColumns) {
        viewModelScope.launch { repository.setSettings { it.copy(eventGridColumns = columns) } }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setSettings { it.copy(vibrationEnabled = enabled) } }
    }

    fun setFavoriteAutoFillCount(count: Int) {
        viewModelScope.launch { repository.setSettings { it.copy(favoriteAutoFillCount = count.coerceIn(1, 12)) } }
    }

    fun setShowDateInClock(show: Boolean) {
        viewModelScope.launch { repository.setSettings { it.copy(showDateInClock = show) } }
    }

    fun setUse24Hour(use24Hour: Boolean) {
        viewModelScope.launch { repository.setSettings { it.copy(use24Hour = use24Hour) } }
    }

    fun dismissHomeHint() {
        viewModelScope.launch { repository.setSettings { it.copy(homeHintDismissed = true) } }
    }

    fun setTotalDurationMode(mode: TotalDurationMode) {
        viewModelScope.launch { repository.setSettings { it.copy(totalDurationMode = mode) } }
    }

    fun setSemesterStartDate(date: LocalDate) {
        viewModelScope.launch { repository.setSettings { it.copy(semesterStartDate = date) } }
    }

    fun setSemesterWeeks(weeks: Int) {
        viewModelScope.launch { repository.setSettings { it.copy(semesterWeeks = weeks.coerceIn(4, 52)) } }
    }

    fun setWallpaperUri(uri: String?) {
        viewModelScope.launch { repository.setSettings { it.copy(wallpaperMode = WallpaperMode.Image, wallpaperUri = uri) } }
    }

    fun setWallpaperDefault() {
        viewModelScope.launch { repository.setSettings { it.copy(wallpaperMode = WallpaperMode.Default, wallpaperUri = null) } }
    }

    fun setWallpaperSolidColor(colorArgb: Int) {
        viewModelScope.launch { repository.setSettings { it.copy(wallpaperMode = WallpaperMode.Solid, wallpaperSolidColorArgb = colorArgb) } }
    }

    fun setWallpaperPortrait(scale: Float, offsetX: Float, offsetY: Float) {
        viewModelScope.launch {
            repository.setSettings {
                it.copy(
                    wallpaperPortraitScale = scale.coerceIn(1f, 3f),
                    wallpaperPortraitOffsetX = offsetX.coerceIn(-1f, 1f),
                    wallpaperPortraitOffsetY = offsetY.coerceIn(-1f, 1f)
                )
            }
        }
    }

    fun setWallpaperLandscape(scale: Float, offsetX: Float, offsetY: Float) {
        viewModelScope.launch {
            repository.setSettings {
                it.copy(
                    wallpaperLandscapeScale = scale.coerceIn(1f, 3f),
                    wallpaperLandscapeOffsetX = offsetX.coerceIn(-1f, 1f),
                    wallpaperLandscapeOffsetY = offsetY.coerceIn(-1f, 1f)
                )
            }
        }
    }

    fun setComponentAlpha(alpha: Float) {
        viewModelScope.launch { repository.setSettings { it.copy(componentAlpha = alpha.coerceIn(0f, 1f)) } }
    }

    fun setGlassEffectEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setSettings { it.copy(glassEffectEnabled = enabled) } }
    }

    fun setWallpaperBlurRadius(radius: Float) {
        viewModelScope.launch { repository.setSettings { it.copy(wallpaperBlurRadius = radius.coerceIn(0f, 40f)) } }
    }

    fun exportCsv(uri: Uri) {
        viewModelScope.launch { repository.exportCsv(uri) }
    }

    fun importCsv(uri: Uri, merge: Boolean) {
        viewModelScope.launch { repository.importCsv(uri, merge) }
    }

    fun noteImageFile(recordId: String, fileName: String): java.io.File =
        repository.noteImageFile(recordId, fileName)

    fun draftImageFile(recordId: String, fileName: String): java.io.File =
        repository.draftImageFile(recordId, fileName)

    suspend fun loadNoteView(recordId: String): NoteViewState = repository.loadNoteView(recordId)

    suspend fun loadNoteEditor(recordId: String): NoteEditorState = repository.loadNoteEditor(recordId)

    suspend fun copyNoteImageToDraft(recordId: String, uri: Uri): String? =
        repository.copyImageToDraft(recordId, uri)

    suspend fun saveNoteDraft(recordId: String, text: String, imageNames: List<String>) {
        repository.saveNoteDraft(recordId, text, imageNames)
    }

    suspend fun saveNote(recordId: String, text: String, imageNames: List<String>) {
        repository.saveNote(recordId, text, imageNames)
    }

    suspend fun removeDraftImageFile(recordId: String, fileName: String) {
        repository.removeDraftImageFile(recordId, fileName)
    }
}

class MainViewModelFactory(
    private val application: BigBrotherApp
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(application) as T
    }
}



