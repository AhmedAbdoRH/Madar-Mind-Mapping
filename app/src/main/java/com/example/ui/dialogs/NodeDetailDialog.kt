package com.example.ui.dialogs

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Shortcut
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.Event
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import com.example.ui.util.NodeDueDateHelper
import com.example.ui.util.DueDateStatus
import java.util.Calendar
import androidx.compose.ui.text.style.TextOverflow
import com.example.ui.dialogs.NodeJsonDialog
import com.example.ui.dialogs.NodeJsonPayload
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
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabPosition
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.R
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
import com.example.ui.util.ColorApplyScope
import com.example.ui.util.ImageStorageHelper
import com.example.ui.util.OrbitColors
import com.example.ui.util.OrbitIcons
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NodeDetailDialog(
    node: MindNodeEntity,
    onDismiss: () -> Unit,
    onSave: (id: String, title: String, colorHex: String, iconName: String, notes: String, checklist: List<ChecklistItem>, linkUrl: String, imageUri: String?, progress: Int?, impact: Int?, dueDate: Long?) -> Unit,
    onDelete: (id: String) -> Unit,
    onPutInPocket: (() -> Unit)? = null,
    onApplyColorScope: ((ColorApplyScope, String) -> Unit)? = null,
    onAddShortcut: (() -> Unit)? = null,
    hasChildren: Boolean = true,
    hasSiblings: Boolean = true,
    directChildren: List<MindNodeEntity> = emptyList(),
    allNodes: List<MindNodeEntity> = emptyList(),
    onUpdateChildrenProgress: ((Map<String, Int>) -> Unit)? = null,
    onImportJsonSubtree: ((parentId: String, payload: NodeJsonPayload) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    // Dynamic direct children progress map for real-time adjustments
    var modifiedChildProgressMap by remember(directChildren) {
        mutableStateOf(directChildren.associate { it.id to (it.progress ?: 0) })
    }

    // Direct children progress calculation (average of direct child ratings/checklists)
    val dynamicCalculatedProgress = remember(directChildren, modifiedChildProgressMap) {
        if (directChildren.isEmpty()) null
        else {
            val scores = directChildren.map { child ->
                when {
                    child.checklist.isNotEmpty() -> {
                        val done = child.checklist.count { it.isDone }
                        (done.toFloat() / child.checklist.size) * 10f
                    }
                    else -> (modifiedChildProgressMap[child.id] ?: child.progress ?: 0).toFloat()
                }
            }
            kotlin.math.round(scores.average()).toInt().coerceIn(0, 10)
        }
    }

    var title by remember { mutableStateOf(node.title) }
    var notes by remember { mutableStateOf(node.notes) }
    var linkUrl by remember { mutableStateOf(node.linkUrl) }
    var colorHex by remember { mutableStateOf(node.colorHex) }
    var iconName by remember { mutableStateOf(node.iconName) }
    var imageUri by remember { mutableStateOf(node.imageUri) }
    var progressEnabled by remember { mutableStateOf(node.progress != null) }
    var isAutoProgress by remember {
        mutableStateOf(directChildren.isNotEmpty() && (node.progress == null || node.progress == dynamicCalculatedProgress))
    }
    var progressValue by remember {
        mutableIntStateOf(
            if (isAutoProgress && dynamicCalculatedProgress != null) dynamicCalculatedProgress else (node.progress ?: 5)
        )
    }

    // Synchronize progress value dynamically when in Auto Progress mode
    LaunchedEffect(dynamicCalculatedProgress, isAutoProgress) {
        if (isAutoProgress && dynamicCalculatedProgress != null) {
            progressValue = dynamicCalculatedProgress
        }
    }

    var impactEnabled by remember { mutableStateOf(node.impact != null) }
    var impactValue by remember { mutableIntStateOf(node.impact ?: 3) }
    var dueDateEnabled by remember { mutableStateOf(node.dueDate != null) }
    var dueDateValue by remember { mutableStateOf<Long?>(node.dueDate) }
    var checklistItems by remember { mutableStateOf(node.checklist) }
    var newChecklistText by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedColorScope by remember { mutableStateOf(ColorApplyScope.THIS_NODE_ONLY) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showJsonDialog by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val savedPath = ImageStorageHelper.saveImageToInternalStorage(context, uri)
                if (savedPath != null) {
                    imageUri = savedPath
                } else {
                    imageUri = uri.toString()
                }
            }
        }
    }

    val activeColor = OrbitColors.parseColor(colorHex)
    val textColor = OrbitColors.getContrastingTextColor(activeColor)
    val activeIconVector = OrbitIcons.getIcon(iconName)

    Dialog(onDismissRequest = onDismiss) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = SleekSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorderSubtle),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 700.dp)
                    .padding(vertical = 12.dp)
                    .testTag("node_detail_dialog")
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Pinned Header Section (Header + Title + Tabs)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 18.dp, start = 20.dp, end = 20.dp, bottom = 10.dp)
                    ) {
                        // Header with Preview Planet
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(activeColor)
                                    .border(1.dp, SleekBorderSubtle, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!imageUri.isNullOrBlank()) {
                                    AsyncImage(
                                        model = imageUri,
                                        contentDescription = title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else if (activeIconVector != null) {
                                    Icon(
                                        imageVector = activeIconVector,
                                        contentDescription = null,
                                        tint = textColor,
                                        modifier = Modifier.size(26.dp)
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
                                    text = if (node.parentId == null) stringResource(R.string.central_map_core) else stringResource(R.string.idea_node),
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = title.ifBlank { stringResource(R.string.unnamed_idea) },
                                    color = TextPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }

                            // JSON Action Button
                            IconButton(
                                onClick = { showJsonDialog = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DataObject,
                                    contentDescription = "بيانات العقدة JSON",
                                    tint = SleekPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            if (onAddShortcut != null) {
                                IconButton(
                                    onClick = onAddShortcut,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shortcut,
                                        contentDescription = "وضع اختصار في الشاشة الرئيسية",
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = TextTertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Title Input
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text(stringResource(R.string.node_title_label)) },
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
                                .focusRequester(focusRequester)
                                .testTag("node_detail_title_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Tabs: [Notes, Tasks, Progress, Style, Image, Link]
                        ScrollableTabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = SleekSurfaceVariant,
                            contentColor = TextPrimary,
                            edgePadding = 6.dp,
                            indicator = { tabPositions: List<TabPosition> ->
                                if (selectedTab < tabPositions.size) {
                                    TabRowDefaults.SecondaryIndicator(
                                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                        color = SleekPrimary
                                    )
                                }
                            },
                            modifier = Modifier.clip(RoundedCornerShape(12.dp))
                        ) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(stringResource(R.string.tab_notes), fontSize = 11.5.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal)
                                        if (notes.isNotBlank()) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(start = 3.dp)
                                                    .size(5.dp)
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
                                        Text(stringResource(R.string.tab_tasks), fontSize = 11.5.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
                                        if (checklistItems.isNotEmpty()) {
                                            Text(
                                                text = " (${checklistItems.count { it.isDone }})",
                                                fontSize = 9.sp,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                }
                            )
                            Tab(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(stringResource(R.string.tab_progress), fontSize = 11.5.sp, fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal)
                                        if (progressEnabled) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(start = 3.dp)
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF10B981))
                                            )
                                        }
                                    }
                                }
                            )
                            Tab(
                                selected = selectedTab == 3,
                                onClick = { selectedTab = 3 },
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(stringResource(R.string.tab_impact), fontSize = 11.5.sp, fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal)
                                        if (impactEnabled) {
                                            val dotColor = when (impactValue) {
                                                1 -> Color(0xFF94A3B8)
                                                2 -> Color(0xFF38BDF8)
                                                3 -> Color(0xFFF59E0B)
                                                4 -> Color(0xFFF97316)
                                                5 -> Color(0xFFEF4444)
                                                else -> Color(0xFF94A3B8)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .padding(start = 3.dp)
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(dotColor)
                                            )
                                        }
                                    }
                                }
                            )
                            Tab(
                                selected = selectedTab == 4,
                                onClick = { selectedTab = 4 },
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(stringResource(R.string.tab_due_date), fontSize = 11.5.sp, fontWeight = if (selectedTab == 4) FontWeight.Bold else FontWeight.Normal)
                                        if (dueDateEnabled && dueDateValue != null) {
                                            val status = NodeDueDateHelper.calculateStatus(dueDateValue)
                                            val statusColor = NodeDueDateHelper.getStatusGlowColor(status)
                                            Box(
                                                modifier = Modifier
                                                    .padding(start = 3.dp)
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(statusColor)
                                            )
                                        }
                                    }
                                }
                            )
                            Tab(
                                selected = selectedTab == 5,
                                onClick = { selectedTab = 5 },
                                text = { Text(stringResource(R.string.tab_style), fontSize = 11.5.sp, fontWeight = if (selectedTab == 5) FontWeight.Bold else FontWeight.Normal) }
                            )
                            Tab(
                                selected = selectedTab == 6,
                                onClick = { selectedTab = 6 },
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(stringResource(R.string.tab_image), fontSize = 11.5.sp, fontWeight = if (selectedTab == 6) FontWeight.Bold else FontWeight.Normal)
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("👑", fontSize = 10.sp)
                                        if (!imageUri.isNullOrBlank()) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(start = 3.dp)
                                                    .size(5.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF10B981))
                                            )
                                        }
                                    }
                                }
                            )
                            Tab(
                                selected = selectedTab == 7,
                                onClick = { selectedTab = 7 },
                                text = { Text(stringResource(R.string.tab_link), fontSize = 11.5.sp, fontWeight = if (selectedTab == 7) FontWeight.Bold else FontWeight.Normal) }
                            )
                        }
                    }

                    HorizontalDivider(color = SleekBorderSubtle)

                    // Scrollable Tab Body Content
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        when (selectedTab) {
                    0 -> {
                        // Notes Tab (Primary default)
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text(stringResource(R.string.tab_notes)) },
                            placeholder = { Text(stringResource(R.string.notes_placeholder)) },
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
                                placeholder = { Text(stringResource(R.string.new_task_placeholder)) },
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
                                    contentDescription = stringResource(R.string.add_task_cd),
                                    tint = if (newChecklistText.isNotBlank()) Color.White else TextTertiary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (checklistItems.isEmpty()) {
                            Text(
                                text = stringResource(R.string.no_tasks_yet),
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
                        // Progress Tab (نظام التقدم من 0 إلى 10)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = SleekSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (progressEnabled) Color(0xFF10B981) else SleekBorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.enable_progress_switch),
                                            color = TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = stringResource(R.string.progress_desc),
                                            color = TextSecondary,
                                            fontSize = 11.5.sp
                                        )
                                    }
                                    Switch(
                                        checked = progressEnabled,
                                        onCheckedChange = { progressEnabled = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = Color(0xFF10B981)
                                        )
                                    )
                                }

                                if (progressEnabled) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    HorizontalDivider(color = SleekBorderSubtle)
                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Auto vs Manual Toggle if the node has direct children
                                    if (directChildren.isNotEmpty()) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = SleekSurfaceElevated,
                                            border = BorderStroke(1.dp, SleekBorderSubtle),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .padding(4.dp)
                                                    .fillMaxWidth()
                                            ) {
                                                // Automatic Mode Tab
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = if (isAutoProgress) SleekPrimary else Color.Transparent,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clickable {
                                                            isAutoProgress = true
                                                            if (dynamicCalculatedProgress != null) {
                                                                progressValue = dynamicCalculatedProgress
                                                            }
                                                        }
                                                ) {
                                                    Text(
                                                        text = "⚡ تلقائي من الأبناء (${directChildren.size})",
                                                        color = if (isAutoProgress) Color.White else TextSecondary,
                                                        fontSize = 12.sp,
                                                        fontWeight = if (isAutoProgress) FontWeight.Bold else FontWeight.Normal,
                                                        textAlign = TextAlign.Center,
                                                        modifier = Modifier.padding(vertical = 8.dp)
                                                    )
                                                }

                                                // Manual Mode Tab
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = if (!isAutoProgress) SleekPrimary else Color.Transparent,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clickable { isAutoProgress = false }
                                                ) {
                                                    Text(
                                                        text = "🎛️ تقييم يدوي",
                                                        color = if (!isAutoProgress) Color.White else TextSecondary,
                                                        fontSize = 12.sp,
                                                        fontWeight = if (!isAutoProgress) FontWeight.Bold else FontWeight.Normal,
                                                        textAlign = TextAlign.Center,
                                                        modifier = Modifier.padding(vertical = 8.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        if (isAutoProgress) {
                                            Surface(
                                                shape = RoundedCornerShape(14.dp),
                                                color = SleekPrimary.copy(alpha = 0.08f),
                                                border = BorderStroke(1.dp, SleekPrimary.copy(alpha = 0.25f)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text(
                                                            text = "التقييم التلقائي المحسوب ديناميكياً:",
                                                            fontSize = 12.sp,
                                                            color = TextSecondary
                                                        )
                                                        Text(
                                                            text = "$progressValue / 10 (${progressValue * 10}%)",
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = SleekPrimary
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.height(10.dp))
                                                    HorizontalDivider(color = SleekBorderSubtle)
                                                    Spacer(modifier = Modifier.height(8.dp))

                                                    Text(
                                                        text = "تحكم ديناميكي بتقدم الأبناء المباشرين:",
                                                        fontSize = 11.5.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = TextPrimary
                                                    )

                                                    Spacer(modifier = Modifier.height(6.dp))

                                                    Column(
                                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        directChildren.forEach { child ->
                                                            val childColor = remember(child.colorHex) { OrbitColors.parseColor(child.colorHex) }
                                                            val childIcon = remember(child.iconName) { OrbitIcons.getIcon(child.iconName) }
                                                            val hasChildChecklist = child.checklist.isNotEmpty()
                                                            val currentProg = modifiedChildProgressMap[child.id] ?: child.progress ?: 0

                                                            Surface(
                                                                shape = RoundedCornerShape(10.dp),
                                                                color = SleekSurfaceElevated,
                                                                border = BorderStroke(1.dp, SleekBorderSubtle),
                                                                modifier = Modifier.fillMaxWidth()
                                                            ) {
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                                    modifier = Modifier
                                                                        .fillMaxWidth()
                                                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                                                ) {
                                                                    Row(
                                                                        verticalAlignment = Alignment.CenterVertically,
                                                                        modifier = Modifier.weight(1f)
                                                                    ) {
                                                                        Box(
                                                                            modifier = Modifier
                                                                                .size(24.dp)
                                                                                .clip(CircleShape)
                                                                                .background(childColor),
                                                                            contentAlignment = Alignment.Center
                                                                        ) {
                                                                            if (childIcon != null) {
                                                                                Icon(
                                                                                    imageVector = childIcon,
                                                                                    contentDescription = null,
                                                                                    tint = OrbitColors.getContrastingTextColor(childColor),
                                                                                    modifier = Modifier.size(13.dp)
                                                                                )
                                                                            }
                                                                        }
                                                                        Spacer(modifier = Modifier.width(8.dp))
                                                                        Text(
                                                                            text = child.title,
                                                                            fontSize = 12.sp,
                                                                            color = TextPrimary,
                                                                            fontWeight = FontWeight.Medium,
                                                                            maxLines = 1,
                                                                            overflow = TextOverflow.Ellipsis
                                                                        )
                                                                    }

                                                                    if (hasChildChecklist) {
                                                                        val done = child.checklist.count { it.isDone }
                                                                        val total = child.checklist.size
                                                                        Surface(
                                                                            shape = RoundedCornerShape(6.dp),
                                                                            color = SleekSurface,
                                                                            border = BorderStroke(1.dp, SleekBorderSubtle)
                                                                        ) {
                                                                            Text(
                                                                                text = "قائمة $done/$total",
                                                                                fontSize = 10.5.sp,
                                                                                color = Color(0xFF10B981),
                                                                                fontWeight = FontWeight.Bold,
                                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                            )
                                                                        }
                                                                    } else {
                                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                                            IconButton(
                                                                                onClick = {
                                                                                    val nextVal = (currentProg - 1).coerceAtLeast(0)
                                                                                    modifiedChildProgressMap = modifiedChildProgressMap + (child.id to nextVal)
                                                                                },
                                                                                enabled = currentProg > 0,
                                                                                modifier = Modifier
                                                                                    .size(26.dp)
                                                                                    .clip(CircleShape)
                                                                                    .background(SleekSurface)
                                                                            ) {
                                                                                Text("-", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                                                            }

                                                                            Text(
                                                                                text = "$currentProg/10",
                                                                                fontSize = 11.5.sp,
                                                                                fontWeight = FontWeight.Bold,
                                                                                color = Color(0xFF10B981),
                                                                                modifier = Modifier.padding(horizontal = 6.dp)
                                                                            )

                                                                            IconButton(
                                                                                onClick = {
                                                                                    val nextVal = (currentProg + 1).coerceAtMost(10)
                                                                                    modifiedChildProgressMap = modifiedChildProgressMap + (child.id to nextVal)
                                                                                },
                                                                                enabled = currentProg < 10,
                                                                                modifier = Modifier
                                                                                    .size(26.dp)
                                                                                    .clip(CircleShape)
                                                                                    .background(SleekSurface)
                                                                            ) {
                                                                                Text("+", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(12.dp))
                                        }
                                    }

                                    // Progress Level Header & Badge
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = stringResource(R.string.progress_level_label),
                                            color = TextSecondary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (progressValue == 10) Color(0xFF059669) else SleekSurfaceElevated,
                                            border = BorderStroke(1.dp, if (progressValue == 10) Color(0xFF10B981) else SleekBorderSubtle)
                                        ) {
                                            Text(
                                                text = if (progressValue == 10) stringResource(R.string.progress_completed_badge) else "$progressValue / 10 (${progressValue * 10}%)",
                                                color = if (progressValue == 10) Color.White else Color(0xFF10B981),
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Interactive 10-piece visual representation preview
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        for (i in 1..10) {
                                            val isFilled = i <= progressValue
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(28.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (isFilled) Color(0xFF10B981) else SleekSurfaceElevated)
                                                    .border(
                                                        1.dp,
                                                        if (isFilled) Color(0xFF059669) else SleekBorderSubtle,
                                                        RoundedCornerShape(6.dp)
                                                    )
                                                    .clickable {
                                                        isAutoProgress = false
                                                        progressValue = i
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "$i",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isFilled) Color.White else TextTertiary
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Quick Level Stepper Buttons (0, -, +)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        TextButton(
                                            onClick = {
                                                isAutoProgress = false
                                                progressValue = 0
                                            },
                                            colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
                                        ) {
                                            Text("إعادة ضبط (0)", fontSize = 12.sp)
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = {
                                                    isAutoProgress = false
                                                    if (progressValue > 0) progressValue--
                                                },
                                                enabled = progressValue > 0,
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(SleekSurfaceElevated)
                                            ) {
                                                Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Text(
                                                text = "$progressValue",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SleekPrimary
                                            )

                                            Spacer(modifier = Modifier.width(12.dp))

                                            IconButton(
                                                onClick = {
                                                    isAutoProgress = false
                                                    if (progressValue < 10) progressValue++
                                                },
                                                enabled = progressValue < 10,
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(SleekSurfaceElevated)
                                            ) {
                                                Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    3 -> {
                        // Impact Power Tab (قوة التأثير: 5 مستويات من بدون تأثير إلى مؤثر جداً)
                        val activeImpactColor = when (impactValue) {
                            1 -> Color(0xFF94A3B8)
                            2 -> Color(0xFF38BDF8)
                            3 -> Color(0xFFF59E0B)
                            4 -> Color(0xFFF97316)
                            5 -> Color(0xFFEF4444)
                            else -> Color(0xFF94A3B8)
                        }
                        val activeImpactName = when (impactValue) {
                            1 -> stringResource(R.string.impact_level_1)
                            2 -> stringResource(R.string.impact_level_2)
                            3 -> stringResource(R.string.impact_level_3)
                            4 -> stringResource(R.string.impact_level_4)
                            5 -> stringResource(R.string.impact_level_5)
                            else -> stringResource(R.string.impact_level_1)
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = SleekSurfaceVariant,
                            border = BorderStroke(1.dp, if (impactEnabled) activeImpactColor else SleekBorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.enable_impact_switch),
                                            color = TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = stringResource(R.string.impact_desc),
                                            color = TextSecondary,
                                            fontSize = 11.5.sp
                                        )
                                    }
                                    Switch(
                                        checked = impactEnabled,
                                        onCheckedChange = { impactEnabled = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = activeImpactColor
                                        )
                                    )
                                }

                                if (impactEnabled) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    HorizontalDivider(color = SleekBorderSubtle)
                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Active Level Header & Selected Badge
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = stringResource(R.string.impact_level_label),
                                            color = TextSecondary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = activeImpactColor.copy(alpha = 0.15f),
                                            border = BorderStroke(1.2.dp, activeImpactColor)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(activeImpactColor)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "⚡ $activeImpactName ($impactValue/5)",
                                                    color = activeImpactColor,
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Compact 5-level segmented bar
                                    val levels = listOf(
                                        Triple(1, stringResource(R.string.impact_level_1), Color(0xFF94A3B8)),
                                        Triple(2, stringResource(R.string.impact_level_2), Color(0xFF38BDF8)),
                                        Triple(3, stringResource(R.string.impact_level_3), Color(0xFFF59E0B)),
                                        Triple(4, stringResource(R.string.impact_level_4), Color(0xFFF97316)),
                                        Triple(5, stringResource(R.string.impact_level_5), Color(0xFFEF4444))
                                    )

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        levels.forEach { (lvl, name, color) ->
                                            val isSelected = impactValue == lvl
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isSelected) color.copy(alpha = 0.2f) else SleekSurfaceElevated,
                                                border = BorderStroke(
                                                    width = if (isSelected) 1.5.dp else 1.dp,
                                                    color = if (isSelected) color else SleekBorderSubtle
                                                ),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable { impactValue = lvl }
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "⚡ $lvl",
                                                        color = if (isSelected) color else TextPrimary,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = name.take(6),
                                                        color = if (isSelected) color else TextSecondary,
                                                        fontSize = 9.5.sp,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Stepper controls
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        TextButton(
                                            onClick = { impactValue = 1 },
                                            colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
                                        ) {
                                            Text(stringResource(R.string.impact_level_1), fontSize = 11.5.sp)
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { if (impactValue > 1) impactValue-- },
                                                enabled = impactValue > 1,
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(SleekSurfaceElevated)
                                            ) {
                                                Text("-", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            }

                                            Spacer(modifier = Modifier.width(10.dp))

                                            Text(
                                                text = "$impactValue / 5",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = activeImpactColor
                                            )

                                            Spacer(modifier = Modifier.width(10.dp))

                                            IconButton(
                                                onClick = { if (impactValue < 5) impactValue++ },
                                                enabled = impactValue < 5,
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(SleekSurfaceElevated)
                                            ) {
                                                Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    4 -> {
                        // Due Date Tab
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = SleekSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (dueDateEnabled && dueDateValue != null) NodeDueDateHelper.getStatusGlowColor(NodeDueDateHelper.calculateStatus(dueDateValue)).copy(alpha = 0.5f) else SleekBorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (dueDateEnabled && dueDateValue != null) {
                                                        NodeDueDateHelper.getStatusGlowColor(NodeDueDateHelper.calculateStatus(dueDateValue)).copy(alpha = 0.15f)
                                                    } else SleekSurfaceElevated
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CalendarMonth,
                                                contentDescription = null,
                                                tint = if (dueDateEnabled && dueDateValue != null) {
                                                    NodeDueDateHelper.getStatusGlowColor(NodeDueDateHelper.calculateStatus(dueDateValue))
                                                } else TextSecondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = stringResource(R.string.due_date_title),
                                                color = TextPrimary,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = stringResource(R.string.enable_due_date_switch),
                                                color = TextSecondary,
                                                fontSize = 11.5.sp
                                            )
                                        }
                                    }
                                    Switch(
                                        checked = dueDateEnabled,
                                        onCheckedChange = { enabled ->
                                            dueDateEnabled = enabled
                                            if (enabled && dueDateValue == null) {
                                                val c = Calendar.getInstance().apply {
                                                    set(Calendar.HOUR_OF_DAY, 23)
                                                    set(Calendar.MINUTE, 59)
                                                    set(Calendar.SECOND, 0)
                                                    set(Calendar.MILLISECOND, 0)
                                                }
                                                dueDateValue = c.timeInMillis
                                            }
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = SleekPrimary
                                        )
                                    )
                                }

                                if (dueDateEnabled) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    HorizontalDivider(color = SleekBorderSubtle)
                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Status Badge & Relative Warning Card
                                    val currentStatus = NodeDueDateHelper.calculateStatus(dueDateValue)
                                    val statusColor = NodeDueDateHelper.getStatusGlowColor(currentStatus)
                                    val relativeLabel = NodeDueDateHelper.formatRelativeDueDate(dueDateValue)
                                    val exactLabel = NodeDueDateHelper.formatFullDate(dueDateValue)

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = statusColor.copy(alpha = 0.12f),
                                        border = androidx.compose.foundation.BorderStroke(1.2.dp, statusColor.copy(alpha = 0.7f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(10.dp)
                                                            .clip(CircleShape)
                                                            .background(statusColor)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = relativeLabel,
                                                        color = statusColor,
                                                        fontSize = 13.5.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                if (currentStatus == DueDateStatus.APPROACHING || currentStatus == DueDateStatus.DUE_TODAY || currentStatus == DueDateStatus.OVERDUE) {
                                                    Icon(
                                                        imageVector = Icons.Default.WarningAmber,
                                                        contentDescription = null,
                                                        tint = statusColor,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }

                                            if (exactLabel.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "📅 $exactLabel",
                                                    color = TextPrimary,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Quick Selection Chips
                                    Text(
                                        text = "مواعيد سريعة مقترحة:",
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        // Today (End of Day)
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = SleekSurfaceElevated,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorderSubtle),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    val c = Calendar.getInstance().apply {
                                                        set(Calendar.HOUR_OF_DAY, 23)
                                                        set(Calendar.MINUTE, 59)
                                                        set(Calendar.SECOND, 0)
                                                    }
                                                    dueDateValue = c.timeInMillis
                                                }
                                        ) {
                                            Text(
                                                text = stringResource(R.string.due_date_quick_today),
                                                fontSize = 11.5.sp,
                                                color = TextPrimary,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(vertical = 8.dp)
                                            )
                                        }

                                        // Tomorrow
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = SleekSurfaceElevated,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorderSubtle),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    val c = Calendar.getInstance().apply {
                                                        add(Calendar.DAY_OF_YEAR, 1)
                                                        set(Calendar.HOUR_OF_DAY, 23)
                                                        set(Calendar.MINUTE, 59)
                                                        set(Calendar.SECOND, 0)
                                                    }
                                                    dueDateValue = c.timeInMillis
                                                }
                                        ) {
                                            Text(
                                                text = stringResource(R.string.due_date_quick_tomorrow),
                                                fontSize = 11.5.sp,
                                                color = TextPrimary,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(vertical = 8.dp)
                                            )
                                        }

                                        // 3 Days
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = SleekSurfaceElevated,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorderSubtle),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    val c = Calendar.getInstance().apply {
                                                        add(Calendar.DAY_OF_YEAR, 3)
                                                        set(Calendar.HOUR_OF_DAY, 23)
                                                        set(Calendar.MINUTE, 59)
                                                        set(Calendar.SECOND, 0)
                                                    }
                                                    dueDateValue = c.timeInMillis
                                                }
                                        ) {
                                            Text(
                                                text = stringResource(R.string.due_date_quick_3days),
                                                fontSize = 11.5.sp,
                                                color = TextPrimary,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(vertical = 8.dp)
                                            )
                                        }

                                        // 1 Week
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = SleekSurfaceElevated,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorderSubtle),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    val c = Calendar.getInstance().apply {
                                                        add(Calendar.DAY_OF_YEAR, 7)
                                                        set(Calendar.HOUR_OF_DAY, 23)
                                                        set(Calendar.MINUTE, 59)
                                                        set(Calendar.SECOND, 0)
                                                    }
                                                    dueDateValue = c.timeInMillis
                                                }
                                        ) {
                                            Text(
                                                text = stringResource(R.string.due_date_quick_week),
                                                fontSize = 11.5.sp,
                                                color = TextPrimary,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(vertical = 8.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Date Picker Button & Clear Button
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Button(
                                            onClick = {
                                                val c = Calendar.getInstance().apply {
                                                    dueDateValue?.let { timeInMillis = it }
                                                }
                                                val year = c.get(Calendar.YEAR)
                                                val month = c.get(Calendar.MONTH)
                                                val day = c.get(Calendar.DAY_OF_MONTH)

                                                DatePickerDialog(context, { _, pickedYear, pickedMonth, pickedDay ->
                                                    val hour = c.get(Calendar.HOUR_OF_DAY)
                                                    val minute = c.get(Calendar.MINUTE)
                                                    TimePickerDialog(context, { _, pickedHour, pickedMinute ->
                                                        val resultCal = Calendar.getInstance().apply {
                                                            set(Calendar.YEAR, pickedYear)
                                                            set(Calendar.MONTH, pickedMonth)
                                                            set(Calendar.DAY_OF_MONTH, pickedDay)
                                                            set(Calendar.HOUR_OF_DAY, pickedHour)
                                                            set(Calendar.MINUTE, pickedMinute)
                                                            set(Calendar.SECOND, 0)
                                                            set(Calendar.MILLISECOND, 0)
                                                        }
                                                        dueDateValue = resultCal.timeInMillis
                                                    }, hour, minute, false).show()
                                                }, year, month, day).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = SleekPrimary,
                                                contentColor = if (AppTheme.colors.isDark) Color(0xFF1F2D60) else Color.White
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Event,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(stringResource(R.string.due_date_pick_button), fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                        }

                                        TextButton(
                                            onClick = {
                                                dueDateValue = null
                                                dueDateEnabled = false
                                            },
                                            colors = ButtonDefaults.textButtonColors(
                                                contentColor = Color(0xFFBA1A1A)
                                            )
                                        ) {
                                            Text(stringResource(R.string.due_date_clear_button), fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    5 -> {
                        // Style Tab: Color swatches & Scope Propagation & Optional Icons
                        ColorSelectorRow(
                            selectedColorHex = colorHex,
                            onColorSelected = { colorHex = it },
                            mapId = node.mapId,
                            showSetAsDefaultOption = true,
                            showScopeSelector = true,
                            selectedScope = selectedColorScope,
                            onScopeSelected = { selectedColorScope = it },
                            onApplyScopeAction = { scope, hex ->
                                onApplyColorScope?.invoke(scope, hex)
                            },
                            showPrimaryColorAction = true,
                            onSetAsPrimaryColor = { hex ->
                                onApplyColorScope?.invoke(ColorApplyScope.SET_AS_PRIMARY_MAP_COLOR, hex)
                            },
                            hasChildren = hasChildren,
                            hasSiblings = hasSiblings
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        IconSelectorRow(
                            selectedIconName = iconName,
                            onIconSelected = { iconName = if (it == "none") "" else it },
                            activeColor = activeColor,
                            allowNone = true
                        )
                    }

                    6 -> {
                        // Image / Photo Planet Tab
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = SleekSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (!imageUri.isNullOrBlank()) SleekPrimary else SleekBorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                if (!imageUri.isNullOrBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .size(90.dp)
                                            .clip(CircleShape)
                                            .border(3.dp, activeColor, CircleShape)
                                            .shadow(8.dp, CircleShape)
                                    ) {
                                        AsyncImage(
                                            model = imageUri,
                                            contentDescription = "Node Photo Planet",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        text = stringResource(R.string.node_has_image),
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Text(
                                        text = stringResource(R.string.node_has_image_desc),
                                        color = TextSecondary,
                                        fontSize = 11.5.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Button(
                                            onClick = {
                                                photoPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = SleekPrimary.copy(alpha = 0.15f),
                                                contentColor = SleekPrimary
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AddPhotoAlternate,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(stringResource(R.string.change_photo), fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        TextButton(
                                            onClick = { imageUri = null },
                                            colors = ButtonDefaults.textButtonColors(
                                                contentColor = Color(0xFFBA1A1A)
                                            )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(stringResource(R.string.remove_photo), fontSize = 12.sp)
                                        }
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(CircleShape)
                                            .background(SleekSurfaceElevated)
                                            .border(1.dp, SleekBorderSubtle, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Image,
                                            contentDescription = null,
                                            tint = SleekPrimary,
                                            modifier = Modifier.size(34.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = stringResource(R.string.node_image_title),
                                            color = TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                                            border = BorderStroke(1.dp, Color(0xFFF59E0B))
                                        ) {
                                            Text(
                                                text = "👑 PRO",
                                                color = Color(0xFFF59E0B),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = stringResource(R.string.node_image_subtitle),
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Button(
                                        onClick = {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = SleekPrimary,
                                            contentColor = if (AppTheme.colors.isDark) Color(0xFF1F2D60) else Color.White
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AddPhotoAlternate,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(stringResource(R.string.choose_photo), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    7 -> {
                        // Link Tab
                        OutlinedTextField(
                            value = linkUrl,
                            onValueChange = { linkUrl = it },
                            label = { Text(stringResource(R.string.link_url_label)) },
                            placeholder = { Text(stringResource(R.string.link_url_placeholder)) },
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
            }

            HorizontalDivider(color = SleekBorderSubtle)

            // Pinned Bottom Action Buttons (Delete, Pocket, Cancel, Save)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (node.parentId != null) {
                                IconButton(
                                    onClick = { showDeleteConfirm = true }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.delete_node_cd),
                                        tint = Color(0xFFBA1A1A)
                                    )
                                }
                            }
                        }

                        Row {
                            TextButton(onClick = onDismiss) {
                                Text(stringResource(R.string.action_cancel), color = TextSecondary)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (title.isNotBlank()) {
                                        if (modifiedChildProgressMap.isNotEmpty() && isAutoProgress) {
                                            onUpdateChildrenProgress?.invoke(modifiedChildProgressMap)
                                        }
                                        val finalProgress = if (progressEnabled) progressValue.coerceIn(0, 10) else null
                                        val finalImpact = if (impactEnabled) impactValue.coerceIn(1, 5) else null
                                        val finalDueDate = if (dueDateEnabled) dueDateValue else null
                                        onSave(node.id, title.trim(), colorHex, iconName, notes.trim(), checklistItems, linkUrl.trim(), imageUri, finalProgress, finalImpact, finalDueDate)
                                        if (selectedColorScope != ColorApplyScope.THIS_NODE_ONLY) {
                                            onApplyColorScope?.invoke(selectedColorScope, colorHex)
                                        }
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
                                Text(stringResource(R.string.action_save), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showJsonDialog) {
        NodeJsonDialog(
            initialPayload = NodeJsonSerializer.buildSubtreePayload(
                targetNode = node,
                allNodes = allNodes,
                overrideTitle = title,
                overrideNotes = notes,
                overrideColorHex = colorHex,
                overrideIconName = iconName,
                overrideLinkUrl = linkUrl,
                overrideImageUri = imageUri,
                overrideProgress = progressValue,
                overrideProgressEnabled = progressEnabled,
                overrideImpact = impactValue,
                overrideImpactEnabled = impactEnabled,
                overrideDueDate = dueDateValue,
                overrideDueDateEnabled = dueDateEnabled,
                overrideChecklist = checklistItems
            ),
            onDismiss = { showJsonDialog = false },
            onApply = { payload ->
                showJsonDialog = false
                title = payload.title
                notes = payload.notes
                colorHex = payload.colorHex
                iconName = payload.iconName
                linkUrl = payload.linkUrl
                imageUri = payload.imageUri
                if (payload.progress != null) {
                    progressEnabled = true
                    progressValue = payload.progress
                    isAutoProgress = false
                }
                if (payload.impact != null) {
                    impactEnabled = true
                    impactValue = payload.impact
                }
                if (payload.dueDate != null) {
                    dueDateEnabled = true
                    dueDateValue = payload.dueDate
                }
                if (payload.checklist.isNotEmpty()) {
                    checklistItems = payload.checklist
                }
                if (payload.children.isNotEmpty()) {
                    onImportJsonSubtree?.invoke(node.id, payload)
                }
            }
        )
    }

    if (showDeleteConfirm) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text(stringResource(R.string.delete_node_dialog_title), color = TextPrimary) },
                text = {
                    Text(
                        stringResource(R.string.delete_node_dialog_msg, title),
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
                        Text(stringResource(R.string.action_delete), color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text(stringResource(R.string.action_cancel), color = TextSecondary)
                    }
                }
            )
        }
    }
}

