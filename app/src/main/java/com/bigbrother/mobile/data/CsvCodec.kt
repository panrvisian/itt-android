package com.bigbrother.mobile.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.bigbrother.mobile.domain.TimeUtils

object CsvCodec {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun export(bundle: AppBundle): String {
        val s = bundle.settings
        val sb = StringBuilder()
        sb.appendLine("#BIGBROTHER_EXPORT_V1")
        sb.appendLine("[SETTINGS]")
        sb.appendLine("theme,${csvField(mapThemeForWindows(s.themeMode))}")
        sb.appendLine("use24hour,${s.use24Hour}")
        sb.appendLine("clockshowdate,${s.showDateInClock}")
        sb.appendLine("quickcount,${s.favoriteAutoFillCount}")
        sb.appendLine("fontsize,${csvField(mapFontForWindows(s.fontScaleMode))}")
        sb.appendLine("autostart,false")
        sb.appendLine("closetotray,false")
        sb.appendLine("silentstart,false")
        sb.appendLine("durationmode,${csvField(if (s.totalDurationMode == TotalDurationMode.Unique) "unique" else "sum")}")
        sb.appendLine("semesterstart,${dateFormatter.format(s.semesterStartDate)}")
        sb.appendLine("semesterweeks,${s.semesterWeeks}")
        sb.appendLine("rememberwindow,false")
        sb.appendLine("rememberlayout,false")
        sb.appendLine("mainsplitpercent,50")
        sb.appendLine("rightsplitpercent,33")
        sb.appendLine("historysplitpercent,55")
        sb.appendLine("windowbounds,100,100,1280,800")
        sb.appendLine("[ANDROID]")
        sb.appendLine("theme_mode,${csvField(s.themeMode.name.lowercase())}")
        sb.appendLine("ui_style,${csvField(s.uiStyle.name.lowercase())}")
        sb.appendLine("monet_enabled,${s.monetEnabled}")
        sb.appendLine("accent_color,${s.accentColorArgb?.toString().orEmpty()}")
        sb.appendLine("floating_bottom_bar,${s.floatingBottomBarEnabled}")
        sb.appendLine("liquid_glass_bottom_bar,${s.liquidGlassBottomBarEnabled}")
        sb.appendLine("font_mode,${csvField(s.fontScaleMode.name.lowercase())}")
        sb.appendLine("show_clock,${s.showClockSection}")
        sb.appendLine("show_running,${s.showRunningSection}")
        sb.appendLine("show_favorite,${s.showFavoriteSection}")
        sb.appendLine("show_grouped,${s.showGroupedSection}")
        sb.appendLine("grid_columns,${csvField(when (s.eventGridColumns) {
            EventGridColumns.Two -> "2"
            EventGridColumns.Three -> "3"
            EventGridColumns.Four -> "4"
            EventGridColumns.Auto -> "auto"
        })}")
        sb.appendLine("vibration,${s.vibrationEnabled}")
        sb.appendLine("favorite_fill,${s.favoriteAutoFillCount}")
        sb.appendLine("show_date,${s.showDateInClock}")
        sb.appendLine("use_24h,${s.use24Hour}")
        sb.appendLine("duration_mode,${csvField(if (s.totalDurationMode == TotalDurationMode.Unique) "unique" else "sum")}")
        sb.appendLine("semester_date,${dateFormatter.format(s.semesterStartDate)}")
        sb.appendLine("week_start,${s.weekStartDay.value}")
        sb.appendLine("semester_weeks,${s.semesterWeeks}")
        sb.appendLine("[GROUPS]")
        sb.appendLine("id,name,color,is_system,deleted")
        bundle.groups.forEach { g ->
            sb.appendLine(listOf(g.id, g.name, g.colorArgb.toString(), g.isSystem.toString(), g.isDeleted.toString()).joinToString(",") { csvField(it) })
        }
        sb.appendLine("[EVENTS]")
        sb.appendLine("id,group_id,name,deleted,favorite,sort_order")
        bundle.events.forEach { e ->
            sb.appendLine(listOf(e.id, e.groupId, e.name, e.isDeleted.toString(), e.isFavorite.toString(), e.sortOrder.toString()).joinToString(",") { csvField(it) })
        }
        sb.appendLine("[RECORDS]")
        sb.appendLine("id,event_id,start,end,is_continuation,event_name,group_id,group_name,group_color,note")
        bundle.records.forEach { r ->
            sb.appendLine(
                listOf(
                    r.id,
                    r.eventId,
                    dateTimeFormatter.format(TimeUtils.toLocalDateTime(r.startTime)),
                    r.endTime?.let { dateTimeFormatter.format(TimeUtils.toLocalDateTime(it)) } ?: "",
                    r.isContinuation.toString(),
                    r.eventNameSnapshot,
                    r.groupIdSnapshot,
                    r.groupNameSnapshot,
                    r.groupColorArgbSnapshot.toString(),
                    r.noteText
                ).joinToString(",") { csvField(it) }
            )
        }
        sb.appendLine("[NOTE_IMAGES]")
        sb.appendLine("record_id,file_name,sort_order")
        bundle.noteImages.forEach { image ->
            sb.appendLine(listOf(image.recordId, image.fileName, image.sortOrder.toString()).joinToString(",") { csvField(it) })
        }
        return sb.toString()
    }

    fun parse(text: String): AppBundle {
        var section = ""
        var settings = AppSettings()
        val groups = mutableListOf<GroupEntity>()
        val events = mutableListOf<EventEntity>()
        val records = mutableListOf<RecordEntity>()
        val noteImages = mutableListOf<NoteImageEntity>()
        for (parts in csvRecordSequence(text)) {
            if (parts.isEmpty() || parts.all { it.isBlank() }) continue
            if (parts.size == 1 && parts[0].startsWith("[") && parts[0].endsWith("]")) {
                section = parts[0].substring(1, parts[0].length - 1).uppercase()
                continue
            }
            if (parts[0].startsWith("#")) continue
            when (section) {
                "SETTINGS" -> settings = applyWindowsSetting(parts, settings)
                "ANDROID" -> settings = applyAndroidSetting(parts, settings)
                "GROUPS" -> if (parts.size >= 5 && parts[0] != "id") {
                    groups += GroupEntity(
                        id = parts[0],
                        name = parts[1],
                        colorArgb = parts[2].toIntOrNull() ?: 0,
                        isSystem = parseBool(parts[3]) ?: false,
                        isDeleted = parseBool(parts[4]) ?: false,
                        sortOrder = groups.size
                    )
                }
                "EVENTS" -> if (parts.size >= 4 && parts[0] != "id") {
                    events += EventEntity(
                        id = parts[0],
                        groupId = parts[1],
                        name = parts[2],
                        isDeleted = parseBool(parts[3]) ?: false,
                        isFavorite = parts.getOrNull(4)?.let { parseBool(it) } ?: false,
                        sortOrder = parts.getOrNull(5)?.toIntOrNull() ?: events.size
                    )
                }
                "RECORDS" -> if (parts.size >= 5 && parts[0] != "id") {
                    val start = TimeUtils.parseDateTime(parts[2])
                    if (start != null) {
                        val end = parts[3].takeIf { it.isNotBlank() }?.let { TimeUtils.parseDateTime(it) }
                        val event = events.firstOrNull { it.id == parts[1] }
                        val group = groups.firstOrNull { it.id == event?.groupId }
                        records += RecordEntity(
                            id = parts[0],
                            eventId = parts[1],
                            startTime = start,
                            endTime = end,
                            isContinuation = parseBool(parts[4]) ?: false,
                            eventNameSnapshot = parts.getOrNull(5) ?: event?.name ?: "已删除事件",
                            groupIdSnapshot = parts.getOrNull(6) ?: group?.id ?: event?.groupId.orEmpty(),
                            groupNameSnapshot = parts.getOrNull(7) ?: group?.name ?: "未分组",
                            groupColorArgbSnapshot = parts.getOrNull(8)?.toIntOrNull() ?: group?.colorArgb ?: 0xFF9E9E9E.toInt(),
                            noteText = parts.getOrNull(9) ?: ""
                        )
                    }
                }
                "NOTE_IMAGES" -> if (parts.size >= 3 && parts[0] != "record_id") {
                    noteImages += NoteImageEntity(
                        recordId = parts[0],
                        fileName = parts[1],
                        sortOrder = parts[2].toIntOrNull() ?: noteImages.size
                    )
                }
            }
        }
        if (groups.none { it.isSystem }) {
            groups.add(0, GroupEntity(name = "未分组", colorArgb = 0xFF9E9E9E.toInt(), isSystem = true, isDeleted = false))
        }
        return AppBundle(settings = settings, groups = groups, events = events, records = records, noteImages = noteImages)
    }

    private fun applyWindowsSetting(parts: List<String>, settings: AppSettings): AppSettings {
        if (parts.size < 2) return settings
        return when (parts[0].lowercase()) {
            "theme" -> settings.copy(themeMode = if (parts[1].equals("dark", true)) ThemeMode.Dark else ThemeMode.Light)
            "use24hour" -> settings.copy(use24Hour = parseBool(parts[1]) ?: settings.use24Hour)
            "clockshowdate" -> settings.copy(showDateInClock = parseBool(parts[1]) ?: settings.showDateInClock)
            "quickcount" -> settings.copy(favoriteAutoFillCount = parts[1].toIntOrNull() ?: settings.favoriteAutoFillCount)
            "fontsize" -> settings.copy(fontScaleMode = mapFontFromWindows(parts[1]))
            "durationmode" -> settings.copy(totalDurationMode = if (parts[1].equals("unique", true)) TotalDurationMode.Unique else TotalDurationMode.Sum)
            "semesterstart" -> settings.copy(semesterStartDate = LocalDate.parse(parts[1], dateFormatter))
            "semesterweeks" -> settings.copy(semesterWeeks = parts[1].toIntOrNull() ?: settings.semesterWeeks)
            else -> settings
        }
    }

    private fun applyAndroidSetting(parts: List<String>, settings: AppSettings): AppSettings {
        if (parts.size < 2) return settings
        return when (parts[0].lowercase()) {
            "theme_mode" -> settings.copy(themeMode = mapTheme(parts[1]))
            "ui_style" -> settings.copy(uiStyle = if (parts[1].equals("material", true)) UiStyle.Material else UiStyle.Miuix)
            "monet_enabled" -> settings.copy(monetEnabled = parseBool(parts[1]) ?: settings.monetEnabled)
            "accent_color" -> settings.copy(accentColorArgb = parts[1].toIntOrNull())
            "floating_bottom_bar" -> settings.copy(floatingBottomBarEnabled = parts[1].toBooleanStrictOrNull() ?: true)
            "liquid_glass_bottom_bar" -> settings.copy(liquidGlassBottomBarEnabled = parts[1].toBooleanStrictOrNull() ?: true)
            "font_mode" -> settings.copy(fontScaleMode = mapFont(parts[1]))
            "show_clock" -> settings.copy(showClockSection = parseBool(parts[1]) ?: settings.showClockSection)
            "show_running" -> settings.copy(showRunningSection = parseBool(parts[1]) ?: settings.showRunningSection)
            "show_favorite" -> settings.copy(showFavoriteSection = parseBool(parts[1]) ?: settings.showFavoriteSection)
            "show_grouped" -> settings.copy(showGroupedSection = parseBool(parts[1]) ?: settings.showGroupedSection)
            "grid_columns" -> settings.copy(eventGridColumns = mapColumns(parts[1]))
            "vibration" -> settings.copy(vibrationEnabled = parseBool(parts[1]) ?: settings.vibrationEnabled)
            "favorite_fill" -> settings.copy(favoriteAutoFillCount = parts[1].toIntOrNull() ?: settings.favoriteAutoFillCount)
            "show_date" -> settings.copy(showDateInClock = parseBool(parts[1]) ?: settings.showDateInClock)
            "use_24h" -> settings.copy(use24Hour = parseBool(parts[1]) ?: settings.use24Hour)
            "duration_mode" -> settings.copy(totalDurationMode = if (parts[1].equals("unique", true)) TotalDurationMode.Unique else TotalDurationMode.Sum)
            "semester_date" -> settings.copy(semesterStartDate = LocalDate.parse(parts[1], dateFormatter))
            "week_start" -> settings.copy(weekStartDay = DayOfWeek.of(parts[1].toIntOrNull() ?: settings.weekStartDay.value))
            "semester_weeks" -> settings.copy(semesterWeeks = parts[1].toIntOrNull() ?: settings.semesterWeeks)
            else -> settings
        }
    }

    private fun mapTheme(value: String): ThemeMode = when (value.lowercase()) {
        "light" -> ThemeMode.Light
        "dark" -> ThemeMode.Dark
        else -> ThemeMode.System
    }

    private fun mapFont(value: String): FontScaleMode = when (value.lowercase()) {
        "extra_small" -> FontScaleMode.ExtraSmall
        "small" -> FontScaleMode.Small
        "compact" -> FontScaleMode.Compact
        "medium" -> FontScaleMode.System
        "large" -> FontScaleMode.Large
        "xlarge" -> FontScaleMode.XLarge
        "extra_large" -> FontScaleMode.ExtraLarge
        else -> FontScaleMode.System
    }

    private fun mapColumns(value: String): EventGridColumns = when (value) {
        "2" -> EventGridColumns.Two
        "3" -> EventGridColumns.Three
        "4" -> EventGridColumns.Four
        else -> EventGridColumns.Auto
    }

    private fun mapThemeForWindows(mode: ThemeMode): String = when (mode) {
        ThemeMode.Dark -> "dark"
        ThemeMode.Light, ThemeMode.System -> "light"
    }

    private fun mapFontForWindows(mode: FontScaleMode): String = when (mode) {
        FontScaleMode.ExtraSmall, FontScaleMode.Small, FontScaleMode.Compact -> "small"
        FontScaleMode.System -> "medium"
        FontScaleMode.Large, FontScaleMode.XLarge, FontScaleMode.ExtraLarge -> "large"
    }

    private fun mapFontFromWindows(value: String): FontScaleMode = when (value.lowercase()) {
        "small" -> FontScaleMode.Small
        "medium" -> FontScaleMode.System
        "large" -> FontScaleMode.Large
        "xlarge" -> FontScaleMode.XLarge
        else -> FontScaleMode.System
    }

    private fun parseBool(value: String): Boolean? = when (value.trim().lowercase()) {
        "true" -> true
        "false" -> false
        else -> null
    }

    private fun csvField(value: String): String = if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"${value.replace("\"", "\"\"")}\"" else value

    private fun csvRecordSequence(text: String): Sequence<List<String>> = sequence {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var quoted = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                quoted && c == '"' && i + 1 < text.length && text[i + 1] == '"' -> {
                    current.append('"')
                    i++
                }
                quoted && c == '"' -> quoted = false
                !quoted && c == '"' -> quoted = true
                !quoted && c == ',' -> {
                    fields += current.toString()
                    current.clear()
                }
                !quoted && (c == '\n' || c == '\r') -> {
                    if (c == '\r' && i + 1 < text.length && text[i + 1] == '\n') i++
                    fields += current.toString()
                    current.clear()
                    yield(fields.toList())
                    fields.clear()
                }
                else -> current.append(c)
            }
            i++
        }
        if (current.isNotEmpty() || fields.isNotEmpty()) {
            fields += current.toString()
            yield(fields.toList())
        }
    }
}

