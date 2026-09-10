package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shortcut
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.ThemeMode
import com.example.data.model.MindMapEntity
import com.example.ui.dialogs.BackupRestoreCenterDialog
import com.example.ui.dialogs.ExportMapDialog
import com.example.ui.theme.AppTheme
import com.example.ui.theme.SleekBackground
import com.example.ui.theme.SleekBorderSubtle
import com.example.ui.theme.SleekPrimary
import com.example.ui.theme.SleekSurface
import com.example.ui.theme.SleekSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.util.GoogleDriveSyncManager
import com.example.ui.util.OrbitColors
import com.example.ui.util.OrbitIcons
import com.example.ui.util.PinnedNodeShortcut
import com.example.ui.viewmodel.MindMapViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

enum class UniverseViewMode {
    GRID,
    LIST
}

@Composable
fun UniverseListScreen(
    maps: List<MindMapEntity>,
    viewModel: MindMapViewModel? = null,
    onSelectMap: (Long) -> Unit,
    onCreateNewMap: () -> Unit,
    onDuplicateMap: (Long) -> Unit,
    onDeleteMap: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("all") }
    var viewMode by remember { mutableStateOf(UniverseViewMode.GRID) }
    var showThemeMenu by remember { mutableStateOf(false) }

    val themeMode by viewModel?.themeMode?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(ThemeMode.SYSTEM) }
    val isDark = AppTheme.colors.isDark

    val mapNodeCounts by viewModel?.mapNodeCounts?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(emptyMap()) }
    val totalNodesCount by viewModel?.totalNodesCount?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(0) }
    val pinnedShortcuts by viewModel?.pinnedShortcuts?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(emptyList()) }
    val pinnedMapIds by viewModel?.pinnedMapIds?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(emptySet()) }

    val isExportDialogOpen by viewModel?.isExportDialogOpen?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(false) }
    val exportTargetMap by viewModel?.exportTargetMap?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(null) }
    val exportTargetNodes by viewModel?.exportTargetNodes?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(emptyList()) }
    val isImportDialogOpen by viewModel?.isImportDialogOpen?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(false) }
    val syncStatus by viewModel?.syncStatus?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(com.example.ui.util.SyncStatus.Idle) }

    // 🪐 Drag & Drop State for Merging Universes
    var draggedSourceMap by remember { mutableStateOf<MindMapEntity?>(null) }
    var dragTouchOffset by remember { mutableStateOf(Offset.Zero) }
    var hoveredTargetMapId by remember { mutableStateOf<Long?>(null) }
    val mapBoundsMap = remember { mutableStateMapOf<Long, Rect>() }

    // Dialog state for confirming map merge
    var mergeConfirmSourceAndTarget by remember { mutableStateOf<Pair<MindMapEntity, MindMapEntity>?>(null) }
    // Dialog state for manually picking a destination map to merge into
    var mapToMergePicker by remember { mutableStateOf<MindMapEntity?>(null) }
    var showCreateOrImportHub by remember { mutableStateOf(false) }
    var showSmartTextToMap by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val density = LocalDensity.current
    var googleAccount by remember { mutableStateOf(GoogleDriveSyncManager.getLastSignedInAccount(context)) }
    var showAccountMenu by remember { mutableStateOf(false) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val signedAccount = task.getResult(ApiException::class.java)
            googleAccount = signedAccount
        } catch (_: Exception) {
        }
    }

    // Dynamic greeting based on hour of day
    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greetingText = remember(currentHour) {
        when (currentHour) {
            in 5..11 -> R.string.greeting_morning
            in 12..16 -> R.string.greeting_afternoon
            in 17..21 -> R.string.greeting_evening
            else -> R.string.greeting_night
        }
    }

    val filteredMaps = remember(maps, searchQuery, selectedFilter, pinnedMapIds) {
        if (selectedFilter == "shortcuts") {
            emptyList()
        } else {
            var result = if (searchQuery.isBlank()) maps else {
                maps.filter {
                    it.title.contains(searchQuery, ignoreCase = true) ||
                            it.description.contains(searchQuery, ignoreCase = true)
                }
            }
            result.sortedWith(
                compareByDescending<MindMapEntity> { it.id in pinnedMapIds }
                    .thenByDescending { it.updatedAt }
            )
        }
    }

    val filteredShortcuts = remember(pinnedShortcuts, searchQuery, selectedFilter) {
        if (selectedFilter == "universes" || selectedFilter == "recent") {
            emptyList()
        } else {
            val list = if (searchQuery.isBlank()) pinnedShortcuts else {
                pinnedShortcuts.filter {
                    it.nodeTitle.contains(searchQuery, ignoreCase = true) ||
                            it.mapTitle.contains(searchQuery, ignoreCase = true)
                }
            }
            list.sortedWith(
                compareByDescending<PinnedNodeShortcut> { it.isPinned }
                    .thenByDescending { it.addedAt }
            )
        }
    }

    val isArabicLocale = remember {
        Locale.getDefault().language.startsWith("ar")
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = SleekBackground,
                floatingActionButtonPosition = FabPosition.End,
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        onClick = { showCreateOrImportHub = true },
                        containerColor = SleekPrimary,
                        contentColor = if (isDark) Color(0xFF1F2D60) else Color.White,
                        shape = RoundedCornerShape(18.dp),
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        text = {
                            Text(
                                text = stringResource(R.string.create_new_universe_label),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        },
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 12.dp)
                            .shadow(8.dp, RoundedCornerShape(18.dp))
                            .testTag("create_universe_fab")
                    )
                },
                modifier = modifier
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // =========================================================================
                    // TOP BAR (Brand Identity + Cloud Profile + Backup Center + Theme Switcher)
                    // =========================================================================
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        // App Logo Badge & Brand Name
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(SleekPrimary, Color(0xFF8B5CF6))
                                        )
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Explore,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Text(
                                    text = stringResource(R.string.app_name),
                                    color = TextPrimary,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.3).sp
                                )
                            }
                        }

                        // 👤 Google Account & Cloud Sync Profile Button
                        Box {
                            Surface(
                                shape = CircleShape,
                                color = if (googleAccount != null) SleekPrimary.copy(alpha = 0.15f) else SleekSurfaceVariant,
                                border = BorderStroke(
                                    1.dp,
                                    if (googleAccount != null) SleekPrimary.copy(alpha = 0.6f) else SleekBorderSubtle
                                ),
                                modifier = Modifier.size(38.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        if (googleAccount == null) {
                                            val client = GoogleDriveSyncManager.getGoogleSignInClient(context)
                                            googleSignInLauncher.launch(client.signInIntent)
                                        } else {
                                            showAccountMenu = true
                                        }
                                    },
                                    modifier = Modifier.testTag("google_account_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = stringResource(R.string.account_profile_cd),
                                        tint = if (googleAccount != null) SleekPrimary else TextSecondary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showAccountMenu,
                                onDismissRequest = { showAccountMenu = false },
                                containerColor = SleekSurface
                            ) {
                                if (googleAccount != null) {
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = googleAccount?.displayName ?: "Google User",
                                                    color = TextPrimary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                                Text(
                                                    text = googleAccount?.email ?: "",
                                                    color = TextSecondary,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        },
                                        onClick = {
                                            showAccountMenu = false
                                            viewModel?.openImportDialog()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.account_sign_out), color = Color(0xFFBA1A1A)) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Logout,
                                                contentDescription = null,
                                                tint = Color(0xFFBA1A1A)
                                            )
                                        },
                                        onClick = {
                                            showAccountMenu = false
                                            GoogleDriveSyncManager.getGoogleSignInClient(context).signOut()
                                            googleAccount = null
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // 📦 Backup & Restore Center Launcher
                        Surface(
                            shape = CircleShape,
                            color = SleekSurfaceVariant,
                            border = BorderStroke(1.dp, SleekBorderSubtle),
                            modifier = Modifier.size(38.dp)
                        ) {
                            IconButton(
                                onClick = { viewModel?.openImportDialog() },
                                modifier = Modifier.testTag("backup_center_top_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = "النسخ الاحتياطي والمزامنة",
                                    tint = SleekPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // 🌗 Theme Mode Switcher
                        Box {
                            Surface(
                                shape = CircleShape,
                                color = SleekSurfaceVariant,
                                border = BorderStroke(1.dp, SleekBorderSubtle),
                                modifier = Modifier.size(38.dp)
                            ) {
                                IconButton(
                                    onClick = { showThemeMenu = true },
                                    modifier = Modifier.testTag("theme_switcher_btn")
                                ) {
                                    val icon = when (themeMode) {
                                        ThemeMode.ZEN_BLACK -> Icons.Default.Nightlight
                                        ThemeMode.DARK -> Icons.Default.DarkMode
                                        ThemeMode.LIGHT -> Icons.Default.LightMode
                                        ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                                    }
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = stringResource(R.string.toggle_dark_mode_cd),
                                        tint = SleekPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showThemeMenu,
                                onDismissRequest = { showThemeMenu = false },
                                containerColor = SleekSurface
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = stringResource(R.string.theme_pitch_black),
                                                color = TextPrimary,
                                                fontWeight = if (themeMode == ThemeMode.ZEN_BLACK) FontWeight.Bold else FontWeight.Normal
                                            )
                                            if (themeMode == ThemeMode.ZEN_BLACK) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Icon(Icons.Default.Check, contentDescription = null, tint = SleekPrimary, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    },
                                    onClick = {
                                        viewModel?.setThemeMode(ThemeMode.ZEN_BLACK)
                                        showThemeMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = stringResource(R.string.theme_dark),
                                                color = TextPrimary,
                                                fontWeight = if (themeMode == ThemeMode.DARK) FontWeight.Bold else FontWeight.Normal
                                            )
                                            if (themeMode == ThemeMode.DARK) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Icon(Icons.Default.Check, contentDescription = null, tint = SleekPrimary, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    },
                                    onClick = {
                                        viewModel?.setThemeMode(ThemeMode.DARK)
                                        showThemeMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = stringResource(R.string.theme_light),
                                                color = TextPrimary,
                                                fontWeight = if (themeMode == ThemeMode.LIGHT) FontWeight.Bold else FontWeight.Normal
                                            )
                                            if (themeMode == ThemeMode.LIGHT) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Icon(Icons.Default.Check, contentDescription = null, tint = SleekPrimary, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    },
                                    onClick = {
                                        viewModel?.setThemeMode(ThemeMode.LIGHT)
                                        showThemeMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = stringResource(R.string.theme_system),
                                                color = TextPrimary,
                                                fontWeight = if (themeMode == ThemeMode.SYSTEM) FontWeight.Bold else FontWeight.Normal
                                            )
                                            if (themeMode == ThemeMode.SYSTEM) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Icon(Icons.Default.Check, contentDescription = null, tint = SleekPrimary, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    },
                                    onClick = {
                                        viewModel?.setThemeMode(ThemeMode.SYSTEM)
                                        showThemeMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // 🪐 Drag to Merge Hint Banner
                    AnimatedVisibility(
                        visible = draggedSourceMap != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF59E0B).copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Hub,
                                    contentDescription = null,
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "اسحب «${draggedSourceMap?.title}» وأفلته فوق عالم آخر لدمجه كفرع مداري 🪐",
                                    color = TextPrimary,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // =========================================================================
                    // MAIN CONTENT: SCROLLABLE DASHBOARD & GRIDS
                    // =========================================================================
                    if (viewMode == UniverseViewMode.GRID) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 90.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // 1. Hero & Stats Section
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Column {
                                    HeroWelcomeSection(
                                        greetingRes = greetingText,
                                        displayName = googleAccount?.displayName?.substringBefore(" ") ?: "",
                                        totalMaps = maps.size,
                                        totalNodes = totalNodesCount,
                                        isDriveSynced = googleAccount != null,
                                        syncStatus = syncStatus,
                                        onOpenDrive = { viewModel?.openImportDialog() }
                                    )

                                    Spacer(modifier = Modifier.height(18.dp))

                                    // 2. Starter Templates Carousel
                                    StarterTemplatesSection(
                                        onSelectTemplate = { templateKey ->
                                            viewModel?.createMapFromTemplate(templateKey, isArabicLocale)
                                        }
                                    )

                                    Spacer(modifier = Modifier.height(18.dp))

                                    // 3. Search & View Mode Switcher + Unified Filter Tabs
                                    SearchAndControlsSection(
                                        searchQuery = searchQuery,
                                        onSearchQueryChange = { searchQuery = it },
                                        selectedFilter = selectedFilter,
                                        onFilterSelected = { selectedFilter = it },
                                        viewMode = viewMode,
                                        onViewModeChange = { viewMode = it },
                                        totalShortcutsCount = pinnedShortcuts.size
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                            }

                            // 4. Empty State or Cosmic Circular Nodes Grid
                            val totalVisibleCount = filteredMaps.size + filteredShortcuts.size
                            if (totalVisibleCount == 0) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    EmptyUniversesState(
                                        isSearching = searchQuery.isNotBlank(),
                                        onCreateNewMap = onCreateNewMap
                                    )
                                }
                            } else {
                                // Render Pinned Shortcuts as Circular Planetary Nodes
                                if (filteredShortcuts.isNotEmpty()) {
                                    items(filteredShortcuts, key = { "shortcut_${it.mapId}_${it.nodeId}" }) { shortcut ->
                                        CosmicCircularShortcutNode(
                                            shortcut = shortcut,
                                            onClick = {
                                                viewModel?.openNodeFromShortcut(shortcut.mapId, shortcut.nodeId)
                                            },
                                            onTogglePin = {
                                                viewModel?.togglePinShortcut(shortcut.mapId, shortcut.nodeId)
                                            },
                                            onUnpin = {
                                                viewModel?.unpinNodeShortcut(shortcut.mapId, shortcut.nodeId)
                                            }
                                        )
                                    }
                                }

                                // Render Universes as Circular Planetary Nodes (with Drag & Drop Merging)
                                items(filteredMaps, key = { it.id }) { map ->
                                    val nodeCount = mapNodeCounts[map.id] ?: 1
                                    val isBeingHovered = hoveredTargetMapId == map.id && draggedSourceMap?.id != map.id
                                    val isBeingDragged = draggedSourceMap?.id == map.id
                                    val isPinned = map.id in pinnedMapIds

                                    CosmicCircularUniverseNode(
                                        map = map,
                                        nodeCount = nodeCount,
                                        isPinned = isPinned,
                                        isBeingDragged = isBeingDragged,
                                        isHoveredTarget = isBeingHovered,
                                        onPositioned = { bounds ->
                                            mapBoundsMap[map.id] = bounds
                                        },
                                        onDragStart = { offset ->
                                            draggedSourceMap = map
                                            dragTouchOffset = offset
                                        },
                                        onDrag = { dragDelta ->
                                            dragTouchOffset += dragDelta
                                            // Find if dragging over another map
                                            var foundTargetId: Long? = null
                                            for ((targetId, rect) in mapBoundsMap) {
                                                if (targetId != map.id && rect.contains(dragTouchOffset)) {
                                                    foundTargetId = targetId
                                                    break
                                                }
                                            }
                                            hoveredTargetMapId = foundTargetId
                                        },
                                        onDragEnd = {
                                            val targetId = hoveredTargetMapId
                                            if (targetId != null && targetId != map.id) {
                                                val targetMap = maps.firstOrNull { it.id == targetId }
                                                if (targetMap != null) {
                                                    mergeConfirmSourceAndTarget = Pair(map, targetMap)
                                                }
                                            }
                                            draggedSourceMap = null
                                            hoveredTargetMapId = null
                                        },
                                        onClick = { onSelectMap(map.id) },
                                        onTogglePin = { viewModel?.togglePinMap(map.id) },
                                        onMergeIntoAnother = {
                                            mapToMergePicker = map
                                        },
                                        onExport = { viewModel?.openExportDialogForMap(map.id) },
                                        onDuplicate = { onDuplicateMap(map.id) },
                                        onDelete = { onDeleteMap(map.id) }
                                    )
                                }
                            }
                        }
                    } else {
                        // LIST VIEW MODE
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 90.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item {
                                Column {
                                    HeroWelcomeSection(
                                        greetingRes = greetingText,
                                        displayName = googleAccount?.displayName?.substringBefore(" ") ?: "",
                                        totalMaps = maps.size,
                                        totalNodes = totalNodesCount,
                                        isDriveSynced = googleAccount != null,
                                        syncStatus = syncStatus,
                                        onOpenDrive = { viewModel?.openImportDialog() }
                                    )

                                    Spacer(modifier = Modifier.height(18.dp))

                                    StarterTemplatesSection(
                                        onSelectTemplate = { templateKey ->
                                            viewModel?.createMapFromTemplate(templateKey, isArabicLocale)
                                        }
                                    )

                                    Spacer(modifier = Modifier.height(18.dp))

                                    SearchAndControlsSection(
                                        searchQuery = searchQuery,
                                        onSearchQueryChange = { searchQuery = it },
                                        selectedFilter = selectedFilter,
                                        onFilterSelected = { selectedFilter = it },
                                        viewMode = viewMode,
                                        onViewModeChange = { viewMode = it },
                                        totalShortcutsCount = pinnedShortcuts.size
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                            }

                            val totalVisibleCount = filteredMaps.size + filteredShortcuts.size
                            if (totalVisibleCount == 0) {
                                item {
                                    EmptyUniversesState(
                                        isSearching = searchQuery.isNotBlank(),
                                        onCreateNewMap = onCreateNewMap
                                    )
                                }
                            } else {
                                if (filteredShortcuts.isNotEmpty()) {
                                    items(filteredShortcuts, key = { "shortcut_${it.mapId}_${it.nodeId}" }) { shortcut ->
                                        CosmicListShortcutCard(
                                            shortcut = shortcut,
                                            onClick = { viewModel?.openNodeFromShortcut(shortcut.mapId, shortcut.nodeId) },
                                            onTogglePin = { viewModel?.togglePinShortcut(shortcut.mapId, shortcut.nodeId) },
                                            onUnpin = { viewModel?.unpinNodeShortcut(shortcut.mapId, shortcut.nodeId) }
                                        )
                                    }
                                }

                                items(filteredMaps, key = { it.id }) { map ->
                                    val nodeCount = mapNodeCounts[map.id] ?: 1
                                    val isPinned = map.id in pinnedMapIds
                                    CosmicListUniverseCard(
                                        map = map,
                                        nodeCount = nodeCount,
                                        isPinned = isPinned,
                                        onClick = { onSelectMap(map.id) },
                                        onTogglePin = { viewModel?.togglePinMap(map.id) },
                                        onMergeIntoAnother = { mapToMergePicker = map },
                                        onExport = { viewModel?.openExportDialogForMap(map.id) },
                                        onDuplicate = { onDuplicateMap(map.id) },
                                        onDelete = { onDeleteMap(map.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 🪐 Floating Dragged Universe Visual Feedback (Moves with Finger)
            if (draggedSourceMap != null) {
                val dragColor = remember(draggedSourceMap!!.themeColorHex) {
                    OrbitColors.parseColor(draggedSourceMap!!.themeColorHex)
                }

                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (dragTouchOffset.x - 36.dp.toPx()).roundToInt(),
                                (dragTouchOffset.y - 36.dp.toPx()).roundToInt()
                            )
                        }
                        .size(72.dp)
                        .scale(1.1f)
                        .shadow(
                            elevation = 14.dp,
                            shape = CircleShape,
                            ambientColor = dragColor.copy(alpha = 0.4f),
                            spotColor = dragColor
                        )
                        .clip(CircleShape)
                        .background(dragColor),
                    contentAlignment = Alignment.Center
                ) {
                    val icon = OrbitIcons.getIcon(draggedSourceMap!!.rootNodeId) ?: Icons.Default.Explore
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = OrbitColors.getContrastingTextColor(dragColor),
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }

        // ==========================================
        // 🪐 MERGE CONFIRMATION DIALOG
        // ==========================================
        if (mergeConfirmSourceAndTarget != null) {
            val (source, target) = mergeConfirmSourceAndTarget!!
            MergeMapConfirmDialog(
                sourceMap = source,
                targetMap = target,
                onConfirm = {
                    viewModel?.mergeMapIntoMap(
                        sourceMapId = source.id,
                        targetMapId = target.id
                    )
                    mergeConfirmSourceAndTarget = null
                },
                onDismiss = {
                    mergeConfirmSourceAndTarget = null
                }
            )
        }

        // ==========================================
        // 🪐 MERGE PICKER DIALOG (From 3-dots Menu)
        // ==========================================
        if (mapToMergePicker != null) {
            MergeMapPickerDialog(
                sourceMap = mapToMergePicker!!,
                availableTargetMaps = maps.filter { it.id != mapToMergePicker!!.id },
                onSelectTarget = { targetMap ->
                    mergeConfirmSourceAndTarget = Pair(mapToMergePicker!!, targetMap)
                    mapToMergePicker = null
                },
                onDismiss = {
                    mapToMergePicker = null
                }
            )
        }

        // Export Dialog
        if (isExportDialogOpen && exportTargetMap != null) {
            ExportMapDialog(
                map = exportTargetMap!!,
                nodes = exportTargetNodes,
                onDismiss = { viewModel?.closeExportDialog() }
            )
        }

        // Create Or Import Hub Dialog
        if (showCreateOrImportHub) {
            com.example.ui.dialogs.CreateOrImportHubDialog(
                onDismiss = { showCreateOrImportHub = false },
                onCreateBlankMap = { onCreateNewMap() },
                onOpenSmartTextToMap = { showSmartTextToMap = true },
                onOpenBackupRestoreCenter = { viewModel?.openImportDialog() }
            )
        }

        // Smart Text to Map Dialog
        if (showSmartTextToMap && viewModel != null) {
            com.example.ui.dialogs.SmartTextToMapDialog(
                onDismiss = { showSmartTextToMap = false },
                onCreateFromText = { mapTitle, nodes ->
                    showSmartTextToMap = false
                    viewModel.createMapFromOutline(mapTitle, nodes)
                }
            )
        }

        // Import / Backup & Restore Center Dialog
        if (isImportDialogOpen && viewModel != null) {
            BackupRestoreCenterDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.closeImportDialog() },
                onMapImportedAndSelected = { importedMapId ->
                    onSelectMap(importedMapId)
                }
            )
        }
    }
}

// =========================================================================
// 🌟 HERO & REAL-TIME STATS HEADER
// =========================================================================
@Composable
private fun HeroWelcomeSection(
    greetingRes: Int,
    displayName: String,
    totalMaps: Int,
    totalNodes: Int,
    isDriveSynced: Boolean,
    syncStatus: com.example.ui.util.SyncStatus,
    onOpenDrive: () -> Unit
) {
    val primaryColor = SleekPrimary

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = SleekSurface),
        border = BorderStroke(1.dp, SleekBorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(100f, 60f),
                        radius = 450f
                    )
                )
                .padding(18.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (displayName.isNotBlank()) "${stringResource(greetingRes)}، $displayName ✨" else "${stringResource(greetingRes)} ✨",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.4).sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.app_tagline),
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Stats Chips Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Universes Stat Chip
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SleekSurfaceVariant,
                        border = BorderStroke(1.dp, SleekBorderSubtle),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp)
                        ) {
                            Text(text = "🪐", fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = stringResource(R.string.stats_universes_count, totalMaps),
                                color = TextPrimary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Thoughts Stat Chip
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SleekSurfaceVariant,
                        border = BorderStroke(1.dp, SleekBorderSubtle),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp)
                        ) {
                            Text(text = "💡", fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = stringResource(R.string.stats_nodes_count, totalNodes),
                                color = TextPrimary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Cloud Sync Chip with Dynamic State
                    val isSyncing = syncStatus is com.example.ui.util.SyncStatus.Syncing
                    val isError = syncStatus is com.example.ui.util.SyncStatus.Error

                    val chipColor = when {
                        isSyncing -> SleekPrimary.copy(alpha = 0.14f)
                        isError -> Color(0xFFEF4444).copy(alpha = 0.12f)
                        isDriveSynced -> Color(0xFF10B981).copy(alpha = 0.12f)
                        else -> SleekSurfaceVariant
                    }
                    val chipBorderColor = when {
                        isSyncing -> SleekPrimary.copy(alpha = 0.4f)
                        isError -> Color(0xFFEF4444).copy(alpha = 0.4f)
                        isDriveSynced -> Color(0xFF10B981).copy(alpha = 0.4f)
                        else -> SleekBorderSubtle
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = chipColor,
                        border = BorderStroke(1.dp, chipBorderColor),
                        modifier = Modifier
                            .weight(1.15f)
                            .clickable(onClick = onOpenDrive)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp)
                        ) {
                            if (isSyncing) {
                                val infiniteTransition = rememberInfiniteTransition(label = "heroSyncSpin")
                                val rotation by infiniteTransition.animateFloat(
                                    initialValue = 0f,
                                    targetValue = 360f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1000, easing = LinearEasing),
                                        repeatMode = RepeatMode.Restart
                                    ),
                                    label = "rotation"
                                )
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = SleekPrimary,
                                    modifier = Modifier
                                        .size(15.dp)
                                        .graphicsLayer(rotationZ = rotation)
                                )
                            } else {
                                val iconTint = when {
                                    isError -> Color(0xFFEF4444)
                                    isDriveSynced -> Color(0xFF10B981)
                                    else -> SleekPrimary
                                }
                                val iconVector = when {
                                    isError -> Icons.Default.Warning
                                    isDriveSynced -> Icons.Default.CloudDone
                                    else -> Icons.Default.CloudUpload
                                }
                                Icon(
                                    imageVector = iconVector,
                                    contentDescription = null,
                                    tint = iconTint,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(5.dp))
                            val chipText = when {
                                isSyncing -> stringResource(R.string.auto_sync_status_syncing)
                                isError -> "خطأ بالمزامنة"
                                isDriveSynced -> stringResource(R.string.stats_cloud_synced)
                                else -> stringResource(R.string.stats_cloud_not_synced)
                            }
                            val textTint = when {
                                isSyncing -> SleekPrimary
                                isError -> Color(0xFFEF4444)
                                isDriveSynced -> Color(0xFF10B981)
                                else -> TextSecondary
                            }
                            Text(
                                text = chipText,
                                color = textTint,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 🚀 STARTER TEMPLATES CAROUSEL
// =========================================================================
@Composable
private fun StarterTemplatesSection(
    onSelectTemplate: (String) -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = SleekPrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.section_starter_templates),
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            TemplatePillCard(
                title = stringResource(R.string.template_goals_title),
                subtitle = stringResource(R.string.template_goals_sub),
                icon = Icons.Default.Flag,
                accentColor = Color(0xFF6366F1),
                onClick = { onSelectTemplate("goals") }
            )

            TemplatePillCard(
                title = stringResource(R.string.template_launch_title),
                subtitle = stringResource(R.string.template_launch_sub),
                icon = Icons.Default.RocketLaunch,
                accentColor = Color(0xFF06B6D4),
                onClick = { onSelectTemplate("launch") }
            )

            TemplatePillCard(
                title = stringResource(R.string.template_brainstorm_title),
                subtitle = stringResource(R.string.template_brainstorm_sub),
                icon = Icons.Default.Psychology,
                accentColor = Color(0xFFEC4899),
                onClick = { onSelectTemplate("brainstorm") }
            )

            TemplatePillCard(
                title = stringResource(R.string.template_study_title),
                subtitle = stringResource(R.string.template_study_sub),
                icon = Icons.Default.MenuBook,
                accentColor = Color(0xFFF59E0B),
                onClick = { onSelectTemplate("study") }
            )
        }
    }
}

@Composable
private fun TemplatePillCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SleekSurface,
        border = BorderStroke(1.dp, SleekBorderSubtle),
        modifier = Modifier
            .width(170.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(10.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.16f))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    color = TextTertiary,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// =========================================================================
// 🔍 SEARCH, FILTER & VIEW MODE CONTROLS
// =========================================================================
@Composable
private fun SearchAndControlsSection(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    viewMode: UniverseViewMode,
    onViewModeChange: (UniverseViewMode) -> Unit,
    totalShortcutsCount: Int
) {
    Column {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text(stringResource(R.string.search_maps_placeholder), fontSize = 13.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(19.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = stringResource(R.string.clear_search_cd),
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = SleekPrimary,
                unfocusedBorderColor = SleekBorderSubtle,
                focusedContainerColor = SleekSurface,
                unfocusedContainerColor = SleekSurfaceVariant
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_universe_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filters and View Switcher Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            // Filter: All
            FilterChip(
                selected = selectedFilter == "all",
                onClick = { onFilterSelected("all") },
                label = { Text(stringResource(R.string.filter_all), fontSize = 11.5.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SleekPrimary.copy(alpha = 0.16f),
                    selectedLabelColor = SleekPrimary,
                    containerColor = SleekSurfaceVariant,
                    labelColor = TextSecondary
                ),
                shape = RoundedCornerShape(10.dp),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedFilter == "all",
                    borderColor = SleekBorderSubtle,
                    selectedBorderColor = SleekPrimary
                )
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Filter: Universes Only
            FilterChip(
                selected = selectedFilter == "universes",
                onClick = { onFilterSelected("universes") },
                label = { Text(stringResource(R.string.filter_universes), fontSize = 11.5.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SleekPrimary.copy(alpha = 0.16f),
                    selectedLabelColor = SleekPrimary,
                    containerColor = SleekSurfaceVariant,
                    labelColor = TextSecondary
                ),
                shape = RoundedCornerShape(10.dp),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedFilter == "universes",
                    borderColor = SleekBorderSubtle,
                    selectedBorderColor = SleekPrimary
                )
            )

            if (totalShortcutsCount > 0) {
                Spacer(modifier = Modifier.width(6.dp))

                // Filter: Shortcuts
                FilterChip(
                    selected = selectedFilter == "shortcuts",
                    onClick = { onFilterSelected("shortcuts") },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚡ ", fontSize = 10.sp)
                            Text(stringResource(R.string.filter_shortcuts), fontSize = 11.5.sp)
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFF59E0B).copy(alpha = 0.18f),
                        selectedLabelColor = Color(0xFFF59E0B),
                        containerColor = SleekSurfaceVariant,
                        labelColor = TextSecondary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedFilter == "shortcuts",
                        borderColor = SleekBorderSubtle,
                        selectedBorderColor = Color(0xFFF59E0B)
                    )
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Filter: Recent
            FilterChip(
                selected = selectedFilter == "recent",
                onClick = { onFilterSelected("recent") },
                label = { Text(stringResource(R.string.filter_recent), fontSize = 11.5.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SleekPrimary.copy(alpha = 0.16f),
                    selectedLabelColor = SleekPrimary,
                    containerColor = SleekSurfaceVariant,
                    labelColor = TextSecondary
                ),
                shape = RoundedCornerShape(10.dp),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedFilter == "recent",
                    borderColor = SleekBorderSubtle,
                    selectedBorderColor = SleekPrimary
                )
            )

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(8.dp))

            // View Mode Toggle Button Group
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = SleekSurfaceVariant,
                border = BorderStroke(1.dp, SleekBorderSubtle)
            ) {
                Row(modifier = Modifier.padding(2.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (viewMode == UniverseViewMode.GRID) SleekPrimary.copy(alpha = 0.2f) else Color.Transparent,
                        modifier = Modifier.clickable { onViewModeChange(UniverseViewMode.GRID) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = stringResource(R.string.view_mode_grid),
                            tint = if (viewMode == UniverseViewMode.GRID) SleekPrimary else TextTertiary,
                            modifier = Modifier
                                .padding(5.dp)
                                .size(17.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(2.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (viewMode == UniverseViewMode.LIST) SleekPrimary.copy(alpha = 0.2f) else Color.Transparent,
                        modifier = Modifier.clickable { onViewModeChange(UniverseViewMode.LIST) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ViewAgenda,
                            contentDescription = stringResource(R.string.view_mode_list),
                            tint = if (viewMode == UniverseViewMode.LIST) SleekPrimary else TextTertiary,
                            modifier = Modifier
                                .padding(5.dp)
                                .size(17.dp)
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// 🪐 COSMIC CIRCULAR UNIVERSE NODE (Interactive Celestial Spherical Node)
// =========================================================================
@Composable
private fun CosmicCircularUniverseNode(
    map: MindMapEntity,
    nodeCount: Int,
    isPinned: Boolean = false,
    isBeingDragged: Boolean,
    isHoveredTarget: Boolean,
    onPositioned: (Rect) -> Unit,
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onMergeIntoAnother: () -> Unit,
    onExport: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val themeColor = remember(map.themeColorHex) {
        OrbitColors.parseColor(map.themeColorHex)
    }

    val formattedDate = remember(map.updatedAt) {
        val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
        sdf.format(Date(map.updatedAt))
    }

    var showMenu by remember { mutableStateOf(false) }

    // Pulsing animation when another node is hovered over this node for merging (only active during hover)
    val pulseScale = if (isHoveredTarget) {
        val infiniteTransition = rememberInfiniteTransition(label = "hover_pulse")
        val animatedScale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_scale"
        )
        animatedScale
    } else {
        1f
    }

    val scaleState by animateFloatAsState(
        targetValue = if (isHoveredTarget) pulseScale else if (isBeingDragged) 0.92f else 1f,
        label = "node_scale"
    )

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SleekSurface),
        border = BorderStroke(
            if (isHoveredTarget) 2.5.dp else if (isPinned) 1.5.dp else 1.dp,
            if (isHoveredTarget) Color(0xFFF59E0B) else if (isPinned) SleekPrimary.copy(alpha = 0.6f) else themeColor.copy(alpha = 0.25f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isHoveredTarget) 8.dp else if (isPinned) 4.dp else 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .scale(scaleState)
            .onGloballyPositioned { coords ->
                onPositioned(coords.boundsInWindow())
            }
            .pointerInput(map.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { startOffset ->
                        onDragStart(startOffset)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount)
                    },
                    onDragEnd = {
                        onDragEnd()
                    },
                    onDragCancel = {
                        onDragEnd()
                    }
                )
            }
            .clickable(onClick = onClick)
            .testTag("universe_card_${map.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            if (isHoveredTarget) Color(0xFFF59E0B).copy(alpha = 0.15f) else themeColor.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
                .padding(14.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Top Action Bar: Quick Thoughts badge & 3-Dots Menu
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = themeColor.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = stringResource(R.string.node_count_badge, nodeCount),
                                color = themeColor,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                            )
                        }

                        if (isPinned) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SleekPrimary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "📌",
                                    fontSize = 10.5.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.5.dp)
                                )
                            }
                        }
                    }

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.action_options),
                                tint = TextTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            containerColor = SleekSurface
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (isPinned) stringResource(R.string.action_unpin) else stringResource(R.string.action_pin_to_top), color = TextPrimary) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.PushPin,
                                        contentDescription = null,
                                        tint = if (isPinned) SleekPrimary else TextSecondary
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onTogglePin()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_merge_map), color = TextPrimary) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Hub,
                                        contentDescription = null,
                                        tint = Color(0xFFF59E0B)
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onMergeIntoAnother()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_export), color = TextPrimary) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.FileDownload,
                                        contentDescription = null,
                                        tint = SleekPrimary
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onExport()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_duplicate), color = TextPrimary) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        tint = TextSecondary
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onDuplicate()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_delete), color = Color(0xFFBA1A1A)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = Color(0xFFBA1A1A)
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Circular Node Graphic (Normal natural node style)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(54.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = CircleShape,
                            ambientColor = themeColor.copy(alpha = 0.35f),
                            spotColor = themeColor
                        )
                        .clip(CircleShape)
                        .background(themeColor)
                ) {
                    val icon = OrbitIcons.getIcon(map.rootNodeId) ?: Icons.Default.Explore
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = OrbitColors.getContrastingTextColor(themeColor),
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Drop Target Banner
                if (isHoveredTarget) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF59E0B),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.drop_to_merge_badge),
                            color = Color.Black,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Universe Title
                Text(
                    text = map.title,
                    color = TextPrimary,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = formattedDate,
                    color = TextTertiary,
                    fontSize = 10.5.sp
                )
            }
        }
    }
}

// =========================================================================
// ⚡ COSMIC CIRCULAR SHORTCUT NODE (Seamless Circular Node Integration)
// =========================================================================
@Composable
private fun CosmicCircularShortcutNode(
    shortcut: PinnedNodeShortcut,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onUnpin: () -> Unit
) {
    val nodeColor = remember(shortcut.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(shortcut.colorHex))
        } catch (_: Exception) {
            Color(0xFF6366F1)
        }
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SleekSurface),
        border = BorderStroke(if (shortcut.isPinned) 1.5.dp else 1.dp, if (shortcut.isPinned) Color(0xFFF59E0B) else Color(0xFFF59E0B).copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (shortcut.isPinned) 4.dp else 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("pinned_node_shortcut_${shortcut.nodeId}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            if (shortcut.isPinned) Color(0xFFF59E0B).copy(alpha = 0.16f) else Color(0xFFF59E0B).copy(alpha = 0.09f),
                            Color.Transparent
                        )
                    )
                )
                .padding(14.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Top Row: Shortcut Indicator Badge & Pin / Close Buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF59E0B).copy(alpha = if (shortcut.isPinned) 0.28f else 0.18f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                        ) {
                            Text(text = if (shortcut.isPinned) "📌" else "⚡", fontSize = 9.sp)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (shortcut.isPinned) "مثبت" else "اختصار",
                                color = Color(0xFFF59E0B),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onTogglePin,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = if (shortcut.isPinned) "إلغاء التثبيت" else "تثبيت في البداية",
                                tint = if (shortcut.isPinned) Color(0xFFF59E0B) else TextTertiary,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(2.dp))

                        IconButton(
                            onClick = onUnpin,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إزالة الاختصار",
                                tint = TextTertiary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Circular Celestial Shortcut Planet
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(76.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                            radius = size.minDimension / 2f - 2f,
                            style = Stroke(width = 1.2f)
                        )
                        drawCircle(
                            color = nodeColor.copy(alpha = 0.12f),
                            radius = size.minDimension / 2f - 8f,
                            style = Stroke(width = 1f)
                        )
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        nodeColor.copy(alpha = 0.88f),
                                        nodeColor
                                    )
                                )
                            )
                            .shadow(6.dp, CircleShape)
                    ) {
                        val initials = shortcut.nodeTitle.trim().take(2).uppercase().ifBlank { "✦" }
                        Text(
                            text = initials,
                            color = OrbitColors.getContrastingTextColor(nodeColor),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Node Title
                Text(
                    text = shortcut.nodeTitle,
                    color = TextPrimary,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Parent Universe Label
                Text(
                    text = "🪐 ${shortcut.mapTitle}",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// =========================================================================
// 📋 COSMIC LIST CARDS (Detailed Row Layouts)
// =========================================================================
@Composable
private fun CosmicListUniverseCard(
    map: MindMapEntity,
    nodeCount: Int,
    isPinned: Boolean = false,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onMergeIntoAnother: () -> Unit,
    onExport: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val themeColor = remember(map.themeColorHex) {
        OrbitColors.parseColor(map.themeColorHex)
    }

    val formattedDate = remember(map.updatedAt) {
        val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
        sdf.format(Date(map.updatedAt))
    }

    var showMenu by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SleekSurface),
        border = BorderStroke(if (isPinned) 1.5.dp else 1.dp, if (isPinned) SleekPrimary.copy(alpha = 0.5f) else SleekBorderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPinned) 3.dp else 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("universe_card_${map.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            // Circular Node Graphic (Normal natural node style)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(46.dp)
                    .shadow(
                        elevation = 6.dp,
                        shape = CircleShape,
                        ambientColor = themeColor.copy(alpha = 0.35f),
                        spotColor = themeColor
                    )
                    .clip(CircleShape)
                    .background(themeColor)
            ) {
                val icon = OrbitIcons.getIcon(map.rootNodeId) ?: Icons.Default.Explore
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = OrbitColors.getContrastingTextColor(themeColor),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = map.title,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = themeColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "$nodeCount",
                            color = themeColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                        )
                    }
                    if (isPinned) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "📌", fontSize = 11.sp)
                    }
                }

                if (map.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = map.description,
                        color = TextSecondary,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = formattedDate,
                color = TextTertiary,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.width(4.dp))

            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.action_options),
                        tint = TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    containerColor = SleekSurface
                ) {
                    DropdownMenuItem(
                        text = { Text(if (isPinned) stringResource(R.string.action_unpin) else stringResource(R.string.action_pin_to_top), color = TextPrimary) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = null,
                                tint = if (isPinned) SleekPrimary else TextSecondary
                            )
                        },
                        onClick = {
                            showMenu = false
                            onTogglePin()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_merge_map), color = TextPrimary) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Hub,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B)
                            )
                        },
                        onClick = {
                            showMenu = false
                            onMergeIntoAnother()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_export), color = TextPrimary) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = null,
                                tint = SleekPrimary
                            )
                        },
                        onClick = {
                            showMenu = false
                            onExport()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_duplicate), color = TextPrimary) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        },
                        onClick = {
                            showMenu = false
                            onDuplicate()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_delete), color = Color(0xFFBA1A1A)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = Color(0xFFBA1A1A)
                            )
                        },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CosmicListShortcutCard(
    shortcut: PinnedNodeShortcut,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onUnpin: () -> Unit
) {
    val nodeColor = remember(shortcut.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(shortcut.colorHex))
        } catch (_: Exception) {
            Color(0xFF6366F1)
        }
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SleekSurface),
        border = BorderStroke(if (shortcut.isPinned) 1.5.dp else 1.dp, if (shortcut.isPinned) Color(0xFFF59E0B) else Color(0xFFF59E0B).copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (shortcut.isPinned) 3.dp else 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(nodeColor.copy(alpha = 0.2f))
            ) {
                val initials = shortcut.nodeTitle.trim().take(2).uppercase().ifBlank { "✦" }
                Text(
                    text = initials,
                    color = nodeColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = shortcut.nodeTitle,
                        color = TextPrimary,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFF59E0B).copy(alpha = if (shortcut.isPinned) 0.25f else 0.16f)
                    ) {
                        Text(
                            text = if (shortcut.isPinned) "📌 مثبت" else "⚡ اختصار",
                            color = Color(0xFFF59E0B),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "🪐 ${shortcut.mapTitle}",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onTogglePin,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = if (shortcut.isPinned) "إلغاء التثبيت" else "تثبيت في البداية",
                        tint = if (shortcut.isPinned) Color(0xFFF59E0B) else TextTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(2.dp))

                IconButton(
                    onClick = onUnpin,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "إزالة الاختصار",
                        tint = TextTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// =========================================================================
// 🪐 EMPTY STATE ILLUSTRATION & CTA
// =========================================================================
@Composable
private fun EmptyUniversesState(
    isSearching: Boolean,
    onCreateNewMap: () -> Unit
) {
    val primaryColor = SleekPrimary

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 20.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Cosmic empty orbital graphic
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(90.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.15f),
                        radius = size.minDimension / 2f,
                        style = Stroke(width = 1.5f)
                    )
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.08f),
                        radius = size.minDimension / 2f - 12f,
                        style = Stroke(width = 1.2f)
                    )
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(primaryColor.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = if (isSearching) Icons.Default.Search else Icons.Default.Explore,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = if (isSearching) stringResource(R.string.no_search_results_title) else stringResource(R.string.no_maps_title),
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isSearching) stringResource(R.string.no_search_results_subtitle) else stringResource(R.string.no_maps_subtitle),
                color = TextSecondary,
                fontSize = 12.5.sp,
                textAlign = TextAlign.Center,
                lineHeight = 17.sp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            if (!isSearching) {
                Spacer(modifier = Modifier.height(18.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SleekPrimary,
                    modifier = Modifier.clickable(onClick = onCreateNewMap)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.create_first_map_action),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// 🪐 MERGE MAP CONFIRMATION DIALOG
// =========================================================================
@Composable
private fun MergeMapConfirmDialog(
    sourceMap: MindMapEntity,
    targetMap: MindMapEntity,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val sourceColor = remember(sourceMap.themeColorHex) { OrbitColors.parseColor(sourceMap.themeColorHex) }
    val targetColor = remember(targetMap.themeColorHex) { OrbitColors.parseColor(targetMap.themeColorHex) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SleekSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Hub,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.merge_confirm_title),
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.merge_confirm_msg, sourceMap.title, targetMap.title),
                    color = TextSecondary,
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Visual Representation of Merge: Source Planet -> Target Planet
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Source Planet Chip
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = sourceColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, sourceColor.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(sourceColor)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Explore,
                                    contentDescription = null,
                                    tint = OrbitColors.getContrastingTextColor(sourceColor),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = sourceMap.title,
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = SleekPrimary,
                        modifier = Modifier.size(22.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Target Planet Chip
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = targetColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, targetColor.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(targetColor)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Explore,
                                    contentDescription = null,
                                    tint = OrbitColors.getContrastingTextColor(targetColor),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = targetMap.title,
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "تأكيد الدمج 🪐",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.action_cancel),
                    color = TextSecondary
                )
            }
        }
    )
}

// =========================================================================
// 🪐 MERGE MAP PICKER DIALOG (Select Target Universe to Merge Into)
// =========================================================================
@Composable
private fun MergeMapPickerDialog(
    sourceMap: MindMapEntity,
    availableTargetMaps: List<MindMapEntity>,
    onSelectTarget: (MindMapEntity) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SleekSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Hub,
                    contentDescription = null,
                    tint = SleekPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.merge_map_dialog_title),
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.merge_map_dialog_desc, sourceMap.title),
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (availableTargetMaps.isEmpty()) {
                    Text(
                        text = "لا توجد عوالم أخرى متاحة للدمج معها حالياً.",
                        color = TextTertiary,
                        fontSize = 12.5.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                    ) {
                        items(availableTargetMaps, key = { it.id }) { targetMap ->
                            val color = OrbitColors.parseColor(targetMap.themeColorHex)
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = SleekSurfaceVariant,
                                border = BorderStroke(1.dp, SleekBorderSubtle),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectTarget(targetMap) }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(10.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Explore,
                                            contentDescription = null,
                                            tint = OrbitColors.getContrastingTextColor(color),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = targetMap.title,
                                            color = TextPrimary,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (targetMap.description.isNotBlank()) {
                                            Text(
                                                text = targetMap.description,
                                                color = TextSecondary,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = null,
                                        tint = TextTertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.action_cancel),
                    color = TextSecondary
                )
            }
        }
    )
}
