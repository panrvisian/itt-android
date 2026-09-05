package com.bigbrother.mobile.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bigbrother.mobile.R
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeChild

internal data class BottomBarDestination(
    val tab: AppTab,
    val label: String,
    val iconRes: Int
)

internal val appBottomBarDestinations = listOf(
    BottomBarDestination(AppTab.Home, "首页", R.drawable.ic_home),
    BottomBarDestination(AppTab.Timeline, "时间轴", R.drawable.ic_timeline),
    BottomBarDestination(AppTab.Notes, "备注", R.drawable.ic_notes),
    BottomBarDestination(AppTab.Stats, "统计", R.drawable.ic_stats),
    BottomBarDestination(AppTab.Settings, "设置", R.drawable.ic_settings)
)

@Composable
internal fun AppBottomBar(
    selectedIndex: Int,
    floating: Boolean,
    liquidGlass: Boolean,
    hazeState: HazeState,
    onSelected: (Int) -> Unit
) {
    if (!floating) {
        NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
            appBottomBarDestinations.forEachIndexed { index, destination ->
                NavigationBarItem(
                    selected = selectedIndex == index,
                    onClick = { onSelected(index) },
                    icon = {
                        Icon(
                            painter = painterResource(destination.iconRes),
                            contentDescription = destination.label,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = { Text(destination.label) }
                )
            }
        }
        return
    }

    val shape = CircleShape
    val surface = MaterialTheme.colorScheme.surfaceContainer
    val outline = MaterialTheme.colorScheme.outlineVariant
    val barModifier = Modifier
        .padding(horizontal = 18.dp, vertical = 10.dp)
        .navigationBarsPadding()
        .fillMaxWidth()
        .height(72.dp)
        .shadow(12.dp, shape, clip = false)
        .then(
            if (liquidGlass) {
                Modifier.hazeChild(
                    state = hazeState,
                    shape = shape,
                    style = HazeStyle(
                        tint = surface.copy(alpha = 0.52f),
                        blurRadius = 24.dp,
                        noiseFactor = 0.08f
                    )
                )
            } else {
                Modifier.background(surface, shape)
            }
        )
        .liquidGlassLens(liquidGlass)
        .border(
            width = 1.dp,
            color = if (liquidGlass) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f) else outline,
            shape = shape
        )
        .clip(shape)

    BoxWithConstraints(modifier = barModifier) {
        val itemWidth = maxWidth / appBottomBarDestinations.size
        val targetOffset = itemWidth * selectedIndex
        val indicatorOffset = animateDpAsState(
            targetValue = targetOffset,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "bottomBarIndicator"
        )
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset.value + 4.dp, y = 6.dp)
                .size(width = itemWidth - 8.dp, height = 60.dp)
                .background(
                    brush = if (liquidGlass) {
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.78f),
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.44f),
                                Color.White.copy(alpha = 0.08f)
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.secondaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    },
                    shape = CircleShape
                )
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.onSurface.copy(alpha = if (liquidGlass) 0.14f else 0f),
                    CircleShape
                )
        )

        Row(
            modifier = Modifier.fillMaxWidth().height(72.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            appBottomBarDestinations.forEachIndexed { index, destination ->
                val selected = selectedIndex == index
                val scale = animateFloatAsState(
                    targetValue = if (selected && liquidGlass) 1.08f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "bottomBarItemScale"
                )
                Column(
                    modifier = Modifier
                        .size(width = itemWidth, height = 72.dp)
                        .clickable { onSelected(index) }
                        .graphicsLayer {
                            scaleX = scale.value
                            scaleY = scale.value
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(destination.iconRes),
                        contentDescription = destination.label,
                        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(25.dp)
                    )
                    Text(
                        text = destination.label,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        lineHeight = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            }
        }
    }
}
