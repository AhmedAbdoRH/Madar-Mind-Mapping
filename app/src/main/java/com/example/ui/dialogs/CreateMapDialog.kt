package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.ui.theme.AppTheme
import com.example.ui.theme.SleekBorder
import com.example.ui.theme.SleekBorderSubtle
import com.example.ui.theme.SleekPrimary
import com.example.ui.theme.SleekSurface
import com.example.ui.theme.SleekSurfaceElevated
import com.example.ui.theme.SleekSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.util.OrbitColors
import com.example.ui.util.OrbitIcons

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateMapDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, description: String, themeColorHex: String, rootIcon: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedColorHex by remember { mutableStateOf("#4F5B92") }
    var selectedIconName by remember { mutableStateOf("rocket_launch") }

    val activeColor = OrbitColors.parseColor(selectedColorHex)
    val textColor = OrbitColors.getContrastingTextColor(activeColor)

    Dialog(onDismissRequest = onDismiss) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = SleekSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorderSubtle),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .testTag("create_map_dialog")
            ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header with Preview Planet
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(activeColor)
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        val headerIcon = OrbitIcons.getIcon(selectedIconName)
                        if (headerIcon != null) {
                            Icon(
                                imageVector = headerIcon,
                                contentDescription = null,
                                tint = textColor,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text(
                                text = title.take(2).uppercase().ifBlank { "★" },
                                color = textColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.new_map_dialog_title),
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.new_map_dialog_subtitle),
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Inspiration Presets
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val goalsTitle = stringResource(R.string.template_goals_title)
                    val goalsSub = stringResource(R.string.template_goals_sub)
                    val launchTitle = stringResource(R.string.template_launch_title)
                    val launchSub = stringResource(R.string.template_launch_sub)
                    val bsTitle = stringResource(R.string.template_brainstorm_title)
                    val bsSub = stringResource(R.string.template_brainstorm_sub)
                    val studyTitle = stringResource(R.string.template_study_title)
                    val studySub = stringResource(R.string.template_study_sub)

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedColorHex == "#6366F1") SleekPrimary.copy(alpha = 0.2f) else SleekSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedColorHex == "#6366F1") SleekPrimary else SleekBorderSubtle),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                title = goalsTitle
                                description = goalsSub
                                selectedColorHex = "#6366F1"
                                selectedIconName = "flag"
                            }
                    ) {
                        Text(
                            text = "🎯 $goalsTitle",
                            color = TextPrimary,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedColorHex == "#06B6D4") SleekPrimary.copy(alpha = 0.2f) else SleekSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedColorHex == "#06B6D4") SleekPrimary else SleekBorderSubtle),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                title = launchTitle
                                description = launchSub
                                selectedColorHex = "#06B6D4"
                                selectedIconName = "rocket_launch"
                            }
                    ) {
                        Text(
                            text = "🚀 $launchTitle",
                            color = TextPrimary,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedColorHex == "#EC4899") SleekPrimary.copy(alpha = 0.2f) else SleekSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedColorHex == "#EC4899") SleekPrimary else SleekBorderSubtle),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                title = bsTitle
                                description = bsSub
                                selectedColorHex = "#EC4899"
                                selectedIconName = "psychology"
                            }
                    ) {
                        Text(
                            text = "🧠 $bsTitle",
                            color = TextPrimary,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.map_name_label)) },
                    placeholder = { Text(stringResource(R.string.map_name_placeholder)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = SleekPrimary,
                        unfocusedBorderColor = SleekBorder,
                        focusedLabelColor = SleekPrimary,
                        focusedContainerColor = SleekSurface,
                        unfocusedContainerColor = SleekSurfaceVariant
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_map_title_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Description Input
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.map_desc_label)) },
                    placeholder = { Text(stringResource(R.string.map_desc_placeholder)) },
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = SleekPrimary,
                        unfocusedBorderColor = SleekBorder,
                        focusedContainerColor = SleekSurface,
                        unfocusedContainerColor = SleekSurfaceVariant
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Theme Color Palette
                ColorSelectorRow(
                    selectedColorHex = selectedColorHex,
                    onColorSelected = { selectedColorHex = it },
                    title = stringResource(R.string.core_theme_color_label)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Icon selection
                IconSelectorRow(
                    selectedIconName = selectedIconName,
                    onIconSelected = { selectedIconName = it },
                    activeColor = activeColor,
                    title = stringResource(R.string.core_icon_label),
                    allowNone = false
                )

                Spacer(modifier = Modifier.height(22.dp))

                // Action Buttons
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.action_cancel), color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onConfirm(title.trim(), description.trim(), selectedColorHex, selectedIconName)
                            }
                        },
                        enabled = title.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SleekPrimary,
                            contentColor = if (AppTheme.colors.isDark) Color(0xFF1F2D60) else Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("confirm_create_map_button")
                    ) {
                        Text(stringResource(R.string.action_create_map_confirm), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        }
    }
}

