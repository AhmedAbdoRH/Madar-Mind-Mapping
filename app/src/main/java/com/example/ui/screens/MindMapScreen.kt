package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.GeneratingTokens
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Shortcut
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.MindMapEntity
import com.example.data.model.MindNodeEntity
import com.example.ui.canvas.OrbitalMindCanvas
import com.example.ui.dialogs.AddNodeDialog
import com.example.ui.dialogs.ExportMapDialog
import com.example.ui.dialogs.ImageViewerDialog
import com.example.ui.dialogs.NodeDetailDialog
import com.example.ui.dialogs.NodeQuickActionMenu
import com.example.ui.dialogs.PocketActionSheet
import com.example.ui.sheets.MindMapOutlineSheet
import com.example.ui.theme.AppTheme
import com.example.ui.theme.SleekBackground
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
import com.example.ui.util.OrbitColors
import com.example.ui.util.OrbitIcons
import com.example.ui.util.ShortcutHelper
import com.example.ui.viewmodel.MindMapViewModel

@Composable
fun MindMapScreen(
    viewModel: MindMapViewModel,
    modifier: Modifier = Modifier
) {
    val activeMapId by viewModel.activeMapId.collectAsStateWithLifecycle()
    val allMaps by viewModel.allMaps.collectAsStateWithLifecycle()
    val allNodes by viewModel.allNodes.collectAsStateWithLifecycle()
    val currentCentralNodeId by viewModel.currentCentralNodeId.collectAsStateWithLifecycle()
    val orbitRotationAngle by viewModel.orbitRotationAngle.collectAsStateWithLifecycle()

    val selectedNodeForEdit by viewModel.selectedNodeForEdit.collectAsStateWithLifecycle()
    val selectedNodeForImageViewer by viewModel.selectedNodeForImageViewer.collectAsStateWithLifecycle()
    val isAddNodeOpen by viewModel.isAddNodeOpen.collectAsStateWithLifecycle()
    val isOutlineOpen by viewModel.isOutlineOpen.collectAsStateWithLifecycle()

    val isSearchActive by viewModel.isSearchActive.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val progressSortDescending by viewModel.progressSortDescending.collectAsStateWithLifecycle()
    val impactSortDescending by viewModel.impactSortDescending.collectAsStateWithLifecycle()

    // Export Dialog State
    val isExportDialogOpen by viewModel.isExportDialogOpen.collectAsStateWithLifecycle()
    val exportTargetMap by viewModel.exportTargetMap.collectAsStateWithLifecycle()
    val exportTargetNodes by viewModel.exportTargetNodes.collectAsStateWithLifecycle()

    // Pocket & Quick Actions
    val pocketNode by viewModel.pocketNode.collectAsStateWithLifecycle()
    val isPocketActionDialogOpen by viewModel.isPocketActionDialogOpen.collectAsStateWithLifecycle()
    val dropTargetAction by viewModel.dropTargetAction.collectAsStateWithLifecycle()
    val nodeForQuickActions by viewModel.nodeForQuickActions.collectAsStateWithLifecycle()
    val userFeedbackMessage by viewModel.userFeedbackMessage.collectAsStateWithLifecycle()

    val context = LocalContext.current
    LaunchedEffect(userFeedbackMessage) {
        userFeedbackMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearFeedbackMessage()
        }
    }

    val currentMap = remember(activeMapId, allMaps) {
        allMaps.firstOrNull { it.id == activeMapId }
    }

    val centralNode = remember(currentCentralNodeId, allNodes) {
        allNodes.firstOrNull { it.id == currentCentralNodeId }
            ?: allNodes.firstOrNull { it.parentId == null }
    }

    val orbitingChildren = remember(centralNode, allNodes) {
        if (centralNode == null) emptyList()
        else allNodes.filter { it.parentId == centralNode.id }.sortedBy { it.orderIndex }
    }

    val parentNode = remember(centralNode, allNodes) {
        if (centralNode?.parentId != null) {
            allNodes.firstOrNull { it.id == centralNode.parentId }
        } else null
    }

    // Breadcrumb path from Root down to centralNode
    val breadcrumbs = remember(centralNode, allNodes) {
        val path = mutableListOf<MindNodeEntity>()
        var curr: MindNodeEntity? = centralNode
        while (curr != null) {
            path.add(0, curr)
            curr = allNodes.firstOrNull { it.id == curr?.parentId }
        }
        path
    }

    // Search results
    val searchResults = remember(searchQuery, allNodes) {
        if (searchQuery.isBlank()) emptyList()
        else allNodes.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.notes.contains(searchQuery, ignoreCase = true)
        }
    }

    var zoomOutTrigger by remember { mutableIntStateOf(0) }
    var nodePendingDeletion by remember { mutableStateOf<MindNodeEntity?>(null) }
    var nodeForShortcutChoice by remember { mutableStateOf<MindNodeEntity?>(null) }
    var isSortMenuExpanded by remember { mutableStateOf(false) }

    fun handleNavigateBack() {
        if (centralNode?.parentId != null) {
            zoomOutTrigger++
        } else {
            viewModel.navigateUpLevel()
        }
    }

    // Hardware/System Back Button Navigation
    BackHandler {
        if (nodePendingDeletion != null) {
            nodePendingDeletion = null
        } else if (isSearchActive) {
            viewModel.setSearchActive(false)
        } else if (selectedNodeForImageViewer != null) {
            viewModel.closeImageViewer()
        } else if (isPocketActionDialogOpen) {
            viewModel.closePocketActionDialog()
        } else if (nodeForQuickActions != null) {
            viewModel.closeQuickActions()
        } else if (isOutlineOpen) {
            viewModel.setOutlineOpen(false)
        } else if (isAddNodeOpen) {
            viewModel.closeAddNodeDialog()
        } else if (selectedNodeForEdit != null) {
            viewModel.closeEditNodeDialog()
        } else {
            handleNavigateBack()
        }
    }

    val isDark = AppTheme.colors.isDark

    Scaffold(
        containerColor = SleekBackground,
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (centralNode == null || currentMap == null) {
                // Loading Screen
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    CircularProgressIndicator(color = SleekPrimary)
                }
            } else {
                // Main Radial Canvas
                OrbitalMindCanvas(
                    centralNode = centralNode,
                    orbitingNodes = orbitingChildren,
                    allNodesInMap = allNodes,
                    currentRotationAngle = orbitRotationAngle,
                    pocketNode = pocketNode,
                    zoomOutTrigger = zoomOutTrigger,
                    onRotateBy = { delta -> viewModel.rotateOrbitBy(delta) },
                    onDiveIntoNode = { nodeId -> viewModel.diveIntoNode(nodeId) },
                    onCenterNodeClick = { node -> viewModel.openEditNodeDialog(node) },
                    onOrbitNodeLongClick = { node -> viewModel.openQuickActions(node) },
                    onReorderNodes = { orderedIds -> viewModel.reorderSiblingNodes(orderedIds) },
                    onPutInPocket = { node -> viewModel.putNodeInPocket(node) },
                    onDropHeldNode = { _ -> viewModel.openPocketActionDialog() },
                    onDropNodeOntoTarget = { source, target ->
                        viewModel.openDropActionForNodes(source, target, isDirectDrop = true)
                    },
                    onClearPocket = { viewModel.clearPocket() },
                    onOpenPocketDialog = { viewModel.openPocketActionDialog() },
                    onEditNode = { node -> viewModel.openEditNodeDialog(node) },
                    onDeleteNode = { node -> nodePendingDeletion = node },
                    onImageNodeClick = { node -> viewModel.openImageViewer(node) },
                    onNavigateToOriginalNode = { originalId -> viewModel.navigateToOriginalNode(originalId) },
                    onAddChildClick = {
                        if (pocketNode != null) {
                            viewModel.openPocketActionDialog()
                        } else {
                            viewModel.openAddNodeDialog()
                        }
                    }
                )

                // Floating Back Button on Top Start (Left)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SleekSurface.copy(alpha = 0.92f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorderSubtle),
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 14.dp, top = 14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { handleNavigateBack() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        val backTitle = parentNode?.title
                        if (!backTitle.isNullOrBlank()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = backTitle,
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 140.dp)
                            )
                        }
                    }
                }

                // Vertical Floating Action Strip on Top End (Right): Search, Outline/Index, Reset Orbit, Sort By, Export (Icons only, no text)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = SleekSurface.copy(alpha = 0.94f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorderSubtle),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 14.dp, top = 14.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp)
                    ) {
                        // 1. Search Button
                        IconButton(
                            onClick = { viewModel.setSearchActive(!isSearchActive) },
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("toggle_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = stringResource(R.string.action_search),
                                tint = if (isSearchActive) SleekPrimary else TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // 2. Outline / Tree Index Button (الفهرس)
                        IconButton(
                            onClick = { viewModel.setOutlineOpen(true) },
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("toggle_outline_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatListBulleted,
                                contentDescription = stringResource(R.string.action_outline),
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // 3. Reset Orbit Rotation Button (إعادة الضبط)
                        IconButton(
                            onClick = { viewModel.resetOrbitRotation() },
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("reset_orbit_rotation_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "إعادة ضبط المدار",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // 4. Sort By Button (الترتيب حسب)
                        Box {
                            IconButton(
                                onClick = { isSortMenuExpanded = true },
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("sort_nodes_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sort,
                                    contentDescription = "ترتيب العقد",
                                    tint = if (isSortMenuExpanded) SleekPrimary else TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = isSortMenuExpanded,
                                onDismissRequest = { isSortMenuExpanded = false },
                                modifier = Modifier.background(SleekSurfaceElevated)
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("📊", fontSize = 14.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (progressSortDescending) "شدة التقييم (من الأعلى للأقل ⬇️)" else "شدة التقييم (من الأقل للأعلى ⬆️)",
                                                color = TextPrimary,
                                                fontSize = 12.sp
                                            )
                                        }
                                    },
                                    onClick = {
                                        isSortMenuExpanded = false
                                        viewModel.toggleSortSiblingsByProgress()
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("⚡", fontSize = 14.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (impactSortDescending) "قوة التأثير (من الأعلى للأقل ⬇️)" else "قوة التأثير (من الأقل للأعلى ⬆️)",
                                                color = TextPrimary,
                                                fontSize = 12.sp
                                            )
                                        }
                                    },
                                    onClick = {
                                        isSortMenuExpanded = false
                                        viewModel.toggleSortSiblingsByImpact()
                                    }
                                )
                            }
                        }

                        // 5. Export Button
                        IconButton(
                            onClick = { viewModel.openExportDialogForCurrentMap() },
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("export_current_map_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = stringResource(R.string.action_export),
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Search Overlay Floating Card (when search is open)
                AnimatedVisibility(
                    visible = isSearchActive,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 66.dp, start = 16.dp, end = 16.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = SleekSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorderSubtle),
                        shadowElevation = 8.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.setSearchQuery(it) },
                                placeholder = { Text(stringResource(R.string.search_idea_placeholder), fontSize = 13.sp) },
                                singleLine = true,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = SleekPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                trailingIcon = {
                                    IconButton(onClick = {
                                        if (searchQuery.isNotBlank()) {
                                            viewModel.setSearchQuery("")
                                        } else {
                                            viewModel.setSearchActive(false)
                                        }
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = stringResource(R.string.action_clear),
                                            tint = TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = SleekPrimary,
                                    unfocusedBorderColor = SleekBorderSubtle,
                                    focusedContainerColor = SleekSurface,
                                    unfocusedContainerColor = SleekSurfaceVariant
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (searchQuery.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.matches_found_count, searchResults.size),
                                    color = TextTertiary,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                LazyColumn(modifier = Modifier.height(180.dp)) {
                                    items(searchResults) { matchNode ->
                                        val matchColor = OrbitColors.parseColor(matchNode.colorHex)
                                        val textColor = OrbitColors.getContrastingTextColor(matchColor)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    viewModel.diveIntoNode(matchNode.id)
                                                    viewModel.setSearchActive(false)
                                                }
                                                .padding(vertical = 6.dp, horizontal = 6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(matchColor)
                                                    .border(1.dp, Color.White, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                val searchIcon = OrbitIcons.getIcon(matchNode.iconName)
                                                if (searchIcon != null) {
                                                    Icon(
                                                        imageVector = searchIcon,
                                                        contentDescription = null,
                                                        tint = textColor,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                } else {
                                                    Text(
                                                        text = matchNode.title.take(1).uppercase().ifBlank { "•" },
                                                        color = textColor,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = matchNode.title,
                                                color = TextPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs & Sheets
    if (selectedNodeForImageViewer != null) {
        val imageNode = selectedNodeForImageViewer!!
        ImageViewerDialog(
            node = imageNode,
            isCentralNode = imageNode.id == centralNode?.id,
            allNodes = allNodes,
            onDismiss = { viewModel.closeImageViewer() },
            onDiveIntoNode = { nodeId ->
                viewModel.closeImageViewer()
                viewModel.diveIntoNode(nodeId)
            },
            onEditNode = { node ->
                viewModel.closeImageViewer()
                viewModel.openEditNodeDialog(node)
            }
        )
    }

    if (isAddNodeOpen && centralNode != null) {
        AddNodeDialog(
            parentCentralNode = centralNode,
            onDismiss = { viewModel.closeAddNodeDialog() },
            onConfirm = { title, colorHex, iconName, notes, imageUri ->
                viewModel.createChildNode(
                    title = title,
                    colorHex = colorHex,
                    iconName = iconName,
                    notes = notes,
                    imageUri = imageUri,
                    targetParentId = centralNode.id
                )
            }
        )
    }

    if (selectedNodeForEdit != null) {
        val editNode = selectedNodeForEdit!!
        NodeDetailDialog(
            node = editNode,
            onDismiss = { viewModel.closeEditNodeDialog() },
            onSave = { id, title, colorHex, iconName, notes, checklist, linkUrl, imageUri, progress, impact, dueDate ->
                viewModel.saveNodeEdits(id, title, colorHex, iconName, notes, checklist, linkUrl, imageUri, progress, impact, dueDate)
            },
            onDelete = { id -> viewModel.deleteNode(id) },
            onPutInPocket = {
                viewModel.putNodeInPocket(editNode)
            },
            onApplyColorScope = { scope, hex ->
                viewModel.applyColorWithScope(editNode.id, hex, scope)
            },
            onAddShortcut = {
                nodeForShortcutChoice = editNode
            },
            hasChildren = allNodes.any { it.parentId == editNode.id },
            hasSiblings = allNodes.count { it.parentId == editNode.parentId } > 1,
            directChildren = allNodes.filter { it.parentId == editNode.id },
            allNodes = allNodes,
            onUpdateChildrenProgress = { progressMap ->
                viewModel.updateChildrenProgress(progressMap)
            },
            onImportJsonSubtree = { pId, payload ->
                viewModel.importNodeJsonSubtree(pId, payload)
            }
        )
    }

    if (nodeForShortcutChoice != null) {
        val targetNode = nodeForShortcutChoice!!
        com.example.ui.dialogs.AddShortcutChoiceDialog(
            itemTitle = targetNode.title,
            onDismiss = { nodeForShortcutChoice = null },
            onConfirm = { targetType ->
                nodeForShortcutChoice = null
                viewModel.pinNodeShortcutWithTarget(currentMap, targetNode, targetType)
            }
        )
    }

    if (nodeForQuickActions != null) {
        val quickNode = nodeForQuickActions!!
        val originalTargetId = remember(quickNode, allNodes) {
            if (quickNode.isSyncTwin) {
                val directMaster = allNodes.firstOrNull { it.id == quickNode.syncMasterId && it.id != quickNode.id }
                if (directMaster != null) directMaster.id
                else if (!quickNode.syncMasterId.isNullOrBlank() && quickNode.id != quickNode.syncMasterId) {
                    allNodes.filter { it.syncMasterId == quickNode.syncMasterId && it.id != quickNode.id }
                        .minByOrNull { it.createdAt }?.id
                } else null
            } else null
        }

        NodeQuickActionMenu(
            node = quickNode,
            onPutInPocket = {
                viewModel.putNodeInPocket(quickNode)
            },
            onEdit = {
                viewModel.closeQuickActions()
                viewModel.openEditNodeDialog(quickNode)
            },
            onViewImage = {
                viewModel.closeQuickActions()
                viewModel.openImageViewer(quickNode)
            },
            onNavigateToOriginal = if (originalTargetId != null) {
                {
                    viewModel.closeQuickActions()
                    viewModel.navigateToOriginalNode(originalTargetId)
                }
            } else null,
            onAddShortcut = {
                viewModel.closeQuickActions()
                nodeForShortcutChoice = quickNode
            },
            onDelete = {
                viewModel.closeQuickActions()
                nodePendingDeletion = quickNode
            },
            onDismiss = { viewModel.closeQuickActions() },
            onApplyColorScope = { scope, hex ->
                viewModel.applyColorWithScope(quickNode.id, hex, scope)
            }
        )
    }

    if (dropTargetAction != null) {
        val action = dropTargetAction!!
        PocketActionSheet(
            pocketNode = action.sourceNode,
            currentCentralNode = action.targetNode,
            allNodes = allNodes,
            isDirectDrop = action.isDirectDrop,
            onMove = { viewModel.executeDropMove(action.sourceNode.id, action.targetNode.id) },
            onClone = { viewModel.executeDropClone(action.sourceNode.id, action.targetNode.id) },
            onSyncTwin = { viewModel.executeDropSyncTwin(action.sourceNode.id, action.targetNode.id) },
            onClearPocket = { viewModel.closeDropActionForNodes() },
            onDismiss = { viewModel.closeDropActionForNodes() }
        )
    } else if (isPocketActionDialogOpen && pocketNode != null && centralNode != null) {
        PocketActionSheet(
            pocketNode = pocketNode!!,
            currentCentralNode = centralNode,
            allNodes = allNodes,
            isDirectDrop = false,
            onMove = { viewModel.applyPocketMoveToCurrentOrbit() },
            onClone = { viewModel.applyPocketCloneToCurrentOrbit() },
            onSyncTwin = { viewModel.applyPocketSyncTwinToCurrentOrbit() },
            onClearPocket = { viewModel.clearPocket() },
            onDismiss = { viewModel.closePocketActionDialog() }
        )
    }

    if (isOutlineOpen && currentMap != null) {
        MindMapOutlineSheet(
            map = currentMap,
            allNodes = allNodes,
            onSelectNode = { nodeId -> viewModel.diveIntoNode(nodeId) },
            onDismiss = { viewModel.setOutlineOpen(false) },
            exportMarkdown = viewModel.getExportMarkdown()
        )
    }

    if (isExportDialogOpen && exportTargetMap != null) {
        ExportMapDialog(
            map = exportTargetMap!!,
            nodes = exportTargetNodes,
            currentNode = centralNode,
            onDismiss = { viewModel.closeExportDialog() }
        )
    }

    if (nodePendingDeletion != null) {
        val targetNode = nodePendingDeletion!!
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            AlertDialog(
                onDismissRequest = { nodePendingDeletion = null },
                title = {
                    Text(
                        stringResource(R.string.delete_node_dialog_title),
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        stringResource(R.string.delete_node_dialog_msg, targetNode.title),
                        color = TextSecondary
                    )
                },
                containerColor = SleekSurface,
                confirmButton = {
                    Button(
                        onClick = {
                            val id = targetNode.id
                            nodePendingDeletion = null
                            viewModel.deleteNode(id)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBA1A1A)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            stringResource(R.string.action_delete),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { nodePendingDeletion = null }) {
                        Text(stringResource(R.string.action_cancel), color = TextSecondary)
                    }
                }
            )
        }
    }
}
