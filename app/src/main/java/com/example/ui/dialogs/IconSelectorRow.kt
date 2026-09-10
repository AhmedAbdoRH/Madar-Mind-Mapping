package com.example.ui.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.IconPreferences
import com.example.ui.theme.SleekBorder
import com.example.ui.theme.SleekBorderSubtle
import com.example.ui.theme.SleekPrimary
import com.example.ui.theme.SleekSurface
import com.example.ui.theme.SleekSurfaceElevated
import com.example.ui.theme.SleekSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.util.OrbitIconItem
import com.example.ui.util.OrbitIcons

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IconSelectorRow(
    selectedIconName: String,
    onIconSelected: (String) -> Unit,
    activeColor: Color,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.icon_symbol_label),
    allowNone: Boolean = true
) {
    val context = LocalContext.current
    val iconPrefs = remember { IconPreferences(context) }
    var recentKeys by remember { mutableStateOf(iconPrefs.getRecentIcons()) }
    var isExpanded by remember { mutableStateOf(false) }

    val handleIconClick: (String) -> Unit = { key ->
        onIconSelected(key)
        if (key != "none" && key.isNotBlank()) {
            iconPrefs.recordIconUsed(key)
            recentKeys = iconPrefs.getRecentIcons()
        }
    }

    // Build the list of icons for the single row (recents + none + active if missing)
    val singleRowIcons = remember(recentKeys, selectedIconName, allowNone) {
        val result = mutableListOf<OrbitIconItem>()
        if (allowNone) {
            result.add(OrbitIconItem("none", "None", Icons.Default.Close))
        }

        // Add currently selected icon at the front if not "none" and not already in top 6 recents
        val activeItem = if (selectedIconName.isNotBlank() && selectedIconName != "none") {
            OrbitIcons.icons.find { it.key.equals(selectedIconName, ignoreCase = true) }
        } else null

        if (activeItem != null && !recentKeys.take(5).contains(activeItem.key)) {
            result.add(activeItem)
        }

        // Add recent icons
        for (key in recentKeys) {
            if (key != "none" && (activeItem == null || !key.equals(activeItem.key, ignoreCase = true))) {
                val iconItem = OrbitIcons.icons.find { it.key.equals(key, ignoreCase = true) }
                if (iconItem != null && !result.contains(iconItem)) {
                    result.add(iconItem)
                }
            }
            if (result.size >= 8) break
        }

        // If list is small, fill up from OrbitIcons
        if (result.size < 6) {
            for (item in OrbitIcons.icons) {
                if (item.key != "none" && !result.contains(item)) {
                    result.add(item)
                }
                if (result.size >= 8) break
            }
        }
        result
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Section Header with Title and "More/Less" Toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = title,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (!isExpanded) {
                    Surface(
                        color = SleekSurfaceVariant,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, SleekBorderSubtle)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = TextTertiary,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = stringResource(R.string.recent_icons_badge),
                                color = TextTertiary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            TextButton(
                onClick = { isExpanded = !isExpanded }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = if (isExpanded) stringResource(R.string.action_less) else stringResource(R.string.action_more),
                        color = SleekPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = SleekPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // When Collapsed: Single Row of Recent Icons + More Button
        if (!isExpanded) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                singleRowIcons.forEach { iconItem ->
                    val isSelected = if (iconItem.key == "none") {
                        selectedIconName.isBlank() || selectedIconName.equals("none", ignoreCase = true)
                    } else {
                        iconItem.key.equals(selectedIconName, ignoreCase = true)
                    }

                    IconTile(
                        iconItem = iconItem,
                        isSelected = isSelected,
                        activeColor = activeColor,
                        onClick = { handleIconClick(iconItem.key) }
                    )
                }

                // "More" Grid Tile at the end of the single row
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SleekSurfaceVariant)
                        .border(
                            width = 1.dp,
                            color = SleekBorderSubtle,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { isExpanded = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = stringResource(R.string.action_more),
                        tint = SleekPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // When Expanded: Full Grid of all icons
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Surface(
                color = SleekSurfaceElevated,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorderSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OrbitIcons.icons.forEach { iconItem ->
                            if (!allowNone && iconItem.key == "none") return@forEach

                            val isSelected = if (iconItem.key == "none") {
                                selectedIconName.isBlank() || selectedIconName.equals("none", ignoreCase = true)
                            } else {
                                iconItem.key.equals(selectedIconName, ignoreCase = true)
                            }

                            IconTile(
                                iconItem = iconItem,
                                isSelected = isSelected,
                                activeColor = activeColor,
                                onClick = { handleIconClick(iconItem.key) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IconTile(
    iconItem: OrbitIconItem,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isSelected) activeColor.copy(alpha = 0.22f) else SleekSurfaceVariant
            )
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) SleekPrimary else SleekBorderSubtle,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = iconItem.icon,
            contentDescription = iconItem.label,
            tint = if (isSelected) SleekPrimary else TextSecondary,
            modifier = Modifier.size(19.dp)
        )
    }
}
