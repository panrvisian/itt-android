package com.bigbrother.mobile.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import top.yukonga.miuix.kmp.basic.NavigationBar as MiuixNavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem as MiuixNavigationBarItem
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal data class BottomBarDestination(
    val tab: AppTab,
    val label: String,
    val icon: ImageVector,
)

internal val appBottomBarDestinations = listOf(
    BottomBarDestination(AppTab.Home, "首页", Icons.Rounded.Home),
    BottomBarDestination(AppTab.Timeline, "时间轴", Icons.Rounded.Timeline),
    BottomBarDestination(AppTab.Notes, "备注", Icons.Rounded.EditNote),
    BottomBarDestination(AppTab.Stats, "统计", Icons.Rounded.BarChart),
    BottomBarDestination(AppTab.Settings, "设置", Icons.Rounded.Settings),
)

@Composable
internal fun AppBottomBar(
    selectedIndex: Int,
    floating: Boolean,
    liquidGlass: Boolean,
    backdrop: LayerBackdrop?,
    onSelected: (Int) -> Unit,
) {
    if (floating) {
        MiuixLiquidGlassNavigationBar(
            items = appBottomBarDestinations,
            selectedIndex = selectedIndex,
            onItemClick = onSelected,
            backdrop = backdrop,
            isBlurActive = liquidGlass,
        )
    } else {
        MiuixNavigationBar(
            color = MiuixTheme.colorScheme.surface,
            showDivider = true,
        ) {
            appBottomBarDestinations.forEachIndexed { index, destination ->
                MiuixNavigationBarItem(
                    selected = selectedIndex == index,
                    onClick = { onSelected(index) },
                    icon = destination.icon,
                    label = destination.label,
                )
            }
        }
    }
}
