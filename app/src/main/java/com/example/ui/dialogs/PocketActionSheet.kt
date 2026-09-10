package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.MindNodeEntity
import com.example.R
import com.example.ui.theme.SleekBorderSubtle
import com.example.ui.theme.SleekPrimary
import com.example.ui.theme.SleekSurface
import com.example.ui.theme.SleekSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.util.OrbitColors
import com.example.ui.util.OrbitIcons

@Composable
fun PocketActionSheet(
    pocketNode: MindNodeEntity,
    currentCentralNode: MindNodeEntity,
    allNodes: List<MindNodeEntity> = emptyList(),
    isDirectDrop: Boolean = false,
    onMove: () -> Unit,
    onClone: () -> Unit,
    onSyncTwin: () -> Unit,
    onClearPocket: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val sourceColor = remember(pocketNode.colorHex) {
        OrbitColors.parseColor(pocketNode.colorHex)
    }
    val sourceTextColor = remember(sourceColor) {
        OrbitColors.getContrastingTextColor(sourceColor)
    }
    val sourceIcon = remember(pocketNode.iconName) {
        OrbitIcons.getIcon(pocketNode.iconName)
    }

    val targetColor = remember(currentCentralNode.colorHex) {
        OrbitColors.parseColor(currentCentralNode.colorHex)
    }
    val targetTextColor = remember(targetColor) {
        OrbitColors.getContrastingTextColor(targetColor)
    }
    val targetIcon = remember(currentCentralNode.iconName) {
        OrbitIcons.getIcon(currentCentralNode.iconName)
    }

    // Check if target node is a descendant of the source node (to prevent cyclic graph parent loop)
    val isTargetDescendantOfSource = remember(pocketNode.id, currentCentralNode.id, allNodes) {
        if (pocketNode.id == currentCentralNode.id) return@remember true
        var curr = allNodes.firstOrNull { it.id == currentCentralNode.id }
        var isDescendant = false
        while (curr?.parentId != null) {
            if (curr.parentId == pocketNode.id) {
                isDescendant = true
                break
            }
            curr = allNodes.firstOrNull { it.id == curr?.parentId }
        }
        isDescendant
    }

    val isAlreadyChild = pocketNode.parentId == currentCentralNode.id
    val isMoveAllowed = !isTargetDescendantOfSource && !isAlreadyChild && pocketNode.parentId != null

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SleekSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorderSubtle),
            shadowElevation = 14.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("pocket_action_sheet")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isDirectDrop) "🪐" else "📥",
                            fontSize = 22.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Column {
                            Text(
                                text = if (isDirectDrop) "إسقاط داخل مدار العقدة" else "حلقة الجيب المداري",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isDirectDrop) "اختر عملية الإسقاط (نقل أو نسخ أو مزامنة)" else "العقدة جاهزة للإدراج في هذا المدار",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
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

                // Source Node ➔ Target Node Visual Pathway
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SleekSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        // Source Node Capsule
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(sourceColor)
                                    .border(1.5.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (sourceIcon != null) {
                                    Icon(
                                        imageVector = sourceIcon,
                                        contentDescription = null,
                                        tint = sourceTextColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Text(
                                        text = pocketNode.title.take(2).uppercase().ifBlank { "•" },
                                        color = sourceTextColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Column {
                                Text(
                                    text = "العقدة المسحوبة",
                                    color = TextSecondary,
                                    fontSize = 9.5.sp
                                )
                                Text(
                                    text = pocketNode.title,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Arrow
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "To target",
                            tint = SleekPrimary,
                            modifier = Modifier
                                .padding(horizontal = 6.dp)
                                .size(20.dp)
                        )

                        // Target Node Capsule
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "المدار المستقبل",
                                    color = TextSecondary,
                                    fontSize = 9.5.sp
                                )
                                Text(
                                    text = currentCentralNode.title,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(targetColor)
                                    .border(1.5.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (targetIcon != null) {
                                    Icon(
                                        imageVector = targetIcon,
                                        contentDescription = null,
                                        tint = targetTextColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Text(
                                        text = currentCentralNode.title.take(2).uppercase().ifBlank { "•" },
                                        color = targetTextColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "اختر الإجراء المطلوب لتنفيذه في مدار [${currentCentralNode.title}]:",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(10.dp))

                // 1. Move Option (نقل المدار)
                ActionTile(
                    icon = Icons.Default.DriveFileMove,
                    iconTint = if (isMoveAllowed) Color(0xFF8B5CF6) else TextTertiary,
                    title = "نقل المدار إلى هنا (Move)",
                    subtitle = if (isMoveAllowed) {
                        "نقل العقدة وفروعها بالكامل من مكانها القديم لتصبح جزءاً من مدار [${currentCentralNode.title}]"
                    } else if (isAlreadyChild) {
                        "العقدة موجودة بالفعل في هذا المدار"
                    } else {
                        "لا يمكن نقل عقدة إلى أحد مداراتها الفرعية لتجنب الحلقات"
                    },
                    badge = if (isMoveAllowed) "نقل مباشر" else "غير متاح",
                    badgeColor = if (isMoveAllowed) Color(0xFF8B5CF6) else TextTertiary,
                    enabled = isMoveAllowed,
                    onClick = onMove
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 2. Clone Subtree Option (نسخ مستقل)
                ActionTile(
                    icon = Icons.Default.ContentCopy,
                    iconTint = Color(0xFF10B981),
                    title = "نسخ مستقل (Clone Copy)",
                    subtitle = "إنشاء نسخة جديدة ومستقلة بالكامل مع كافة الفروع والتفاصيل داخل [${currentCentralNode.title}]",
                    badge = "نسخة منفصلة",
                    badgeColor = Color(0xFF10B981),
                    enabled = true,
                    onClick = onClone
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 3. Live Sync Twin Option (المزامنة الحية / النقل الآني المتزامن)
                ActionTile(
                    icon = Icons.Default.Sync,
                    iconTint = Color(0xFF06B6D4),
                    title = "مزامنة توأمية حية (Live Sync Twin)",
                    subtitle = "إنشاء توأم متزامن — أي تعديل على المهام أو الملاحظات أو الفروع يتزامن تلقائياً بين المدارين!",
                    badge = "تزامن فوري",
                    badgeColor = Color(0xFF06B6D4),
                    enabled = true,
                    onClick = onSyncTwin
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = SleekBorderSubtle, modifier = Modifier.padding(vertical = 4.dp))

                // Footer
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (onClearPocket != null) {
                        TextButton(
                            onClick = onClearPocket,
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444))
                        ) {
                            Text("تفريغ الجيب", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    TextButton(onClick = onDismiss) {
                        Text("إلغاء", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionTile(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    badge: String,
    badgeColor: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (enabled) SleekSurfaceVariant.copy(alpha = 0.7f) else SleekSurfaceVariant.copy(alpha = 0.35f),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (enabled) SleekBorderSubtle else SleekBorderSubtle.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = if (enabled) 0.15f else 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = title,
                        color = if (enabled) TextPrimary else TextTertiary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeColor.copy(alpha = if (enabled) 0.18f else 0.08f),
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Text(
                            text = badge,
                            color = badgeColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = if (enabled) TextSecondary else TextTertiary,
                    fontSize = 10.5.sp,
                    lineHeight = 13.sp
                )
            }
        }
    }
}
