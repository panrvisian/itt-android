@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.bigbrother.mobile.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectTapGestures
import kotlinx.coroutines.launch

val LocalComponentAlpha = staticCompositionLocalOf { 1f }

@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
    onTitleClick: (() -> Unit)? = null,
    onTitleLongPress: (() -> Unit)? = null,
    titleStyle: TextStyle = MaterialTheme.typography.titleMedium,
    titleAlign: TextAlign? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = LocalComponentAlpha.current))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val titleModifier = if (onTitleClick != null || onTitleLongPress != null) {
                    Modifier.weight(1f).pointerInput(title, onTitleClick, onTitleLongPress) {
                        detectTapGestures(
                            onTap = { onTitleClick?.invoke() },
                            onLongPress = { onTitleLongPress?.invoke() }
                        )
                    }
                } else {
                    Modifier.weight(1f)
                }
                Text(
                    title,
                    style = titleStyle,
                    modifier = titleModifier,
                    textAlign = titleAlign,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                trailing?.invoke()
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun LongPressEventTile(
    title: String,
    subtitle: String? = null,
    color: Color,
    modifier: Modifier = Modifier,
    vibrationEnabled: Boolean = true,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val progress = remember { Animatable(0f) }
    var pressed by rememberSaveable(title) { mutableStateOf(false) }
    var longTriggered by rememberSaveable(title) { mutableStateOf(false) }

    LaunchedEffect(pressed) {
        if (!pressed) {
            progress.snapTo(0f)
            longTriggered = false
        }
    }

    Surface(
        modifier = modifier
            .pointerInput(title) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        longTriggered = false
                        val job = scope.launch {
                            progress.animateTo(1f, tween(500, easing = LinearEasing))
                            if (pressed) {
                                longTriggered = true
                                if (vibrationEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onLongPress()
                            }
                        }
                        try {
                            val released = tryAwaitRelease()
                            if (released && !longTriggered) {
                                onClick()
                            }
                        } finally {
                            pressed = false
                            job.cancel()
                            scope.launch { progress.snapTo(0f) }
                        }
                    }
                )
            },
        shape = RoundedCornerShape(18.dp),
        color = color.copy(alpha = 0.12f),
        tonalElevation = 1.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.95f))
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(18.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress.value)
                        .background(color.copy(alpha = 0.26f))
                )
            }
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun RecordCard(
    title: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier,
    vibrationEnabled: Boolean = true,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val progress = remember { Animatable(0f) }
    var pressed by rememberSaveable(title) { mutableStateOf(false) }
    var longTriggered by rememberSaveable(title) { mutableStateOf(false) }

    LaunchedEffect(pressed) {
        if (!pressed) {
            progress.snapTo(0f)
            longTriggered = false
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(title) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        longTriggered = false
                        val job = if (onLongPress != null) {
                            scope.launch {
                                progress.animateTo(1f, tween(500, easing = LinearEasing))
                                if (pressed) {
                                    longTriggered = true
                                    if (vibrationEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onLongPress()
                                }
                            }
                        } else null
                        try {
                            val released = tryAwaitRelease()
                            if (released && !longTriggered) onClick()
                        } finally {
                            pressed = false
                            job?.cancel()
                            scope.launch { progress.snapTo(0f) }
                        }
                    }
                )
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = LocalComponentAlpha.current)),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.95f))
    ) {
        Box {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress.value)
                    .background(color.copy(alpha = 0.22f))
            )
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(width = 8.dp, height = 44.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(color)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun ChoiceChipRow(
    labels: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.forEachIndexed { index, label ->
            FilterChip(
                selected = index == selectedIndex,
                onClick = { onSelected(index) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors()
            )
        }
    }
}

@Composable
fun ColorSwatchRow(
    colors: List<Color>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        colors.forEachIndexed { index, color ->
            Surface(
                modifier = Modifier
                    .size(30.dp),
                shape = RoundedCornerShape(99.dp),
                color = color,
                border = if (index == selectedIndex) BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface) else null,
                onClick = { onSelected(index) }
            ) {}
        }
    }
}





