package com.example.ui.dialogs

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.R
import com.example.data.ColorPreferences
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
import com.example.ui.util.ImageStorageHelper
import com.example.ui.util.OrbitColors
import com.example.ui.util.OrbitIcons
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddNodeDialog(
    parentCentralNode: MindNodeEntity,
    initialIsImageMode: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (title: String, colorHex: String, iconName: String, notes: String, imageUri: String?) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val colorPrefs = remember { ColorPreferences(context) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    // 0 = Idea Node (فكرة), 1 = Image Node (صورة)
    var selectedTab by remember { mutableIntStateOf(if (initialIsImageMode) 1 else 0) }

    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<String?>(null) }
    var selectedColorHex by remember {
        val savedDefault = colorPrefs.getDefaultNodeColor(parentCentralNode.mapId)
        mutableStateOf(savedDefault)
    }
    var selectedIconName by remember { mutableStateOf("") }

    // Auto-focus title and show software keyboard on dialog launch
    LaunchedEffect(selectedTab) {
        delay(120)
        try {
            focusRequester.requestFocus()
            keyboardController?.show()
        } catch (_: Exception) {}
    }

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

    val activeColor = OrbitColors.parseColor(selectedColorHex)
    val textColor = OrbitColors.getContrastingTextColor(activeColor)
    val activeIconVector = OrbitIcons.getIcon(selectedIconName)

    Dialog(onDismissRequest = onDismiss) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = SleekSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorderSubtle),
                shadowElevation = 10.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 680.dp)
                    .padding(vertical = 12.dp)
                    .testTag("add_node_dialog")
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Header (Pinned)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
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
                            } else if (selectedTab == 1) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else if (activeIconVector != null) {
                                Icon(
                                    imageVector = activeIconVector,
                                    contentDescription = null,
                                    tint = textColor,
                                    modifier = Modifier.size(24.dp)
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
                                text = if (selectedTab == 1) stringResource(R.string.node_type_image) else stringResource(R.string.add_node_dialog_title),
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

                    HorizontalDivider(color = SleekBorderSubtle)

                    // Scrollable Middle Content
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        // Node Type Switcher (فكرة / صورة) - Simplified with pure clean tabs (no redundant icons)
                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = SleekSurfaceVariant,
                            contentColor = SleekPrimary,
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                    color = SleekPrimary,
                                    height = 3.dp
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = {
                                    Text(
                                        text = stringResource(R.string.node_type_idea_ar),
                                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 14.sp,
                                        color = if (selectedTab == 0) SleekPrimary else TextSecondary
                                    )
                                }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = {
                                    selectedTab = 1
                                    if (imageUri == null) {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                },
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = stringResource(R.string.node_type_image_ar),
                                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 14.sp,
                                            color = if (selectedTab == 1) SleekPrimary else TextSecondary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B))
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
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (selectedTab == 1) {
                            // IMAGE NODE MODE: Prominent Photo Picker
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = SleekSurfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, if (imageUri != null) SleekPrimary else SleekBorderSubtle),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    if (!imageUri.isNullOrBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .size(110.dp)
                                                .clip(CircleShape)
                                                .border(3.dp, activeColor, CircleShape)
                                                .clickable {
                                                    photoPickerLauncher.launch(
                                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                    )
                                                }
                                        ) {
                                            AsyncImage(
                                                model = imageUri,
                                                contentDescription = "Selected photo",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(stringResource(R.string.change_photo), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            IconButton(
                                                onClick = { imageUri = null },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = stringResource(R.string.remove_photo),
                                                    tint = Color(0xFFBA1A1A),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    } else {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(CircleShape)
                                                .background(SleekPrimary.copy(alpha = 0.15f))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AddPhotoAlternate,
                                                contentDescription = null,
                                                tint = SleekPrimary,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "اضغط لاختيار صورة العقدة",
                                            color = TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "ستظهر الصورة ككوكب فلكي في المدار",
                                            color = TextSecondary,
                                            fontSize = 11.5.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Title Input for Image Node (Optional)
                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                label = { Text("عنوان / وصف الصورة (اختياري)") },
                                placeholder = { Text("مثال: تصميم الشعار، لقطة شاشة...") },
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
                                    .testTag("add_image_node_title_input")
                            )
                        } else {
                            // IDEA NODE MODE: Title Input
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
                                    .focusRequester(focusRequester)
                                    .testTag("add_node_title_input")
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Icon Selection (for Idea node)
                            IconSelectorRow(
                                selectedIconName = selectedIconName,
                                onIconSelected = { selectedIconName = if (it == "none") "" else it },
                                activeColor = activeColor,
                                allowNone = true
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Simplified Color Palette Swatches with Default Pinning Option
                        ColorSelectorRow(
                            selectedColorHex = selectedColorHex,
                            onColorSelected = { selectedColorHex = it },
                            mapId = parentCentralNode.mapId,
                            showSetAsDefaultOption = true
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Optional Quick Note
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text(stringResource(R.string.quick_note_label)) },
                            placeholder = { Text(stringResource(R.string.quick_note_placeholder)) },
                            minLines = 2,
                            maxLines = 4,
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

                    HorizontalDivider(color = SleekBorderSubtle)

                    // Fixed Actions (Always visible above keyboard)
                    Row(
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.action_cancel), color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))

                        val canCreate = if (selectedTab == 1) {
                            !imageUri.isNullOrBlank() || title.isNotBlank()
                        } else {
                            title.isNotBlank()
                        }

                        val effectiveTitle = if (title.isNotBlank()) {
                            title.trim()
                        } else if (selectedTab == 1 && !imageUri.isNullOrBlank()) {
                            "صورة"
                        } else {
                            ""
                        }

                        Button(
                            onClick = {
                                if (canCreate) {
                                    onConfirm(
                                        effectiveTitle,
                                        selectedColorHex,
                                        if (selectedTab == 1) "" else selectedIconName,
                                        notes.trim(),
                                        if (selectedTab == 1) imageUri else imageUri
                                    )
                                }
                            },
                            enabled = canCreate,
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
}

