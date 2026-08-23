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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DriveFileMove
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.MindNodeEntity
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
    onMove: () -> Unit,
    onClone: () -> Unit,
    onSyncTwin: () -> Unit,
    onClearPocket: () -> Unit,
    onDismiss: () -> Unit
) {
    val nodeColor = remember(pocketNode.colorHex) {
        OrbitColors.parseColor(pocketNode.colorHex)
    }
    val textColor = remember(nodeColor) {
        OrbitColors.getContrastingTextColor(nodeColor)
    }
    val icon = remember(pocketNode.iconName) {
        OrbitIcons.getIcon(pocketNode.iconName)
    }

    val isSameNodeOrRoot = pocketNode.id == currentCentralNode.id || pocketNode.parentId == null

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SleekSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorderSubtle),
            shadowElevation = 12.dp,
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
                            text = "🪐",
                            fontSize = 20.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Column {
                            Text(
                                text = "حلقة الجيب المداري",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "العقدة المحتجزة جاهزة للإدراج",
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

                // Node Card Preview
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SleekSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(nodeColor)
                                .border(1.5.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (icon != null) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = textColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Text(
                                    text = pocketNode.title.take(2).uppercase().ifBlank { "•" },
                                    color = textColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = pocketNode.title,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "الهدف: مدار [${currentCentralNode.title}]",
                                color = SleekPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "اختر الإجراء المطلوب في هذا المدار:",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(10.dp))

                // 1. Live Sync Twin Option (المزامنة الحية / النقل الآني المتزامن)
                ActionTile(
                    icon = Icons.Default.Sync,
                    iconTint = Color(0xFF06B6D4),
                    title = "مزامنة حية (Live Sync Twin)",
                    subtitle = "تكرار العقدة كمرآة متزامنة حية — أي تعديل في أي منهما يتزامن تلقائياً!",
                    badge = "تزامن فوري",
                    badgeColor = Color(0xFF06B6D4),
                    onClick = onSyncTwin
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 2. Clone Subtree Option (نسخ مستقل)
                ActionTile(
                    icon = Icons.Default.ContentCopy,
                    iconTint = Color(0xFF10B981),
                    title = "نسخ مستقل (Clone Copy)",
                    subtitle = "إنشاء نسخة جديدة ومستقلة بالكامل مع جميع مداراتها الفرعية",
                    badge = "نسخة منفصلة",
                    badgeColor = Color(0xFF10B981),
                    onClick = onClone
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 3. Move Option (نقل المدار)
                if (!isSameNodeOrRoot) {
                    ActionTile(
                        icon = Icons.Default.DriveFileMove,
                        iconTint = Color(0xFF8B5CF6),
                        title = "نقل المدار إلى هنا (Move Here)",
                        subtitle = "نقل العقدة ومداراتها بالكامل من مكانها القديم إلى هذا المدار",
                        badge = "نقل مباشر",
                        badgeColor = Color(0xFF8B5CF6),
                        onClick = onMove
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                HorizontalDivider(color = SleekBorderSubtle, modifier = Modifier.padding(vertical = 6.dp))

                // Footer: Clear Pocket or Cancel
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(
                        onClick = onClearPocket,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444))
                    ) {
                        Text("إفراغ الحلقة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeColor.copy(alpha = 0.18f),
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
                    color = TextSecondary,
                    fontSize = 10.5.sp,
                    lineHeight = 13.sp
                )
            }
        }
    }
}
