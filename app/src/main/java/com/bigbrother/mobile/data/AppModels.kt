package com.bigbrother.mobile.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID

enum class ThemeMode { System, Light, Dark }
enum class FontScaleMode { System, Small, Medium, Large, XLarge }
enum class EventGridColumns { Auto, Two, Three, Four }
enum class TotalDurationMode { Sum, Unique }
enum class WallpaperMode { Default, Image, Solid }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val fontScaleMode: FontScaleMode = FontScaleMode.System,
    val showClockSection: Boolean = true,
    val showRunningSection: Boolean = true,
    val showFavoriteSection: Boolean = true,
    val showGroupedSection: Boolean = true,
    val eventGridColumns: EventGridColumns = EventGridColumns.Auto,
    val vibrationEnabled: Boolean = true,
    val favoriteAutoFillCount: Int = 6,
    val showDateInClock: Boolean = true,
    val use24Hour: Boolean = true,
    val totalDurationMode: TotalDurationMode = TotalDurationMode.Sum,
    val semesterStartDate: LocalDate = LocalDate.of(LocalDate.now().year, 9, 1),
    val weekStartDay: DayOfWeek = DayOfWeek.MONDAY,
    val semesterWeeks: Int = 18,
    val wallpaperMode: WallpaperMode = WallpaperMode.Default,
    val wallpaperUri: String? = null,
    val wallpaperSolidColorArgb: Int = 0xFFFFFFFF.toInt(),
    val wallpaperPortraitScale: Float = 1f,
    val wallpaperPortraitOffsetX: Float = 0f,
    val wallpaperPortraitOffsetY: Float = 0.08f,
    val wallpaperLandscapeScale: Float = 1f,
    val wallpaperLandscapeOffsetX: Float = 0f,
    val wallpaperLandscapeOffsetY: Float = 0f,
    val componentAlpha: Float = 0.88f
)

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey val id: String = newId(),
    val name: String,
    val colorArgb: Int,
    val isSystem: Boolean = false,
    val isDeleted: Boolean = false,
    val sortOrder: Int = 0
)

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String = newId(),
    val groupId: String,
    val name: String,
    val isDeleted: Boolean = false,
    val isFavorite: Boolean = false,
    val sortOrder: Int = 0
)

@Entity(tableName = "records")
data class RecordEntity(
    @PrimaryKey val id: String = newId(),
    val eventId: String,
    val eventNameSnapshot: String,
    val groupIdSnapshot: String,
    val groupNameSnapshot: String,
    val groupColorArgbSnapshot: Int,
    val startTime: Long,
    val endTime: Long? = null,
    val isContinuation: Boolean = false,
    val noteText: String = ""
)

@Entity(tableName = "note_images")
data class NoteImageEntity(
    @PrimaryKey val id: String = newId(),
    val recordId: String,
    val fileName: String,
    val sortOrder: Int = 0
)

fun newId(): String = UUID.randomUUID().toString().replace("-", "")

data class AppBundle(
    val settings: AppSettings = AppSettings(),
    val groups: List<GroupEntity> = emptyList(),
    val events: List<EventEntity> = emptyList(),
    val records: List<RecordEntity> = emptyList(),
    val noteImages: List<NoteImageEntity> = emptyList()
)



