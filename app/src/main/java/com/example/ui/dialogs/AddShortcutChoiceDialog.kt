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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.ui.theme.TextTertiary

enum class ShortcutTargetType {
    IN_APP_HOME,      // تثبيت في الصفحة الرئيسية للتطبيق
    DEVICE_LAUNCHER,  // إضافة إلى شاشة الموبايل الرئيسية
    BOTH              // كلاهما معاً
}

@Composable
fun AddShortcutChoiceDialog(
    itemTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (ShortcutTargetType) -> Unit
) {
    var selectedTarget by remember { mutableStateOf(ShortcutTargetType.IN_APP_HOME) }

    Dialog(onDismissRequest = onDismiss) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(
                shape = RoundedCornerShape(22.dp),
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
                                            listOf(Color(0xFFF59E0B), Color(0xFFF97316))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = "إضافة اختصار",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = itemTitle.take(30),
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

                    Text(
                        text = "اختر مكان وضع الاختصار:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Option 1: In-App Home Screen (Preferred default)
                    ShortcutOptionTile(
                        icon = Icons.Default.PushPin,
                        iconTint = Color(0xFFF59E0B),
                        title = "📌 في الصفحة الرئيسية للتطبيق (الوصول السريع)",
                        subtitle = "تثبيت العقدة بأعلى قائمة العوالم داخل التطبيق للوصول المباشر إليها",
                        isSelected = selectedTarget == ShortcutTargetType.IN_APP_HOME,
                        onClick = { selectedTarget = ShortcutTargetType.IN_APP_HOME }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Option 2: Mobile Device Launcher Home Screen
                    ShortcutOptionTile(
                        icon = Icons.Default.PhoneAndroid,
                        iconTint = Color(0xFF38BDF8),
                        title = "📱 في شاشة الموبايل الرئيسية (أيقونة سطح المكتب)",
                        subtitle = "إنشاء أيقونة واختصار مستقل على لانشر هاتفك للانتقال الفوري لها",
                        isSelected = selectedTarget == ShortcutTargetType.DEVICE_LAUNCHER,
                        onClick = { selectedTarget = ShortcutTargetType.DEVICE_LAUNCHER }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Option 3: Both
                    ShortcutOptionTile(
                        icon = Icons.Default.Stars,
                        iconTint = Color(0xFF8B5CF6),
                        title = "🌟 كلاهما معاً (التطبيق + شاشة الموبايل)",
                        subtitle = "تثبيت في داخل التطبيق وعلى شاشة الهاتف في نفس الوقت",
                        isSelected = selectedTarget == ShortcutTargetType.BOTH,
                        onClick = { selectedTarget = ShortcutTargetType.BOTH }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = SleekBorderSubtle)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Buttons
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
                        ) {
                            Text("إلغاء", fontSize = 13.sp)
                        }

                        Button(
                            onClick = { onConfirm(selectedTarget) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SleekPrimary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(42.dp)
                                .testTag("confirm_add_shortcut_button")
                        ) {
                            Text(
                                text = "تأكيد وإضافة الاختصار",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShortcutOptionTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) SleekPrimary.copy(alpha = 0.12f) else SleekSurfaceVariant,
        border = BorderStroke(
            1.5.dp,
            if (isSelected) SleekPrimary else SleekBorderSubtle
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
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
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (isSelected) SleekPrimary else TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 15.sp
                )
            }
        }
    }
}
