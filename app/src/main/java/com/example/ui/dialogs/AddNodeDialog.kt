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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.model.MindNodeEntity
import com.example.ui.theme.AppTheme
import com.example.ui.theme.SleekBorder
import com.example.ui.theme.SleekBorderSubtle
import com.example.ui.theme.SleekPrimary
import com.example.ui.theme.SleekSurface
import com.example.ui.theme.SleekSurfaceElevated
import com.example.ui.theme.SleekSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.util.OrbitColors
import com.example.ui.util.OrbitIcons

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddNodeDialog(
    parentCentralNode: MindNodeEntity,
    onDismiss: () -> Unit,
    onConfirm: (title: String, colorHex: String, iconName: String, notes: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedColorHex by remember {
        val parentIndex = OrbitColors.nodeColors.indexOfFirst { it.hex.equals(parentCentralNode.colorHex, ignoreCase = true) }
        val nextIndex = if (parentIndex >= 0) (parentIndex + 1) % OrbitColors.nodeColors.size else 0
        mutableStateOf(OrbitColors.nodeColors[nextIndex].hex)
    }
    var selectedIconName by remember { mutableStateOf("") }
    var showAllIcons by remember { mutableStateOf(false) }

    val activeColor = OrbitColors.parseColor(selectedColorHex)
    val textColor = OrbitColors.getContrastingTextColor(activeColor)
    val activeIconVector = OrbitIcons.getIcon(selectedIconName)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SleekSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorderSubtle),
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("add_node_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(activeColor)
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (activeIconVector != null) {
                            Icon(
                                imageVector = activeIconVector,
                                contentDescription = null,
                                tint = textColor,
                                modifier = Modifier.size(22.dp)
                            )
                        } else {
                            Text(
                                text = title.take(2).uppercase().ifBlank { "+" },
                                color = textColor,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.add_node_dialog_title),
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.orbiting_parent_format, parentCentralNode.title),
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.node_title_label)) },
                    placeholder = { Text(stringResource(R.string.node_title_placeholder)) },
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
                        .testTag("add_node_title_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Color Palette Swatches
                Text(
                    text = stringResource(R.string.color_theme_label),
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OrbitColors.nodeColors.take(10).forEach { item ->
                        val isSelected = item.hex.equals(selectedColorHex, ignoreCase = true)
                        val checkTint = OrbitColors.getContrastingTextColor(item.color)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(item.color)
                                .clickable { selectedColorHex = item.hex }
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) SleekPrimary else SleekBorderSubtle,
                                    shape = CircleShape
                                ),
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
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Icon Selection
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.icon_symbol_label),
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(onClick = { showAllIcons = !showAllIcons }) {
                        Text(
                            text = if (showAllIcons) stringResource(R.string.action_less) else stringResource(R.string.action_more),
                            color = SleekPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                val iconList = if (showAllIcons) OrbitIcons.icons else OrbitIcons.icons.take(12)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    iconList.forEach { iconItem ->
                        val isSelected = if (iconItem.key == "none") {
                            selectedIconName.isBlank() || selectedIconName.equals("none", ignoreCase = true)
                        } else {
                            iconItem.key.equals(selectedIconName, ignoreCase = true)
                        }
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) activeColor.copy(alpha = 0.2f) else SleekSurfaceVariant)
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) SleekPrimary else SleekBorderSubtle,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    selectedIconName = if (iconItem.key == "none") "" else iconItem.key
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (iconItem.key == "none") {
                                Text(
                                    text = stringResource(R.string.icon_none),
                                    color = if (isSelected) SleekPrimary else TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Icon(
                                    imageVector = iconItem.icon,
                                    contentDescription = iconItem.label,
                                    tint = if (isSelected) SleekPrimary else TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Optional Quick Note
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.quick_note_label)) },
                    placeholder = { Text(stringResource(R.string.quick_note_placeholder)) },
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

                Spacer(modifier = Modifier.height(20.dp))

                // Actions
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
                                onConfirm(title.trim(), selectedColorHex, selectedIconName, notes.trim())
                            }
                        },
                        enabled = title.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SleekPrimary,
                            contentColor = if (AppTheme.colors.isDark) Color(0xFF1F2D60) else Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("confirm_add_node_button")
                    ) {
                        Text(stringResource(R.string.action_create_node_confirm), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

