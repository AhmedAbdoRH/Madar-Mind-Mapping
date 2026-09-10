package com.example.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import com.example.ui.util.SmartOutlineParser

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SmartTextToMapDialog(
    onDismiss: () -> Unit,
    onCreateFromText: (mapTitle: String, nodes: List<MindNodeEntity>) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var outlineText by remember { mutableStateOf("") }
    var customTitle by remember { mutableStateOf("") }

    val sampleTemplate = """
# خطة إطلاق المشروع 2026
## 1. دراسة وتحليل السوق
  - تحليل المنافسين الرئيسيين
  - تحديد الشريحة المستهدفة
  - [x] استبيان احتياجات المستخدمين
## 2. التصميم وتجربة المستخدم
  - الهوية البصرية والألوان
  - نماذج الشاشات التفاعلية Wireframes
  - تصميم تجربة المستخدم UX
## 3. التطوير البرمجي
  - بناء الواجهات بواسطة Jetpack Compose
  - قاعدة بيانات محلية Room Database
  - [ ] مزامنة Google Drive السحابية
## 4. التسويق والإطلاق
  - حملة إعلانية رقمية
  - إطلاق النسخة التجريبية Beta
  - التواصل مع صناع المحتوى
""".trimIndent()

    val parsedResult by remember(outlineText, customTitle) {
        derivedStateOf {
            if (outlineText.isBlank()) null
            else SmartOutlineParser.parseTextToMindMap(outlineText, customTitle)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = SleekSurface,
                border = BorderStroke(1.dp, SleekBorderSubtle),
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.88f)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxHeight()
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
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF8B5CF6), Color(0xFF3B82F6))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = "تحويل نص ونقاط إلى خريطة ذكية",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "الصق أي نص منقط أو مرقم أو مسودات لتحويلها فوراً لخريطة",
                                    fontSize = 11.5.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
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

                    // Scrollable Input & Preview Content
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Action Row (Paste from clipboard, Load Example, Clear)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        val clip = clipboardManager.getText()?.text
                                        if (!clip.isNullOrBlank()) {
                                            outlineText = clip
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SleekSurfaceElevated,
                                        contentColor = SleekPrimary
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("لصق من الحافظة", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }

                                TextButton(
                                    onClick = { outlineText = sampleTemplate },
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lightbulb,
                                        contentDescription = null,
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("نموذج تجريبي", fontSize = 12.sp, color = TextSecondary)
                                }
                            }

                            if (outlineText.isNotBlank()) {
                                TextButton(
                                    onClick = { outlineText = "" },
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = null,
                                        tint = TextTertiary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("مسح", fontSize = 12.sp, color = TextTertiary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Text Area for Outline
                        OutlinedTextField(
                            value = outlineText,
                            onValueChange = { outlineText = it },
                            placeholder = {
                                Text(
                                    text = "الصق هنا أي نص:\n# العنوان الرئيسي\n  - النقطة الأولى\n    * تفريعة فرعية\n    * تفريعة أخرى\n  - النقطة الثانية\n    1. نقطة مرقمة\n    2. [ ] مهمة قائمة مهام",
                                    color = TextTertiary,
                                    fontSize = 12.5.sp,
                                    lineHeight = 18.sp
                                )
                            },
                            minLines = 7,
                            maxLines = 14,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SleekPrimary,
                                unfocusedBorderColor = SleekBorderSubtle,
                                focusedContainerColor = SleekSurfaceVariant,
                                unfocusedContainerColor = SleekSurfaceVariant,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("smart_text_input_field")
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Live Smart Parse Summary
                        if (parsedResult != null) {
                            val result = parsedResult!!
                            val planetCount = result.nodes.count { it.parentId == result.nodes.firstOrNull()?.id }
                            val subNodesCount = result.nodes.size - 1 - planetCount

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = SleekSurfaceElevated,
                                border = BorderStroke(1.dp, SleekPrimary.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.FormatListBulleted,
                                                contentDescription = null,
                                                tint = SleekPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "معاينة الهيكل المستخرج",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = TextPrimary
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = SleekPrimary.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "${result.nodes.size} عقدة إجمالية",
                                                color = SleekPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "🪐 المركز: ${result.rootTitle}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "✨ $planetCount مدار رئيسي",
                                            fontSize = 12.sp,
                                            color = TextSecondary
                                        )
                                        if (subNodesCount > 0) {
                                            Text(
                                                text = "🛰️ $subNodesCount قمر فرعي",
                                                fontSize = 12.sp,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = SleekBorderSubtle)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Bottom Action Bar
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
                        ) {
                            Text("إلغاء", fontSize = 14.sp)
                        }

                        Button(
                            onClick = {
                                val result = parsedResult
                                if (result != null && result.nodes.isNotEmpty()) {
                                    onCreateFromText(result.mapTitle, result.nodes)
                                }
                            },
                            enabled = parsedResult != null && (parsedResult?.nodes?.isNotEmpty() == true),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SleekPrimary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .height(46.dp)
                                .testTag("generate_map_from_text_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.RocketLaunch,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "إنشاء الخريطة فوراً 🚀",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
