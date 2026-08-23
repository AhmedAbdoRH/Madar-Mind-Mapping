package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.GeneratingTokens
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.MindMapEntity
import com.example.data.model.MindNodeEntity
import com.example.ui.canvas.OrbitalMindCanvas
import com.example.ui.dialogs.AddNodeDialog
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
import com.example.ui.util.OrbitColors
import com.example.ui.util.OrbitIcons
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
    val isAddNodeOpen by viewModel.isAddNodeOpen.collectAsStateWithLifecycle()
    val isOutlineOpen by viewModel.isOutlineOpen.collectAsStateWithLifecycle()

    val isSearchActive by viewModel.isSearchActive.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    // Pocket & Quick Actions
    val pocketNode by viewModel.pocketNode.collectAsStateWithLifecycle()
    val isPocketActionDialogOpen by viewModel.isPocketActionDialogOpen.collectAsStateWithLifecycle()
    val nodeForQuickActions by viewModel.nodeForQuickActions.collectAsStateWithLifecycle()

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

    // Hardware/System Back Button Navigation
    BackHandler {
        if (isSearchActive) {
            viewModel.setSearchActive(false)
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
            viewModel.navigateUpLevel()
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
                    onRotateBy = { delta -> viewModel.rotateOrbitBy(delta) },
                    onDiveIntoNode = { nodeId -> viewModel.diveIntoNode(nodeId) },
                    onCenterNodeClick = { node -> viewModel.openEditNodeDialog(node) },
                    onOrbitNodeLongClick = { node -> viewModel.openQuickActions(node) },
                    onAddChildClick = {
                        if (pocketNode != null) {
                            viewModel.openPocketActionDialog()
                        } else {
                            viewModel.openAddNodeDialog()
                        }
                    }
                )

                // Top Navigation & Breadcrumbs Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = SleekSurface.copy(alpha = 0.95f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorderSubtle),
                        shadowElevation = 4.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            // Back Button
                            IconButton(
                                onClick = { viewModel.navigateUpLevel() },
                                modifier = Modifier.size(36.dp).testTag("navigate_up_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.action_back),
                                    tint = TextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Breadcrumb Path (Scrollable)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f)
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 4.dp)
                            ) {
                                breadcrumbs.forEachIndexed { index, node ->
                                    val isCurrent = index == breadcrumbs.lastIndex
                                    Text(
                                        text = node.title,
                                        color = if (isCurrent) SleekPrimary else TextSecondary,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { viewModel.diveIntoNode(node.id) }
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    )

                                    if (index < breadcrumbs.lastIndex) {
                                        Text(
                                            text = "›",
                                            color = TextTertiary,
                                            fontSize = 14.sp,
                                            modifier = Modifier.padding(horizontal = 2.dp)
                                        )
                                    }
                                }
                            }

                            // Search Action
                            IconButton(
                                onClick = { viewModel.setSearchActive(!isSearchActive) },
                                modifier = Modifier.size(34.dp).testTag("toggle_search_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = stringResource(R.string.action_search),
                                    tint = if (isSearchActive) SleekPrimary else TextSecondary,
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            // Outline Action
                            IconButton(
                                onClick = { viewModel.setOutlineOpen(true) },
                                modifier = Modifier.size(34.dp).testTag("toggle_outline_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FormatListBulleted,
                                    contentDescription = stringResource(R.string.action_outline),
                                    tint = TextSecondary,
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            // Dark / Light Mode Quick Toggle
                            IconButton(
                                onClick = { viewModel.toggleDarkMode() },
                                modifier = Modifier.size(34.dp).testTag("map_theme_toggle_button")
                            ) {
                                Icon(
                                    imageVector = if (isDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                                    contentDescription = stringResource(R.string.toggle_dark_mode_cd),
                                    tint = if (isDark) SleekPrimary else TextSecondary,
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            // Edit Current Node Action
                            IconButton(
                                onClick = { viewModel.openEditNodeDialog(centralNode) },
                                modifier = Modifier.size(34.dp).testTag("edit_central_node_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = stringResource(R.string.edit_center_cd),
                                    tint = TextSecondary,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }

                    // Search Overlay dropdown if active
                    AnimatedVisibility(
                        visible = isSearchActive,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = SleekSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorderSubtle),
                            shadowElevation = 8.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { viewModel.setSearchQuery(it) },
                                    placeholder = { Text(stringResource(R.string.search_idea_placeholder)) },
                                    singleLine = true,
                                    trailingIcon = {
                                        if (searchQuery.isNotBlank()) {
                                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = stringResource(R.string.action_clear),
                                                    tint = TextSecondary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedBorderColor = SleekPrimary,
                                        unfocusedBorderColor = SleekBorder,
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
                                                    .clickable {
                                                        viewModel.diveIntoNode(matchNode.id)
                                                        viewModel.setSearchActive(false)
                                                    }
                                                    .padding(vertical = 6.dp, horizontal = 4.dp)
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

                // 🪐 Floating Orbital Pocket Ring Indicator (حلقة الجيب المداري العائمة)
                AnimatedVisibility(
                    visible = pocketNode != null,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 86.dp, end = 20.dp)
                ) {
                    if (pocketNode != null) {
                        FloatingOrbitalPocketBadge(
                            pocketNode = pocketNode!!,
                            onClick = { viewModel.openPocketActionDialog() },
                            onClear = { viewModel.clearPocket() }
                        )
                    }
                }

                // Bottom Controls: Add Node FAB on the LEFT (Start), Reset / Depth on the RIGHT
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(20.dp)
                ) {
                    // Floating Add Node Button on the LEFT
                    FloatingActionButton(
                        onClick = {
                            if (pocketNode != null) {
                                viewModel.openPocketActionDialog()
                            } else {
                                viewModel.openAddNodeDialog()
                            }
                        },
                        containerColor = if (pocketNode != null) Color(0xFF06B6D4) else SleekPrimary,
                        contentColor = if (isDark) Color(0xFF1F2D60) else Color.White,
                        shape = CircleShape,
                        modifier = Modifier
                            .shadow(6.dp, CircleShape)
                            .border(1.dp, SleekBorderSubtle, CircleShape)
                            .testTag("add_child_node_fab")
                    ) {
                        Icon(
                            imageVector = if (pocketNode != null) Icons.Default.GeneratingTokens else Icons.Default.Add,
                            contentDescription = if (pocketNode != null) stringResource(R.string.drop_held_node_cd) else stringResource(R.string.add_idea_node_cd),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Reset Rotation or Depth indicator on the RIGHT
                    if (kotlin.math.abs(orbitRotationAngle) > 2f) {
                        Surface(
                            shape = CircleShape,
                            color = SleekSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorderSubtle),
                            shadowElevation = 2.dp,
                            modifier = Modifier.clickable { viewModel.resetOrbitRotation() }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = stringResource(R.string.reset_angle_cd),
                                    tint = TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.reset_angle),
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SleekSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorderSubtle),
                            shadowElevation = 2.dp
                        ) {
                            Text(
                                text = stringResource(R.string.depth_indicator, breadcrumbs.size),
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialogs & Sheets
    if (isAddNodeOpen && centralNode != null) {
        AddNodeDialog(
            parentCentralNode = centralNode,
            onDismiss = { viewModel.closeAddNodeDialog() },
            onConfirm = { title, colorHex, iconName, notes ->
                viewModel.createChildNode(title, colorHex, iconName, notes)
            }
        )
    }

    if (selectedNodeForEdit != null) {
        NodeDetailDialog(
            node = selectedNodeForEdit!!,
            onDismiss = { viewModel.closeEditNodeDialog() },
            onSave = { id, title, colorHex, iconName, notes, checklist, linkUrl ->
                viewModel.saveNodeEdits(id, title, colorHex, iconName, notes, checklist, linkUrl)
            },
            onDelete = { id -> viewModel.deleteNode(id) },
            onPutInPocket = {
                viewModel.putNodeInPocket(selectedNodeForEdit!!)
            }
        )
    }

    if (nodeForQuickActions != null) {
        NodeQuickActionMenu(
            node = nodeForQuickActions!!,
            onPutInPocket = {
                viewModel.putNodeInPocket(nodeForQuickActions!!)
            },
            onEdit = {
                val node = nodeForQuickActions!!
                viewModel.closeQuickActions()
                viewModel.openEditNodeDialog(node)
            },
            onDelete = {
                val id = nodeForQuickActions!!.id
                viewModel.deleteNode(id)
            },
            onDismiss = { viewModel.closeQuickActions() }
        )
    }

    if (isPocketActionDialogOpen && pocketNode != null && centralNode != null) {
        PocketActionSheet(
            pocketNode = pocketNode!!,
            currentCentralNode = centralNode,
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
}

/**
 * Animated Floating Orbital Pocket badge displayed when a node is held for moving, cloning, or live syncing.
 */
@Composable
private fun FloatingOrbitalPocketBadge(
    pocketNode: MindNodeEntity,
    onClick: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
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

    val infiniteTransition = rememberInfiniteTransition(label = "pocket_halo")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = SleekSurface.copy(alpha = 0.96f),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF06B6D4)),
        shadowElevation = 8.dp,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("floating_orbital_pocket")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 10.dp, end = 6.dp, top = 8.dp, bottom = 8.dp)
        ) {
            // Glowing pulsing ring
            Box(
                modifier = Modifier
                    .size((36 * pulseScale).dp)
                    .clip(CircleShape)
                    .background(nodeColor)
                    .border(2.dp, Color(0xFF06B6D4), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Text(
                        text = pocketNode.title.take(1).uppercase().ifBlank { "•" },
                        color = textColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.synced_badge),
                        color = Color(0xFF06B6D4),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = pocketNode.title,
                    color = TextPrimary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 110.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = onClear,
                modifier = Modifier.size(26.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.action_clear),
                    tint = TextTertiary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
