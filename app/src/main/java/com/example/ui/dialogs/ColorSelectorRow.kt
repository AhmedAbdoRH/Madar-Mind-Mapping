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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ScatterPlot
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.ColorPreferences
import com.example.ui.theme.SleekBorderSubtle
import com.example.ui.theme.SleekPrimary
import com.example.ui.theme.SleekSurface
import com.example.ui.theme.SleekSurfaceElevated
import com.example.ui.theme.SleekSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.util.ColorApplyScope
import com.example.ui.util.NodeColorItem
import com.example.ui.util.OrbitColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorSelectorRow(
    selectedColorHex: String,
    onColorSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.color_theme_label),
    mapId: Long? = null,
    showSetAsDefaultOption: Boolean = true,
    showScopeSelector: Boolean = false,
    selectedScope: ColorApplyScope = ColorApplyScope.THIS_NODE_ONLY,
    onScopeSelected: ((ColorApplyScope) -> Unit)? = null,
    onApplyScopeAction: ((ColorApplyScope, String) -> Unit)? = null,
    showPrimaryColorAction: Boolean = false,
    onSetAsPrimaryColor: ((String) -> Unit)? = null,
    hasChildren: Boolean = true,
    hasSiblings: Boolean = true
) {
    val context = LocalContext.current
    val colorPrefs = remember { ColorPreferences(context) }
    var isExpanded by remember { mutableStateOf(false) }
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    var customHexInput by remember { mutableStateOf(selectedColorHex.removePrefix("#")) }

    val defaultNodeColor = remember(selectedColorHex, mapId) {
        colorPrefs.getDefaultNodeColor(mapId)
    }
    val isCurrentDefault = remember(selectedColorHex, mapId) {
        colorPrefs.isDefaultNodeColor(selectedColorHex, mapId)
    }

    val handleColorClick: (String) -> Unit = { hex ->
        onColorSelected(hex)
        customHexInput = hex.removePrefix("#")
        colorPrefs.recordColorUsed(hex)
    }

    // Curated high-contrast primary colors for quick 1-tap picking
    val quickPalette = remember {
        listOf(
            NodeColorItem("Indigo", "#4F5B92", Color(0xFF4F5B92)),
            NodeColorItem("Blue", "#3B82F6", Color(0xFF3B82F6)),
            NodeColorItem("Cyan", "#06B6D4", Color(0xFF06B6D4)),
            NodeColorItem("Emerald", "#10B981", Color(0xFF10B981)),
            NodeColorItem("Amber", "#F59E0B", Color(0xFFF59E0B)),
            NodeColorItem("Rose", "#F43F5E", Color(0xFFF43F5E)),
            NodeColorItem("Magenta", "#EC4899", Color(0xFFEC4899)),
            NodeColorItem("Violet", "#8B5CF6", Color(0xFF8B5CF6)),
            NodeColorItem("Deep Purple", "#6750A4", Color(0xFF6750A4)),
            NodeColorItem("Teal", "#006874", Color(0xFF006874)),
            NodeColorItem("Orange", "#E8710A", Color(0xFFE8710A)),
            NodeColorItem("Crimson", "#EF4444", Color(0xFFEF4444))
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Section Header with Title and "More Colors" Toggle
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

                if (isCurrentDefault) {
                    Surface(
                        color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFF59E0B).copy(alpha = 0.3f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = stringResource(R.string.default_color_badge),
                                color = Color(0xFFF59E0B),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
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

        Spacer(modifier = Modifier.height(6.dp))

        // Clean Horizontal Color Palette
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // If active color is not in quick palette, display it first
            val inQuick = quickPalette.any { it.hex.equals(selectedColorHex, ignoreCase = true) }
            if (!inQuick) {
                val customActive = NodeColorItem("Selected", selectedColorHex, OrbitColors.parseColor(selectedColorHex))
                ColorSwatch(
                    item = customActive,
                    isSelected = true,
                    checkTint = OrbitColors.getContrastingTextColor(customActive.color),
                    onClick = { handleColorClick(selectedColorHex) }
                )
            }

            quickPalette.forEach { item ->
                val isSelected = item.hex.equals(selectedColorHex, ignoreCase = true)
                val checkTint = OrbitColors.getContrastingTextColor(item.color)

                ColorSwatch(
                    item = item,
                    isSelected = isSelected,
                    checkTint = checkTint,
                    onClick = { handleColorClick(item.hex) }
                )
            }

            // Custom color button
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(SleekSurfaceVariant)
                    .border(1.dp, SleekBorderSubtle, CircleShape)
                    .clickable { isExpanded = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = stringResource(R.string.action_more),
                    tint = SleekPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // =========================================================================
        // ⭐ Set as Default Color for New Nodes in Map Option
        // =========================================================================
        if (showSetAsDefaultOption) {
            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isCurrentDefault) Color(0xFFF59E0B).copy(alpha = 0.12f) else SleekSurfaceVariant.copy(alpha = 0.6f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isCurrentDefault) Color(0xFFF59E0B).copy(alpha = 0.4f) else SleekBorderSubtle
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        colorPrefs.setDefaultNodeColor(selectedColorHex, mapId)
                    }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (isCurrentDefault) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = if (isCurrentDefault) Color(0xFFF59E0B) else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.set_as_default_node_color),
                                color = if (isCurrentDefault) Color(0xFFF59E0B) else TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = if (isCurrentDefault) FontWeight.Bold else FontWeight.Medium
                            )
                            Text(
                                text = stringResource(R.string.set_as_default_node_color_desc),
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }

                    if (isCurrentDefault) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFF59E0B)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .padding(3.dp)
                                    .size(12.dp)
                            )
                        }
                    }
                }
            }
        }

        // When Expanded: Full Categorized Palettes + Custom Hex Input
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
                    .padding(top = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
                ) {
                    // Category Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OrbitColors.categories.forEachIndexed { index, cat ->
                            val isCatSelected = selectedCategoryIndex == index
                            FilterChip(
                                selected = isCatSelected,
                                onClick = { selectedCategoryIndex = index },
                                label = {
                                    Text(
                                        text = stringResource(cat.titleRes),
                                        fontSize = 11.sp,
                                        fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SleekPrimary,
                                    selectedLabelColor = Color.White,
                                    containerColor = SleekSurfaceVariant,
                                    labelColor = TextSecondary
                                )
                            )
                        }

                        val isCustomTab = selectedCategoryIndex == OrbitColors.categories.size
                        FilterChip(
                            selected = isCustomTab,
                            onClick = { selectedCategoryIndex = OrbitColors.categories.size },
                            label = {
                                Text(
                                    text = stringResource(R.string.custom_color_tab),
                                    fontSize = 11.sp,
                                    fontWeight = if (isCustomTab) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SleekPrimary,
                                selectedLabelColor = Color.White,
                                containerColor = SleekSurfaceVariant,
                                labelColor = TextSecondary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (selectedCategoryIndex < OrbitColors.categories.size) {
                        val currentCategory = OrbitColors.categories[selectedCategoryIndex]
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            currentCategory.colors.forEach { item ->
                                val isSelected = item.hex.equals(selectedColorHex, ignoreCase = true)
                                val checkTint = OrbitColors.getContrastingTextColor(item.color)

                                ColorSwatch(
                                    item = item,
                                    isSelected = isSelected,
                                    checkTint = checkTint,
                                    onClick = { handleColorClick(item.hex) }
                                )
                            }
                        }
                    } else {
                        // Custom Hex Input
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(OrbitColors.parseColor("#$customHexInput"))
                                    .border(2.dp, SleekBorderSubtle, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            OutlinedTextField(
                                value = customHexInput,
                                onValueChange = { input ->
                                    val filtered = input.filter { it.isLetterOrDigit() }.take(6)
                                    customHexInput = filtered
                                    if (filtered.length == 6 || filtered.length == 3) {
                                        val validHex = "#$filtered"
                                        handleColorClick(validHex)
                                    }
                                },
                                label = { Text(stringResource(R.string.custom_hex_label)) },
                                placeholder = { Text("4F5B92") },
                                prefix = { Text("#", color = SleekPrimary, fontWeight = FontWeight.Bold) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = SleekPrimary,
                                    unfocusedBorderColor = SleekBorderSubtle,
                                    focusedContainerColor = SleekSurface,
                                    unfocusedContainerColor = SleekSurfaceVariant
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // Color Propagation Scope Selector (نطاق تطبيق اللون في شاشة التعديل)
        // =========================================================================
        if (showScopeSelector) {
            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                color = SleekSurfaceVariant.copy(alpha = 0.8f),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = SleekPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.color_scope_title),
                            color = TextPrimary,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Scope Chips
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ColorApplyScope.values().forEach { scope ->
                            val isSelected = selectedScope == scope
                            val icon = getScopeIcon(scope)

                            FilterChip(
                                selected = isSelected,
                                onClick = { onScopeSelected?.invoke(scope) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = if (isSelected) Color.White else SleekPrimary
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(scope.titleRes),
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SleekPrimary,
                                    selectedLabelColor = Color.White,
                                    containerColor = SleekSurface,
                                    labelColor = TextPrimary
                                )
                            )
                        }
                    }

                    if (onApplyScopeAction != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (hasChildren) {
                                Button(
                                    onClick = { onApplyScopeAction(ColorApplyScope.DIRECT_CHILDREN, selectedColorHex) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SleekPrimary.copy(alpha = 0.15f),
                                        contentColor = SleekPrimary
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.SubdirectoryArrowRight, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.apply_color_to_children_btn), fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (hasSiblings) {
                                Button(
                                    onClick = { onApplyScopeAction(ColorApplyScope.SIBLINGS, selectedColorHex) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF10B981).copy(alpha = 0.15f),
                                        contentColor = Color(0xFF10B981)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Hub, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.apply_color_to_siblings_btn), fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = { onApplyScopeAction(ColorApplyScope.ENTIRE_BRANCH, selectedColorHex) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF8B5CF6).copy(alpha = 0.15f),
                                    contentColor = Color(0xFF8B5CF6)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.DeviceHub, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.apply_color_to_branch_btn), fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (showPrimaryColorAction && onSetAsPrimaryColor != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                onSetAsPrimaryColor(selectedColorHex)
                                colorPrefs.setDefaultNodeColor(selectedColorHex, mapId)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF59E0B).copy(alpha = 0.15f),
                                contentColor = Color(0xFFF59E0B)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.set_as_primary_color_btn),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getScopeIcon(scope: ColorApplyScope): ImageVector {
    return when (scope) {
        ColorApplyScope.THIS_NODE_ONLY -> Icons.Default.Palette
        ColorApplyScope.DIRECT_CHILDREN -> Icons.Default.SubdirectoryArrowRight
        ColorApplyScope.ALL_DESCENDANTS -> Icons.Default.AccountTree
        ColorApplyScope.SIBLINGS -> Icons.Default.Hub
        ColorApplyScope.ENTIRE_BRANCH -> Icons.Default.DeviceHub
        ColorApplyScope.NODE_AND_SIBLINGS -> Icons.Default.ScatterPlot
        ColorApplyScope.HARMONIC_GRADIENT -> Icons.Default.AutoAwesome
        ColorApplyScope.SET_AS_PRIMARY_MAP_COLOR -> Icons.Default.Star
    }
}

@Composable
private fun ColorSwatch(
    item: NodeColorItem,
    isSelected: Boolean,
    checkTint: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(item.color)
            .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                color = if (isSelected) SleekPrimary else SleekBorderSubtle,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = checkTint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

