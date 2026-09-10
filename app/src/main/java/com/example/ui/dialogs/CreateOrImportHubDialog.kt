package com.example.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.ui.theme.SleekBorderSubtle
import com.example.ui.theme.SleekPrimary
import com.example.ui.theme.SleekSurface
import com.example.ui.theme.SleekSurfaceElevated
import com.example.ui.theme.SleekSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun CreateOrImportHubDialog(
    onDismiss: () -> Unit,
    onCreateBlankMap: () -> Unit,
    onOpenSmartTextToMap: () -> Unit,
    onOpenBackupRestoreCenter: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = SleekSurface,
                border = BorderStroke(1.dp, SleekBorderSubtle),
                modifier = Modifier.fillMaxWidth(0.95f)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    // Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(SleekPrimary, Color(0xFF8B5CF6))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddCircleOutline,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = "إضافة أو استيراد خريطة",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "اختر الطريقة المفضلة لبدء عالمك الجديد",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق",
                                tint = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = SleekBorderSubtle)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Option 1: Create Blank Map
                    HubOptionCard(
                        icon = Icons.Default.AddCircleOutline,
                        iconBg = Color(0xFF3B82F6),
                        title = "🪐 إنشاء خريطة جديدة فارغة",
                        subtitle = "ابدأ بخريطة نظيفة وقم بإضافة الكواكب والمدارات خطوة بخطوة",
                        badge = "يدوي",
                        onClick = {
                            onDismiss()
                            onCreateBlankMap()
                        },
                        tag = "hub_create_blank_map"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Option 2: Smart Text/Outline to Mind Map
                    HubOptionCard(
                        icon = Icons.Default.AutoAwesome,
                        iconBg = Color(0xFF8B5CF6),
                        title = "✨ تحويل نص ونقاط إلى خريطة ذكية",
                        subtitle = "الصق أي نص منقط، مسودة مهام أو ملخص لتحويله تلقائياً لخريطة متكاملة",
                        badge = "ذكي وسريع ⚡",
                        isHighlighted = true,
                        onClick = {
                            onDismiss()
                            onOpenSmartTextToMap()
                        },
                        tag = "hub_smart_text_to_map"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Option 3: Import JSON / Backup / Google Drive
                    HubOptionCard(
                        icon = Icons.Default.CloudDownload,
                        iconBg = Color(0xFF06B6D4),
                        title = "📥 استيراد ملف / نسخة احتياطية",
                        subtitle = "استيراد خريطة من ملف JSON، أو استعادة النسخ السحابية عبر Google Drive",
                        badge = "استيراد",
                        onClick = {
                            onDismiss()
                            onOpenBackupRestoreCenter()
                        },
                        tag = "hub_import_backup"
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = SleekBorderSubtle)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Bottom Cancel Button
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
                        ) {
                            Text("إلغاء", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HubOptionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color,
    title: String,
    subtitle: String,
    badge: String,
    isHighlighted: Boolean = false,
    onClick: () -> Unit,
    tag: String
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isHighlighted) iconBg.copy(alpha = 0.08f) else SleekSurfaceVariant,
        border = BorderStroke(
            if (isHighlighted) 1.5.dp else 1.dp,
            if (isHighlighted) iconBg.copy(alpha = 0.6f) else SleekBorderSubtle
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag(tag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconBg.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconBg,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = TextPrimary
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = iconBg.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = badge,
                            color = iconBg,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.5.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
