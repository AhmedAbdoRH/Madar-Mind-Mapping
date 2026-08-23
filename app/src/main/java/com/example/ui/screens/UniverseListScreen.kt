package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FabPosition
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.ThemeMode
import com.example.data.model.MindMapEntity
import com.example.ui.theme.AppTheme
import com.example.ui.theme.SleekBackground
import com.example.ui.theme.SleekBorder
import com.example.ui.theme.SleekBorderSubtle
import com.example.ui.theme.SleekPrimary
import com.example.ui.theme.SleekSurface
import com.example.ui.theme.SleekSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.util.OrbitColors
import com.example.ui.viewmodel.MindMapViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    var showThemeMenu by remember { mutableStateOf(false) }
    val themeMode by viewModel?.themeMode?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(ThemeMode.SYSTEM) }
    val isDark = AppTheme.colors.isDark

    val filteredMaps = remember(maps, searchQuery) {
        if (searchQuery.isBlank()) maps
        else maps.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.description.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        containerColor = SleekBackground,
        floatingActionButtonPosition = FabPosition.Start,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateNewMap,
                containerColor = SleekPrimary,
                contentColor = if (isDark) Color(0xFF1F2D60) else Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .padding(start = 8.dp, bottom = 12.dp)
                    .shadow(6.dp, CircleShape)
                    .border(1.dp, SleekBorderSubtle, CircleShape)
                    .testTag("create_universe_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.create_mind_map_cd),
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Simple, Elegant Top Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.universe_list_title),
                    color = TextPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    modifier = Modifier.weight(1f)
                )

                // Dark Mode Toggle Button & Menu
                Box {
                    Surface(
                        shape = CircleShape,
                        color = SleekSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorderSubtle),
                        modifier = Modifier.size(38.dp)
                    ) {
                        IconButton(
                            onClick = { showThemeMenu = true },
                            modifier = Modifier.testTag("theme_mode_toggle_button")
                        ) {
                            Icon(
                                imageVector = when (themeMode) {
                                    ThemeMode.DARK -> Icons.Default.DarkMode
                                    ThemeMode.LIGHT -> Icons.Default.LightMode
                                    ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                                },
                                contentDescription = stringResource(R.string.toggle_dark_mode_cd),
                                tint = if (isDark) SleekPrimary else TextPrimary,
                                modifier = Modifier.size(19.dp)
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

            Spacer(modifier = Modifier.height(14.dp))

            // Search Bar (Shown if there are maps or searching)
            if (maps.isNotEmpty() || searchQuery.isNotEmpty()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.search_maps_placeholder)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_universe_input")
                )

                Spacer(modifier = Modifier.height(14.dp))
            }

            // Mind Maps List or Empty State
            if (filteredMaps.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (searchQuery.isBlank()) stringResource(R.string.no_maps_title) else stringResource(R.string.no_search_results_title),
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (searchQuery.isBlank()) stringResource(R.string.no_maps_subtitle) else stringResource(R.string.no_search_results_subtitle),
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredMaps, key = { it.id }) { map ->
                        SimpleUniverseCard(
                            map = map,
                            onClick = { onSelectMap(map.id) },
                            onDuplicate = { onDuplicateMap(map.id) },
                            onDelete = { onDeleteMap(map.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SimpleUniverseCard(
    map: MindMapEntity,
    onClick: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val themeColor = remember(map.themeColorHex) {
        OrbitColors.parseColor(map.themeColorHex)
    }

    val textColor = remember(themeColor) {
        OrbitColors.getContrastingTextColor(themeColor)
    }

    val formattedDate = remember(map.updatedAt) {
        val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
        sdf.format(Date(map.updatedAt))
    }

    var showMenu by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SleekSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
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
            // Planet Icon Circle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(themeColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Explore,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = map.title,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (map.description.isNotBlank()) {
                    Text(
                        text = map.description,
                        color = TextSecondary,
                        fontSize = 12.sp,
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
