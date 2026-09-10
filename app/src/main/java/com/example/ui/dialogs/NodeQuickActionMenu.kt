package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GeneratingTokens
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Shortcut
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.model.MindNodeEntity
import com.example.ui.theme.SleekBorderSubtle
import com.example.ui.theme.SleekPrimary
import com.example.ui.theme.SleekSurface
import com.example.ui.theme.SleekSurfaceElevated
import com.example.ui.theme.SleekSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.util.ColorApplyScope
import com.example.ui.util.OrbitColors
import com.example.ui.util.OrbitIcons

@Composable
fun NodeQuickActionMenu(
    node: MindNodeEntity,
    onPutInPocket: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    onViewImage: (() -> Unit)? = null,
    onNavigateToOriginal: (() -> Unit)? = null,
    onAddShortcut: (() -> Unit)? = null,
    onApplyColorScope: ((ColorApplyScope, String) -> Unit)? = null
) {
    var quickScope by remember { mutableStateOf(ColorApplyScope.THIS_NODE_ONLY) }

    val nodeColor = remember(node.colorHex) {
        OrbitColors.parseColor(node.colorHex)
    }
    val textColor = remember(nodeColor) {
        OrbitColors.getContrastingTextColor(nodeColor)
    }
    val icon = remember(node.iconName) {
        OrbitIcons.getIcon(node.iconName)
    }

    val quickColors = remember {
        listOf(
            "#4F5B92", "#7C3AED", "#06B6D4", "#10B981",
            "#F59E0B", "#EF4444", "#EC4899", "#1E3A8A",
            "#D9E2FF", "#EADDFF", "#C4EED0", "#FFF0B3"
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SleekSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorderSubtle),
            shadowElevation = 10.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("node_quick_action_menu")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header with Preview Planet
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(nodeColor)
                            .border(1.dp, SleekBorderSubtle, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (icon != null) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = textColor,
                                modifier = Modifier.size(22.dp)
                            )
                        } else {
                            Text(
                                text = node.title.take(2).uppercase().ifBlank { "•" },
                                color = textColor,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = node.title,
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (onNavigateToOriginal != null) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF06B6D4).copy(alpha = 0.2f),
                                    modifier = Modifier.padding(start = 6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Sync,
                                            contentDescription = null,
                                            tint = Color(0xFF06B6D4),
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = stringResource(R.string.synced_badge),
                                            color = Color(0xFF06B6D4),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                        Text(
                            text = stringResource(R.string.quick_colorize_subtitle),
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Color Swatches & Scope Selector
                if (onApplyColorScope != null) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SleekSurfaceVariant.copy(alpha = 0.7f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Palette,
                                        contentDescription = null,
                                        tint = SleekPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.quick_colorize_title),
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Text(
                                    text = stringResource(quickScope.titleRes),
                                    color = SleekPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Scope Selector Chips
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(
                                    ColorApplyScope.THIS_NODE_ONLY,
                                    ColorApplyScope.SET_AS_PRIMARY_MAP_COLOR,
                                    ColorApplyScope.DIRECT_CHILDREN,
                                    ColorApplyScope.SIBLINGS,
                                    ColorApplyScope.ENTIRE_BRANCH,
                                    ColorApplyScope.HARMONIC_GRADIENT
                                ).forEach { scope ->
                                    val isSelected = quickScope == scope
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { quickScope = scope },
                                        label = {
                                            Text(
                                                text = stringResource(scope.titleRes),
                                                fontSize = 10.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = SleekPrimary,
                                            selectedLabelColor = Color.White,
                                            containerColor = SleekSurface,
                                            labelColor = TextSecondary
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = isSelected,
                                            borderColor = SleekBorderSubtle,
                                            selectedBorderColor = SleekPrimary
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Quick Swatch Palette Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                quickColors.forEach { hex ->
                                    val isSelected = hex.equals(node.colorHex, ignoreCase = true)
                                    val colorObj = OrbitColors.parseColor(hex)
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(colorObj)
                                            .border(
                                                width = if (isSelected) 2.dp else 0.5.dp,
                                                color = if (isSelected) Color.White else SleekBorderSubtle,
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                onApplyColorScope(quickScope, hex)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = OrbitColors.getContrastingTextColor(colorObj),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Action 1: View Image in full screen if image node
                if (!node.imageUri.isNullOrBlank() && onViewImage != null) {
                    MenuOptionItem(
                        icon = Icons.Default.Image,
                        iconTint = Color(0xFF38BDF8),
                        title = "عرض الصورة بالحجم الكامل",
                        subtitle = "فتح الصورة وعرضها في نافذة مكبرة وعالية الدقة",
                        onClick = onViewImage
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Action: Return to Original Node
                if (onNavigateToOriginal != null) {
                    MenuOptionItem(
                        icon = Icons.AutoMirrored.Filled.Reply,
                        iconTint = Color(0xFF06B6D4),
                        title = stringResource(R.string.action_jump_to_original_title),
                        subtitle = stringResource(R.string.action_jump_to_original_desc),
                        onClick = onNavigateToOriginal
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Action: Add to Home Screen Shortcut
                if (onAddShortcut != null) {
                    MenuOptionItem(
                        icon = Icons.Default.Shortcut,
                        iconTint = Color(0xFFF59E0B),
                        title = "وضع اختصار في الشاشة الرئيسية",
                        subtitle = "فتح هذه العقدة مباشرة من شاشة هاتفك الرئيسية بضغطة واحدة",
                        onClick = onAddShortcut
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Action 2: Put in Orbital Pocket (حفظ في حلقة الجيب للنقل والمزامنة)
                MenuOptionItem(
                    icon = Icons.Default.GeneratingTokens,
                    iconTint = SleekPrimary,
                    title = "احتجاز في حلقة النقل / الجيب المداري",
                    subtitle = "حفظ العقدة لنقلها أو استنساخها أو مزامنتها في أي مدار آخر",
                    onClick = onPutInPocket
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Action 2: Edit Details
                MenuOptionItem(
                    icon = Icons.Default.Edit,
                    iconTint = Color(0xFF10B981),
                    title = "تعديل التفاصيل ونظام الألوان",
                    subtitle = "تغيير العنوان، الملاحظات، المهام، التدرجات والأيقونات",
                    onClick = onEdit
                )

                if (node.parentId != null) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Action 3: Delete
                    MenuOptionItem(
                        icon = Icons.Default.Delete,
                        iconTint = Color(0xFFEF4444),
                        title = "حذف العقدة ومداراتها",
                        subtitle = "إزالة هذا الكوكب وجميع مداراته الفرعية",
                        onClick = onDelete
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuOptionItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SleekSurfaceVariant.copy(alpha = 0.7f),
        border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 10.5.sp
                )
            }
        }
    }
}
