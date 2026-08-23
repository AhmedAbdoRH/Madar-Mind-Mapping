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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GeneratingTokens
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.ChecklistItem
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
fun NodeDetailDialog(
    node: MindNodeEntity,
    onDismiss: () -> Unit,
    onSave: (id: String, title: String, colorHex: String, iconName: String, notes: String, checklist: List<ChecklistItem>, linkUrl: String) -> Unit,
    onDelete: (id: String) -> Unit,
    onPutInPocket: (() -> Unit)? = null
) {
    var title by remember { mutableStateOf(node.title) }
    var notes by remember { mutableStateOf(node.notes) }
    var linkUrl by remember { mutableStateOf(node.linkUrl) }
    var colorHex by remember { mutableStateOf(node.colorHex) }
    var iconName by remember { mutableStateOf(node.iconName) }
    var checklistItems by remember { mutableStateOf(node.checklist) }
    var newChecklistText by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showAllIcons by remember { mutableStateOf(false) }

    val activeColor = OrbitColors.parseColor(colorHex)
    val textColor = OrbitColors.getContrastingTextColor(activeColor)
    val activeIconVector = OrbitIcons.getIcon(iconName)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SleekSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorderSubtle),
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("node_detail_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header with Preview Planet
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
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
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text(
                                text = title.take(2).uppercase().ifBlank { "•" },
                                color = textColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (node.parentId == null) "Central Map Core" else "Idea Node",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = title.ifBlank { "Unnamed Idea" },
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
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
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Tabs: [Notes, Tasks, Style, Link]
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = SleekSurfaceVariant,
                    contentColor = TextPrimary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = SleekPrimary
                        )
                    },
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Notes", fontSize = 12.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal)
                                if (notes.isNotBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .padding(start = 4.dp)
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(SleekPrimary)
                                    )
                                }
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Tasks", fontSize = 12.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
                                if (checklistItems.isNotEmpty()) {
                                    Text(
                                        text = " (${checklistItems.count { it.isDone }}/${checklistItems.size})",
                                        fontSize = 10.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Style", fontSize = 12.sp, fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("Link", fontSize = 12.sp, fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tab Content
                when (selectedTab) {
                    0 -> {
                        // Notes Tab (Primary default)
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Detailed Notes & Thoughts") },
                            placeholder = { Text("Write notes, ideas, descriptions, or reminders...") },
                            minLines = 6,
                            maxLines = 10,
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
                    }

                    1 -> {
                        // Tasks / Checklist Tab
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = newChecklistText,
                                onValueChange = { newChecklistText = it },
                                placeholder = { Text("Add new task...") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = SleekPrimary,
                                    unfocusedBorderColor = SleekBorder,
                                    focusedContainerColor = SleekSurface,
                                    unfocusedContainerColor = SleekSurfaceVariant
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    if (newChecklistText.isNotBlank()) {
                                        checklistItems = checklistItems + ChecklistItem(text = newChecklistText.trim())
                                        newChecklistText = ""
                                    }
                                },
                                enabled = newChecklistText.isNotBlank(),
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(if (newChecklistText.isNotBlank()) SleekPrimary else SleekSurfaceVariant)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add task",
                                    tint = if (newChecklistText.isNotBlank()) Color.White else TextTertiary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (checklistItems.isEmpty()) {
                            Text(
                                text = "No tasks yet. Break this idea down into actionable items.",
                                color = TextTertiary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            checklistItems.forEachIndexed { index, item ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(SleekSurfaceVariant)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Checkbox(
                                        checked = item.isDone,
                                        onCheckedChange = { checked ->
                                            val updated = checklistItems.toMutableList()
                                            updated[index] = item.copy(isDone = checked)
                                            checklistItems = updated
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = SleekPrimary,
                                            checkmarkColor = Color.White
                                        )
                                    )
                                    Text(
                                        text = item.text,
                                        color = if (item.isDone) TextSecondary else TextPrimary,
                                        textDecoration = if (item.isDone) TextDecoration.LineThrough else TextDecoration.None,
                                        fontSize = 14.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            checklistItems = checklistItems.filterIndexed { i, _ -> i != index }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = TextTertiary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // Style Tab: Color swatches & Optional Icons
                        Text(
                            text = "Color Theme",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OrbitColors.nodeColors.forEach { item ->
                                val isSelected = item.hex.equals(colorHex, ignoreCase = true)
                                val checkTint = OrbitColors.getContrastingTextColor(item.color)
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(item.color)
                                        .clickable { colorHex = item.hex }
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

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Icon Symbol (Optional)",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            TextButton(onClick = { showAllIcons = !showAllIcons }) {
                                Text(
                                    text = if (showAllIcons) "Show Less" else "Show All",
                                    color = SleekPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        val iconsToShow = if (showAllIcons) OrbitIcons.icons else OrbitIcons.icons.take(14)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            iconsToShow.forEach { iconItem ->
                                val isSelected = if (iconItem.key == "none") {
                                    iconName.isBlank() || iconName.equals("none", ignoreCase = true)
                                } else {
                                    iconItem.key.equals(iconName, ignoreCase = true)
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
                                            iconName = if (iconItem.key == "none") "" else iconItem.key
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (iconItem.key == "none") {
                                        Text(
                                            text = "None",
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
                    }

                    3 -> {
                        // Link Tab
                        OutlinedTextField(
                            value = linkUrl,
                            onValueChange = { linkUrl = it },
                            label = { Text("Web Link / Reference URL") },
                            placeholder = { Text("https://example.com/spec") },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Link, contentDescription = null, tint = SleekPrimary)
                            },
                            singleLine = true,
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
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = SleekBorderSubtle)
                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Action Buttons (Delete, Pocket, Cancel, Save)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (node.parentId != null) {
                            IconButton(
                                onClick = { showDeleteConfirm = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Planet",
                                    tint = Color(0xFFBA1A1A)
                                )
                            }
                        }
                        if (onPutInPocket != null) {
                            IconButton(
                                onClick = onPutInPocket
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GeneratingTokens,
                                    contentDescription = "Hold in Orbital Pocket",
                                    tint = SleekPrimary
                                )
                            }
                        }
                    }

                    Row {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel", color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (title.isNotBlank()) {
                                    onSave(node.id, title.trim(), colorHex, iconName, notes.trim(), checklistItems, linkUrl.trim())
                                }
                            },
                            enabled = title.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SleekPrimary,
                                contentColor = if (AppTheme.colors.isDark) Color(0xFF1F2D60) else Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("save_node_edits_button")
                        ) {
                            Text("Save Changes", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Idea & Orbits?", color = TextPrimary) },
            text = {
                Text(
                    "Are you sure you want to remove '$title' and all of its orbiting sub-ideas?",
                    color = TextSecondary
                )
            },
            containerColor = SleekSurface,
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(node.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBA1A1A))
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

