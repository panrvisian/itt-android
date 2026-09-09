package com.bigbrother.mobile.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.NumberPicker
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.ButtonDefaults as MiuixButtonDefaults
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.highlight.BloomStroke
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.highlight.LightPosition
import top.yukonga.miuix.kmp.blur.highlight.LightSource
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

enum class CalendarHeaderMode {
    Day,
    Week,
    Month,
    Semester
}

private const val CALENDAR_PAGER_COUNT = 2401
private const val CALENDAR_PAGER_CENTER = CALENDAR_PAGER_COUNT / 2
private val weekLabels = listOf("日", "一", "二", "三", "四", "五", "六")
private val compactWeekHeight = 66.dp
private val monthWeekdayHeaderHeight = 30.dp
private val monthWeekHeight = 44.dp
private val monthCalendarHeight = 294.dp

/**
 * Fixed MiuiX calendar header shared by timeline, notes and statistics.
 * Horizontal gestures are intentionally contained by the week/month pagers so the parent main
 * pager continues to own horizontal gestures everywhere outside this header.
 */
@Composable
fun MiuixCalendarHeader(
    selectedDate: LocalDate,
    mode: CalendarHeaderMode,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    semesterStart: LocalDate? = null,
    semesterEnd: LocalDate? = null,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val today = LocalDate.now()
    val pagerState = rememberPagerState(
        initialPage = CALENDAR_PAGER_CENTER,
        pageCount = { CALENDAR_PAGER_COUNT }
    )

    LaunchedEffect(selectedDate, mode, expanded) {
        if (pagerState.currentPage != CALENDAR_PAGER_CENTER) {
            pagerState.scrollToPage(CALENDAR_PAGER_CENTER)
        }
    }

    LaunchedEffect(pagerState, mode, expanded, selectedDate) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { page ->
                if (expanded || page == CALENDAR_PAGER_CENTER) return@collect
                val offset = (page - CALENDAR_PAGER_CENTER).toLong()
                when (mode) {
                    CalendarHeaderMode.Week -> onDateSelected(selectedDate.plusWeeks(offset))
                    CalendarHeaderMode.Month -> onDateSelected(selectedDate.plusMonths(offset))
                    else -> Unit
                }
            }
    }

    val pageOffset = pagerState.currentPage - CALENDAR_PAGER_CENTER
    val displayedMonth = YearMonth.from(selectedDate).plusMonths(pageOffset.toLong())
    val headerLabel = when {
        expanded && mode != CalendarHeaderMode.Semester -> formatMonth(displayedMonth)
        mode == CalendarHeaderMode.Day -> formatDayMonth(selectedDate)
        mode == CalendarHeaderMode.Week -> {
            val start = startOfSundayWeek(selectedDate)
            "${formatShortDate(start)} — ${formatShortDate(start.plusDays(6))}"
        }
        mode == CalendarHeaderMode.Month -> formatMonth(YearMonth.from(selectedDate))
        else -> {
            if (semesterStart != null && semesterEnd != null) {
                "${formatShortDate(semesterStart)} — ${formatShortDate(semesterEnd)}"
            } else {
                "当前学期"
            }
        }
    }
    val currentLabel = when (mode) {
        CalendarHeaderMode.Day -> "今天"
        CalendarHeaderMode.Week -> "本周"
        CalendarHeaderMode.Month -> "本月"
        CalendarHeaderMode.Semester -> "当前学期"
    }
    val expansionProgress by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "calendarExpansionProgress"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            MiuixLiquidGlassCapsuleButton(
                text = currentLabel,
                modifier = Modifier.align(Alignment.CenterStart),
                onClick = {
                    when (mode) {
                        CalendarHeaderMode.Day -> onDateSelected(today)
                        CalendarHeaderMode.Week -> onDateSelected(today)
                        CalendarHeaderMode.Month -> onDateSelected(today.withDayOfMonth(1))
                        CalendarHeaderMode.Semester -> Unit
                    }
                    onExpandedChange(false)
                }
            )
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .then(
                        if (!expanded && mode == CalendarHeaderMode.Month) {
                            Modifier.pointerInput(selectedDate) {
                                var totalDrag = 0f
                                detectHorizontalDragGestures(
                                    onDragStart = { totalDrag = 0f },
                                    onHorizontalDrag = { change, dragAmount ->
                                        change.consume()
                                        totalDrag += dragAmount
                                    },
                                    onDragCancel = { totalDrag = 0f },
                                    onDragEnd = {
                                        val threshold = size.width * 0.18f
                                        when {
                                            totalDrag <= -threshold -> onDateSelected(selectedDate.plusMonths(1))
                                            totalDrag >= threshold -> onDateSelected(selectedDate.minusMonths(1))
                                        }
                                        totalDrag = 0f
                                    }
                                )
                            }
                        } else {
                            Modifier
                        }
                    )
                    .clip(CircleShape)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = if (expanded) "收起日历" else "展开日历"
                    ) { onExpandedChange(!expanded) }
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.Text(
                    text = headerLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically,
                content = actions
            )
        }

        if (mode == CalendarHeaderMode.Semester) {
            if (expanded) {
                NumberPicker(
                    value = 0,
                    onValueChange = {},
                    range = 0..0,
                    visibleItemCount = 3,
                    label = { "当前学期" },
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        } else {
            AnimatedCalendarPager(
                pagerState = pagerState,
                selectedDate = selectedDate,
                mode = mode,
                expanded = expanded,
                expansionProgress = expansionProgress,
                onDateSelected = {
                    onDateSelected(it)
                    if (expanded) onExpandedChange(false)
                }
            )
        }

        CalendarExpandHandle(
            expanded = expanded,
            onClick = { onExpandedChange(!expanded) }
        )
    }
}

private val calendarButtonHighlight = Highlight(
    width = 1.dp,
    alpha = 0.8f,
    style = BloomStroke(
        color = Color.White.copy(alpha = 0.12f),
        innerBlurRadius = 2.dp,
        primaryLight = LightSource(
            position = LightPosition(0.3f, -0.2f, -0.05f),
            color = Color.White,
            intensity = 1f
        ),
        secondaryLight = LightSource(
            position = LightPosition(0.7f, 0.9f, -0.5f),
            color = Color.White,
            intensity = 0.35f
        ),
        dualPeak = true
    )
)

@Composable
fun MiuixLiquidGlassCapsuleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    content: @Composable RowScope.() -> Unit
) {
    val backdrop = LocalCalendarButtonBackdrop.current
    val containerColor = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.42f)
    val glassModifier = if (backdrop != null) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { CircleShape },
            effects = {
                vibrancy()
                blur(4.dp.toPx(), 4.dp.toPx())
                lens(
                    refractionHeight = 18.dp.toPx(),
                    refractionAmount = 18.dp.toPx()
                )
            },
            highlight = { calendarButtonHighlight },
            onDrawSurface = { drawRect(containerColor) }
        )
    } else {
        Modifier.background(containerColor, CircleShape)
    }
    MiuixButton(
        onClick = onClick,
        modifier = modifier
            .then(glassModifier)
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                }
            ),
        cornerRadius = 40.dp,
        minHeight = 40.dp,
        colors = MiuixButtonDefaults.buttonColors(
            color = Color.Transparent,
            contentColor = MiuixTheme.colorScheme.onSurface
        ),
        insideMargin = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        content = content
    )
}

@Composable
fun MiuixLiquidGlassCapsuleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    MiuixLiquidGlassCapsuleButton(
        onClick = onClick,
        modifier = modifier,
        contentDescription = text
    ) {
        top.yukonga.miuix.kmp.basic.Text(
            text = text,
            style = MiuixTheme.textStyles.button
        )
    }
}

@Composable
fun MiuixLiquidGlassMenuSurface(
    backdrop: LayerBackdrop?,
    containerColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    val glassModifier = if (backdrop != null) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                blur(14.dp.toPx(), 14.dp.toPx())
                lens(
                    refractionHeight = 22.dp.toPx(),
                    refractionAmount = 16.dp.toPx()
                )
            },
            highlight = { calendarButtonHighlight },
            onDrawSurface = { drawRect(containerColor) }
        )
    } else {
        Modifier.background(containerColor, shape)
    }

    Box(
        modifier = modifier
            .then(glassModifier)
            .clip(shape)
            .border(1.dp, Color.White.copy(alpha = 0.14f), shape),
        content = { content() }
    )
}

@Composable
private fun WeekCalendar(
    weekStart: LocalDate,
    selectedDate: LocalDate,
    emphasizeSelectedDay: Boolean,
    onDateSelected: (LocalDate) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        repeat(7) { index ->
            val date = weekStart.plusDays(index.toLong())
            CalendarDayCell(
                weekday = weekLabels[index],
                date = date,
                selected = emphasizeSelectedDay && date == selectedDate,
                muted = false,
                modifier = Modifier.weight(1f),
                onClick = { onDateSelected(date) }
            )
        }
    }
}

@Composable
private fun AnimatedCalendarPager(
    pagerState: PagerState,
    selectedDate: LocalDate,
    mode: CalendarHeaderMode,
    expanded: Boolean,
    expansionProgress: Float,
    onDateSelected: (LocalDate) -> Unit
) {
    val hasCompactWeek = mode == CalendarHeaderMode.Day || mode == CalendarHeaderMode.Week
    val collapsedHeight = if (hasCompactWeek) compactWeekHeight else 0.dp
    val animatedHeight = collapsedHeight + (monthCalendarHeight - collapsedHeight) * expansionProgress
    val showCompactPager = !expanded && expansionProgress <= 0.001f && hasCompactWeek

    HorizontalPager(
        state = pagerState,
        userScrollEnabled = showCompactPager || (expanded && expansionProgress >= 0.999f),
        modifier = Modifier
            .fillMaxWidth()
            .height(animatedHeight)
            .clipToBounds()
    ) { page ->
        val pageOffset = (page - CALENDAR_PAGER_CENTER).toLong()
        if (showCompactPager) {
            WeekCalendar(
                weekStart = startOfSundayWeek(selectedDate).plusWeeks(pageOffset),
                selectedDate = selectedDate,
                emphasizeSelectedDay = mode == CalendarHeaderMode.Day,
                onDateSelected = onDateSelected
            )
        } else {
            AnimatedMonthCalendar(
                month = YearMonth.from(selectedDate).plusMonths(pageOffset),
                selectedDate = selectedDate,
                animateSelectedWeek = hasCompactWeek && page == CALENDAR_PAGER_CENTER,
                emphasizeSelectedDay = mode == CalendarHeaderMode.Day,
                expansionProgress = expansionProgress,
                onDateSelected = onDateSelected
            )
        }
    }
}

@Composable
private fun AnimatedMonthCalendar(
    month: YearMonth,
    selectedDate: LocalDate,
    animateSelectedWeek: Boolean,
    emphasizeSelectedDay: Boolean,
    expansionProgress: Float,
    onDateSelected: (LocalDate) -> Unit
) {
    val firstVisibleDate = startOfSundayWeek(month.atDay(1))
    val selectedWeekStart = startOfSundayWeek(selectedDate)
    val movingRowIndex = if (animateSelectedWeek && YearMonth.from(selectedDate) == month) {
        (ChronoUnit.DAYS.between(firstVisibleDate, selectedWeekStart) / 7L)
            .toInt()
            .takeIf { it in 0..5 }
    } else {
        null
    }
    val fadeAlpha = ((expansionProgress - 0.12f) / 0.88f).coerceIn(0f, 1f)
    val datesEnabled = expansionProgress >= 0.999f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(monthCalendarHeight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(monthWeekdayHeaderHeight)
                .graphicsLayer { alpha = fadeAlpha },
            verticalAlignment = Alignment.CenterVertically
        ) {
            weekLabels.forEach { label ->
                androidx.compose.material3.Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        repeat(6) { row ->
            if (row != movingRowIndex) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(monthWeekHeight)
                        .offset(y = monthWeekdayHeaderHeight + monthWeekHeight * row.toFloat())
                        .graphicsLayer { alpha = fadeAlpha }
                ) {
                    repeat(7) { column ->
                        val date = firstVisibleDate.plusDays((row * 7L) + column)
                        CalendarDayCell(
                            weekday = null,
                            date = date,
                            selected = date == selectedDate,
                            muted = YearMonth.from(date) != month,
                            enabled = datesEnabled,
                            modifier = Modifier.weight(1f),
                            onClick = { onDateSelected(date) }
                        )
                    }
                }
            }
        }

        if (movingRowIndex != null) {
            val targetOffset = monthWeekdayHeaderHeight + monthWeekHeight * movingRowIndex.toFloat()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(compactWeekHeight)
                    .offset(y = targetOffset * expansionProgress)
            ) {
                repeat(7) { column ->
                    val date = selectedWeekStart.plusDays(column.toLong())
                    AnimatedWeekDayCell(
                        weekday = weekLabels[column],
                        date = date,
                        selectedDate = selectedDate,
                        month = month,
                        emphasizeSelectedDay = emphasizeSelectedDay,
                        expansionProgress = expansionProgress,
                        modifier = Modifier.weight(1f),
                        onClick = { onDateSelected(date) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedWeekDayCell(
    weekday: String,
    date: LocalDate,
    selectedDate: LocalDate,
    month: YearMonth,
    emphasizeSelectedDay: Boolean,
    expansionProgress: Float,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val accent = MiuixTheme.colorScheme.primary
    val selectedText = if (accent.luminance() > 0.55f) Color.Black else Color.White
    val selectedAmount = if (date == selectedDate) {
        if (emphasizeSelectedDay) 1f else expansionProgress
    } else {
        0f
    }
    val expandedTextColor = if (YearMonth.from(date) != month) {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f)
    } else {
        MaterialTheme.colorScheme.onBackground
    }
    val unselectedTextColor = lerpColor(
        MaterialTheme.colorScheme.onBackground,
        expandedTextColor,
        expansionProgress
    )
    val dateTop = 22.dp + (3.dp - 22.dp) * expansionProgress

    Box(
        modifier = modifier
            .height(compactWeekHeight)
            .semantics {
                role = Role.Button
                contentDescription = formatFullDate(date)
            }
            .clickable(onClick = onClick)
    ) {
        androidx.compose.material3.Text(
            text = weekday,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 3.dp)
                .graphicsLayer { alpha = 1f - expansionProgress },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = dateTop)
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.copy(alpha = selectedAmount)),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selectedAmount >= 0.5f) FontWeight.Bold else FontWeight.Normal,
                color = lerpColor(unselectedTextColor, selectedText, selectedAmount)
            )
        }
    }
}

@Composable
private fun CalendarDayCell(
    weekday: String?,
    date: LocalDate,
    selected: Boolean,
    muted: Boolean,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val accent = MiuixTheme.colorScheme.primary
    val selectedText = if (accent.luminance() > 0.55f) Color.Black else Color.White
    Column(
        modifier = modifier
            .semantics {
                role = Role.Button
                contentDescription = formatFullDate(date)
            }
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (weekday != null) {
            androidx.compose.material3.Text(
                text = weekday,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(3.dp))
        }
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                .background(if (selected) accent else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    selected -> selectedText
                    muted -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f)
                    else -> MaterialTheme.colorScheme.onBackground
                }
            )
        }
    }
}

@Composable
private fun CalendarExpandHandle(expanded: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp)
            .semantics {
                role = Role.Button
                contentDescription = if (expanded) "收起日历" else "展开日历"
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .align(Alignment.BottomCenter)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
        )
        MiuixIcon(
            imageVector = MiuixIcons.ChevronForward,
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(12.dp)
                .graphicsLayer { rotationZ = if (expanded) -90f else 90f },
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

fun Modifier.collapseCalendarOnBodyInteraction(
    expanded: Boolean,
    onCollapse: () -> Unit
): Modifier = if (!expanded) {
    this
} else {
    pointerInput(onCollapse) {
        awaitEachGesture {
            awaitFirstDown(
                requireUnconsumed = false,
                pass = androidx.compose.ui.input.pointer.PointerEventPass.Initial
            )
            while (true) {
                val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Final)
                val moved = event.changes.any { it.position != it.previousPosition }
                val released = event.changes.none { it.pressed }
                if (moved || released) {
                    onCollapse()
                    break
                }
            }
        }
    }
}

private fun startOfSundayWeek(date: LocalDate): LocalDate {
    val daysFromSunday = date.dayOfWeek.value % DayOfWeek.SUNDAY.value
    return date.minusDays(daysFromSunday.toLong())
}

private fun formatFullDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("yyyy 年 M 月 d 日 EEEE", Locale.CHINA))

private fun formatDayMonth(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("M 月 d 日", Locale.CHINA))

private fun formatShortDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("M 月 d 日", Locale.CHINA))

private fun formatMonth(month: YearMonth): String = "${month.year} 年 ${month.monthValue} 月"
