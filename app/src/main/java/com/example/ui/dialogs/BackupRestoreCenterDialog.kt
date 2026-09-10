package com.example.ui.dialogs

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
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
import com.example.ui.util.DriveBackupFile
import com.example.ui.util.GoogleDriveSyncManager
import com.example.ui.util.OrbitColors
import com.example.ui.util.OrbitMindBackupManager
import com.example.ui.viewmodel.MindMapViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

@Composable
fun BackupRestoreCenterDialog(
    viewModel: MindMapViewModel,
    onDismiss: () -> Unit,
    onMapImportedAndSelected: (Long) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isDark = AppTheme.colors.isDark

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf(
        stringResource(R.string.tab_google_drive),
        stringResource(R.string.tab_import_file),
        stringResource(R.string.tab_paste_json),
        stringResource(R.string.tab_full_backup)
    )

    // General state
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    // Google Drive state
    var googleAccount by remember { mutableStateOf(GoogleDriveSyncManager.getLastSignedInAccount(context)) }
    var driveBackups by remember { mutableStateOf<List<DriveBackupFile>>(emptyList()) }
    var isLoadingDriveFiles by remember { mutableStateOf(false) }
    var isUploadingToDrive by remember { mutableStateOf(false) }
    var isDownloadingFromDrive by remember { mutableStateOf(false) }
    var downloadedDrivePreview by remember { mutableStateOf<OrbitMindBackupManager.BackupPreview?>(null) }
    var downloadedDriveJson by remember { mutableStateOf<String?>(null) }

    // State for File Import
    var loadedJsonForImport by remember { mutableStateOf<String?>(null) }
    var loadedPreview by remember { mutableStateOf<OrbitMindBackupManager.BackupPreview?>(null) }

    // State for Pasted JSON
    var pastedJsonText by remember { mutableStateOf("") }

    // State for Full Backup Export
    var isExportingFullBackup by remember { mutableStateOf(false) }
    var pendingFullBackupContent by remember { mutableStateOf<String?>(null) }

    var triggerReloadFiles by remember { mutableStateOf(0) }

    val authRecoveryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            triggerReloadFiles++
        }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val signedAccount = task.getResult(ApiException::class.java)
            googleAccount = signedAccount
            errorMessage = null
            successMessage = "Google Drive connected!"
        } catch (e: Exception) {
            errorMessage = "Google Drive sign-in: ${e.localizedMessage ?: "Canceled or failed"}"
        }
    }

    fun loadDriveFiles() {
        val account = googleAccount ?: return
        isLoadingDriveFiles = true
        errorMessage = null
        coroutineScope.launch {
            val result = GoogleDriveSyncManager.listBackupFiles(context, account)
            isLoadingDriveFiles = false
            result.fold(
                onSuccess = { list ->
                    driveBackups = list
                },
                onFailure = { err ->
                    if (err is com.google.android.gms.auth.UserRecoverableAuthException) {
                        val intent = err.intent
                        if (intent != null) {
                            authRecoveryLauncher.launch(intent)
                        } else {
                            errorMessage = "Google Drive authorization required"
                        }
                    } else {
                        errorMessage = "Failed to load Google Drive files: ${err.localizedMessage}"
                    }
                }
            )
        }
    }

    LaunchedEffect(googleAccount, triggerReloadFiles) {
        if (googleAccount != null) {
            loadDriveFiles()
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            errorMessage = null
            val content = OrbitMindBackupManager.readContentFromDocumentUri(context, uri)
            if (content != null) {
                val previewResult = OrbitMindBackupManager.parseBackupPreview(content)
                previewResult.fold(
                    onSuccess = { preview ->
                        loadedJsonForImport = content
                        loadedPreview = preview
                        errorMessage = null
                    },
                    onFailure = { err ->
                        loadedJsonForImport = null
                        loadedPreview = null
                        errorMessage = err.localizedMessage ?: "Invalid map format"
                    }
                )
            } else {
                errorMessage = "Failed to read file from storage"
            }
        }
    }

    val createFullBackupDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val content = pendingFullBackupContent
        if (uri != null && content != null) {
            val success = OrbitMindBackupManager.writeContentToDocumentUri(context, uri, content)
            if (success) {
                Toast.makeText(context, "Full Backup saved successfully!", Toast.LENGTH_SHORT).show()
                onDismiss()
            } else {
                Toast.makeText(context, "Error saving backup file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Dialog(
        onDismissRequest = { if (!isProcessing && !isUploadingToDrive && !isDownloadingFromDrive) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SleekSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorderSubtle),
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 720.dp)
                .padding(vertical = 16.dp)
                .testTag("backup_restore_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Top Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SleekPrimary.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = SleekPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.backup_center_title),
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.backup_center_subtitle),
                            color = TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        enabled = !isProcessing && !isUploadingToDrive && !isDownloadingFromDrive,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.action_close),
                            tint = TextTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Navigation Tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = SleekSurfaceVariant,
                    edgePadding = 6.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            height = 3.dp,
                            color = SleekPrimary
                        )
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .fillMaxWidth()
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = {
                                selectedTabIndex = index
                                errorMessage = null
                                successMessage = null
                            },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTabIndex == index) SleekPrimary else TextSecondary,
                                    maxLines = 1
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tab Content Scrollable Container
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (errorMessage != null) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFBA1A1A).copy(alpha = 0.12f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBA1A1A).copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = Color(0xFFBA1A1A),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = errorMessage ?: "",
                                    color = Color(0xFFBA1A1A),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    if (successMessage != null) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SleekPrimary.copy(alpha = 0.12f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SleekPrimary.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = SleekPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = successMessage ?: "",
                                    color = SleekPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    when (selectedTabIndex) {
                        0 -> {
                            // TAB 0: GOOGLE DRIVE CLOUD SYNC & RESTORE
                            Column(modifier = Modifier.fillMaxWidth()) {
                                val currentAccount = googleAccount
                                if (currentAccount == null) {
                                    // Not Connected State
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = SleekSurfaceVariant),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorderSubtle),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(20.dp)
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(54.dp)
                                                    .clip(CircleShape)
                                                    .background(SleekPrimary.copy(alpha = 0.15f))
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Cloud,
                                                    contentDescription = null,
                                                    tint = SleekPrimary,
                                                    modifier = Modifier.size(32.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))

                                            Text(
                                                text = stringResource(R.string.gdrive_title),
                                                color = TextPrimary,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold
                                            )

                                            Spacer(modifier = Modifier.height(6.dp))

                                            Text(
                                                text = stringResource(R.string.gdrive_desc),
                                                color = TextSecondary,
                                                fontSize = 12.sp,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )

                                            Spacer(modifier = Modifier.height(16.dp))

                                            Button(
                                                onClick = {
                                                    val client = GoogleDriveSyncManager.getGoogleSignInClient(context)
                                                    googleSignInLauncher.launch(client.signInIntent)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(46.dp)
                                                    .testTag("link_google_drive_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Sync,
                                                    contentDescription = null,
                                                    tint = if (isDark) Color(0xFF1F2D60) else Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = stringResource(R.string.gdrive_connect_btn),
                                                    color = if (isDark) Color(0xFF1F2D60) else Color.White,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    // Connected Account Header
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = SleekSurfaceVariant),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorderSubtle),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(SleekPrimary.copy(alpha = 0.15f))
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.CloudDone,
                                                        contentDescription = null,
                                                        tint = SleekPrimary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(10.dp))

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "Google Drive Linked",
                                                        color = TextPrimary,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = currentAccount.email ?: "Google Account",
                                                        color = TextSecondary,
                                                        fontSize = 11.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }

                                                OutlinedButton(
                                                    onClick = {
                                                        val client = GoogleDriveSyncManager.getGoogleSignInClient(context)
                                                        client.signOut().addOnCompleteListener {
                                                            googleAccount = null
                                                            driveBackups = emptyList()
                                                            successMessage = "Google Drive disconnected"
                                                        }
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(30.dp)
                                                ) {
                                                    Text("Disconnect", fontSize = 10.sp, color = TextSecondary)
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))

                                            // ==========================================
                                            // 🚀 SMART DEBOUNCED AUTO-SYNC CARD
                                            // ==========================================
                                            val isAutoSyncActive by viewModel.isAutoSyncEnabled.collectAsStateWithLifecycle()
                                            val currentSyncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
                                            val lastSyncTime by viewModel.lastSyncTimestamp.collectAsStateWithLifecycle()

                                            Card(
                                                shape = RoundedCornerShape(12.dp),
                                                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorderSubtle),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Box(
                                                            contentAlignment = Alignment.Center,
                                                            modifier = Modifier
                                                                .size(32.dp)
                                                                .clip(CircleShape)
                                                                .background(
                                                                    when (currentSyncStatus) {
                                                                        is com.example.ui.util.SyncStatus.Syncing -> SleekPrimary.copy(alpha = 0.15f)
                                                                        is com.example.ui.util.SyncStatus.Success -> Color(0xFF10B981).copy(alpha = 0.15f)
                                                                        is com.example.ui.util.SyncStatus.Error -> Color(0xFFEF4444).copy(alpha = 0.15f)
                                                                        else -> SleekPrimary.copy(alpha = 0.1f)
                                                                    }
                                                                )
                                                        ) {
                                                            when (currentSyncStatus) {
                                                                is com.example.ui.util.SyncStatus.Syncing -> {
                                                                    val infiniteTransition = rememberInfiniteTransition(label = "syncSpin")
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
                                                                            .size(18.dp)
                                                                            .graphicsLayer(rotationZ = rotation)
                                                                    )
                                                                }
                                                                is com.example.ui.util.SyncStatus.Success -> {
                                                                    Icon(
                                                                        imageVector = Icons.Default.CheckCircle,
                                                                        contentDescription = null,
                                                                        tint = Color(0xFF10B981),
                                                                        modifier = Modifier.size(18.dp)
                                                                    )
                                                                }
                                                                is com.example.ui.util.SyncStatus.Error -> {
                                                                    Icon(
                                                                        imageVector = Icons.Default.Warning,
                                                                        contentDescription = null,
                                                                        tint = Color(0xFFEF4444),
                                                                        modifier = Modifier.size(18.dp)
                                                                    )
                                                                }
                                                                else -> {
                                                                    Icon(
                                                                        imageVector = Icons.Default.CloudDone,
                                                                        contentDescription = null,
                                                                        tint = SleekPrimary,
                                                                        modifier = Modifier.size(18.dp)
                                                                    )
                                                                }
                                                            }
                                                        }

                                                        Spacer(modifier = Modifier.width(10.dp))

                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = stringResource(R.string.auto_sync_title),
                                                                color = TextPrimary,
                                                                fontSize = 12.5.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                            val statusSubtitle = when (currentSyncStatus) {
                                                                is com.example.ui.util.SyncStatus.Syncing -> stringResource(R.string.auto_sync_status_syncing)
                                                                is com.example.ui.util.SyncStatus.Success -> {
                                                                    val formattedTime = remember(lastSyncTime) {
                                                                        java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(lastSyncTime))
                                                                    }
                                                                    stringResource(R.string.auto_sync_status_success, formattedTime)
                                                                }
                                                                is com.example.ui.util.SyncStatus.Error -> (currentSyncStatus as com.example.ui.util.SyncStatus.Error).message
                                                                else -> stringResource(R.string.auto_sync_status_idle)
                                                            }
                                                            Text(
                                                                text = statusSubtitle,
                                                                color = when (currentSyncStatus) {
                                                                    is com.example.ui.util.SyncStatus.Success -> Color(0xFF10B981)
                                                                    is com.example.ui.util.SyncStatus.Error -> Color(0xFFEF4444)
                                                                    else -> TextSecondary
                                                                },
                                                                fontSize = 10.5.sp,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }

                                                        Switch(
                                                            checked = isAutoSyncActive,
                                                            onCheckedChange = { checked ->
                                                                viewModel.setAutoSyncEnabled(checked)
                                                            },
                                                            modifier = Modifier.height(28.dp)
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.height(8.dp))

                                                    Text(
                                                        text = stringResource(R.string.auto_sync_desc),
                                                        color = TextSecondary,
                                                        fontSize = 10.5.sp,
                                                        lineHeight = 14.sp
                                                    )

                                                    Spacer(modifier = Modifier.height(10.dp))

                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        OutlinedButton(
                                                            onClick = {
                                                                viewModel.triggerManualSync { success, error ->
                                                                    if (success) {
                                                                        successMessage = "تمت المزامنة الفورية مع Google Drive بنجاح!"
                                                                        loadDriveFiles()
                                                                    } else {
                                                                        errorMessage = error ?: "فشلت المزامنة الفورية"
                                                                    }
                                                                }
                                                            },
                                                            enabled = currentSyncStatus !is com.example.ui.util.SyncStatus.Syncing,
                                                            shape = RoundedCornerShape(8.dp),
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .height(36.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Sync,
                                                                contentDescription = null,
                                                                tint = SleekPrimary,
                                                                modifier = Modifier.size(15.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text(
                                                                text = stringResource(R.string.auto_sync_sync_now),
                                                                fontSize = 11.5.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = SleekPrimary
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))

                                            // Action: Create Separate Snapshot Backup on Drive
                                            Button(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        isUploadingToDrive = true
                                                        errorMessage = null
                                                        val fullJson = viewModel.generateFullBackupJson()
                                                        val fileName = OrbitMindBackupManager.generateExportFileName("All_Universes", true, ".json")
                                                        val uploadRes = GoogleDriveSyncManager.uploadBackupFile(
                                                            context = context,
                                                            account = currentAccount,
                                                            fileName = fileName,
                                                            jsonContent = fullJson,
                                                            description = "OrbitMind Full Backup Snapshot"
                                                        )
                                                        isUploadingToDrive = false
                                                        uploadRes.fold(
                                                            onSuccess = {
                                                                successMessage = "Full backup uploaded to Google Drive successfully!"
                                                                loadDriveFiles()
                                                            },
                                                            onFailure = { err ->
                                                                if (err is com.google.android.gms.auth.UserRecoverableAuthException) {
                                                                    val intent = err.intent
                                                                    if (intent != null) {
                                                                        authRecoveryLauncher.launch(intent)
                                                                    } else {
                                                                        errorMessage = "Google Drive authorization required"
                                                                    }
                                                                } else {
                                                                    errorMessage = "Upload failed: ${err.localizedMessage}"
                                                                }
                                                            }
                                                        )
                                                    }
                                                },
                                                enabled = !isUploadingToDrive && !isDownloadingFromDrive,
                                                colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(44.dp)
                                                    .testTag("upload_to_gdrive_button")
                                            ) {
                                                if (isUploadingToDrive) {
                                                    CircularProgressIndicator(
                                                        color = if (isDark) Color(0xFF1F2D60) else Color.White,
                                                        modifier = Modifier.size(18.dp),
                                                        strokeWidth = 2.dp
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = stringResource(R.string.gdrive_uploading_progress),
                                                        color = if (isDark) Color(0xFF1F2D60) else Color.White,
                                                        fontSize = 13.sp
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Default.CloudUpload,
                                                        contentDescription = null,
                                                        tint = if (isDark) Color(0xFF1F2D60) else Color.White,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = stringResource(R.string.gdrive_backup_all_btn),
                                                        color = if (isDark) Color(0xFF1F2D60) else Color.White,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Cloud Backups List Header
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = stringResource(R.string.gdrive_cloud_backups_title),
                                            color = TextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )

                                        IconButton(
                                            onClick = { loadDriveFiles() },
                                            enabled = !isLoadingDriveFiles,
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = stringResource(R.string.gdrive_refresh_btn),
                                                tint = SleekPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    if (isLoadingDriveFiles) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 20.dp)
                                        ) {
                                            CircularProgressIndicator(
                                                color = SleekPrimary,
                                                modifier = Modifier.size(24.dp),
                                                strokeWidth = 2.dp
                                            )
                                        }
                                    } else if (driveBackups.isEmpty()) {
                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = SleekSurfaceVariant.copy(alpha = 0.5f)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp)
                                        ) {
                                            Text(
                                                text = stringResource(R.string.gdrive_no_backups),
                                                color = TextSecondary,
                                                fontSize = 12.sp,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp)
                                            )
                                        }
                                    } else {
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            driveBackups.forEach { driveFile ->
                                                DriveBackupItemCard(
                                                    file = driveFile,
                                                    isDownloading = isDownloadingFromDrive,
                                                    onRestore = {
                                                        coroutineScope.launch {
                                                            isDownloadingFromDrive = true
                                                            errorMessage = null
                                                            val downloadRes = GoogleDriveSyncManager.downloadBackupFileContent(
                                                                context = context,
                                                                account = currentAccount,
                                                                fileId = driveFile.id
                                                            )
                                                            isDownloadingFromDrive = false
                                                            downloadRes.fold(
                                                                onSuccess = { content ->
                                                                    val previewRes = OrbitMindBackupManager.parseBackupPreview(content)
                                                                    previewRes.fold(
                                                                        onSuccess = { preview ->
                                                                            downloadedDriveJson = content
                                                                            downloadedDrivePreview = preview
                                                                        },
                                                                        onFailure = { err ->
                                                                            errorMessage = "Invalid map format: ${err.localizedMessage}"
                                                                        }
                                                                    )
                                                                },
                                                                onFailure = { err ->
                                                                    errorMessage = "Download error: ${err.localizedMessage}"
                                                                }
                                                            )
                                                        }
                                                    },
                                                    onDelete = {
                                                        coroutineScope.launch {
                                                            val delRes = GoogleDriveSyncManager.deleteBackupFile(
                                                                context = context,
                                                                account = currentAccount,
                                                                fileId = driveFile.id
                                                            )
                                                            delRes.fold(
                                                                onSuccess = {
                                                                    successMessage = "Deleted backup from Google Drive"
                                                                    loadDriveFiles()
                                                                },
                                                                onFailure = { err ->
                                                                    errorMessage = "Failed to delete file: ${err.localizedMessage}"
                                                                }
                                                            )
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    // Render downloaded Drive Preview if ready
                                    val dPreview = downloadedDrivePreview
                                    val dJson = downloadedDriveJson
                                    if (dPreview != null && dJson != null) {
                                        Spacer(modifier = Modifier.height(14.dp))
                                        BackupPreviewCard(
                                            preview = dPreview,
                                            isProcessing = isProcessing,
                                            onConfirmImport = { asNewCopy ->
                                                isProcessing = true
                                                viewModel.importBackupJson(
                                                    jsonString = dJson,
                                                    asNewCopy = asNewCopy,
                                                    onSuccess = { importedMapId ->
                                                        isProcessing = false
                                                        onDismiss()
                                                        if (importedMapId != null) {
                                                            onMapImportedAndSelected(importedMapId)
                                                        }
                                                    },
                                                    onError = { err ->
                                                        isProcessing = false
                                                        errorMessage = err
                                                    }
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        1 -> {
                            // TAB 1: FILE PICKER IMPORT
                            Column(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = {
                                        openDocumentLauncher.launch(
                                            arrayOf(
                                                "application/json",
                                                "application/octet-stream",
                                                "*/*"
                                            )
                                        )
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, SleekPrimary),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = SleekPrimary.copy(alpha = 0.05f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(72.dp)
                                        .testTag("pick_backup_file_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FileUpload,
                                        contentDescription = null,
                                        tint = SleekPrimary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = stringResource(R.string.select_backup_file_btn),
                                            color = SleekPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = stringResource(R.string.select_backup_file_hint),
                                            color = TextSecondary,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Render Preview Card if File Loaded
                                val preview = loadedPreview
                                val rawJson = loadedJsonForImport
                                if (preview != null && rawJson != null) {
                                    BackupPreviewCard(
                                        preview = preview,
                                        isProcessing = isProcessing,
                                        onConfirmImport = { asNewCopy ->
                                            isProcessing = true
                                            viewModel.importBackupJson(
                                                jsonString = rawJson,
                                                asNewCopy = asNewCopy,
                                                onSuccess = { importedMapId ->
                                                    isProcessing = false
                                                    onDismiss()
                                                    if (importedMapId != null) {
                                                        onMapImportedAndSelected(importedMapId)
                                                    }
                                                },
                                                onError = { err ->
                                                    isProcessing = false
                                                    errorMessage = err
                                                }
                                            )
                                        }
                                    )
                                }
                            }
                        }
                        2 -> {
                            // TAB 2: PASTE JSON CODE
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Paste Raw JSON Code",
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f)
                                    )

                                    OutlinedButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = clipboard.primaryClip
                                            if (clip != null && clip.itemCount > 0) {
                                                val text = clip.getItemAt(0).text?.toString() ?: ""
                                                pastedJsonText = text
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorderSubtle),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentPaste,
                                            contentDescription = null,
                                            tint = TextPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = stringResource(R.string.paste_from_clipboard_btn),
                                            color = TextPrimary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = pastedJsonText,
                                    onValueChange = { pastedJsonText = it },
                                    placeholder = { Text(stringResource(R.string.paste_json_hint), fontSize = 12.sp) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedBorderColor = SleekPrimary,
                                        unfocusedBorderColor = SleekBorder,
                                        focusedContainerColor = SleekSurfaceVariant,
                                        unfocusedContainerColor = SleekSurfaceVariant
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    maxLines = 6,
                                    minLines = 4,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("paste_json_input")
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Live Validation from Pasted Text
                                if (pastedJsonText.isNotBlank()) {
                                    val previewResult = remember(pastedJsonText) {
                                        OrbitMindBackupManager.parseBackupPreview(pastedJsonText)
                                    }

                                    previewResult.fold(
                                        onSuccess = { preview ->
                                            BackupPreviewCard(
                                                preview = preview,
                                                isProcessing = isProcessing,
                                                onConfirmImport = { asNewCopy ->
                                                    isProcessing = true
                                                    viewModel.importBackupJson(
                                                        jsonString = pastedJsonText,
                                                        asNewCopy = asNewCopy,
                                                        onSuccess = { importedMapId ->
                                                            isProcessing = false
                                                            onDismiss()
                                                            if (importedMapId != null) {
                                                                onMapImportedAndSelected(importedMapId)
                                                            }
                                                        },
                                                        onError = { err ->
                                                            isProcessing = false
                                                            errorMessage = err
                                                        }
                                                    )
                                                }
                                            )
                                        },
                                        onFailure = {
                                            Text(
                                                text = stringResource(R.string.import_error_invalid),
                                                color = Color(0xFFBA1A1A),
                                                fontSize = 11.sp
                                            )
                                        }
                                    )
                                }
                            }
                        }
                        3 -> {
                            // TAB 3: FULL UNIVERSE BACKUP
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = SleekSurfaceVariant),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorderSubtle),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(SleekPrimary.copy(alpha = 0.15f))
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CloudDownload,
                                                    contentDescription = null,
                                                    tint = SleekPrimary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = stringResource(R.string.full_backup_title),
                                                    color = TextPrimary,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = stringResource(R.string.full_backup_desc),
                                                    color = TextSecondary,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))

                                        if (isExportingFullBackup) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                CircularProgressIndicator(
                                                    color = SleekPrimary,
                                                    modifier = Modifier.size(24.dp),
                                                    strokeWidth = 2.dp
                                                )
                                            }
                                        } else {
                                            // Action 1: Share Full Backup File
                                             Button(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        isExportingFullBackup = true
                                                        val fullJson = viewModel.generateFullBackupJson()
                                                        val fileName = OrbitMindBackupManager.generateExportFileName("All_Universes", true, ".json")
                                                        OrbitMindBackupManager.shareBackupFile(context, fileName, fullJson, "application/json")
                                                        isExportingFullBackup = false
                                                        onDismiss()
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(46.dp)
                                                    .testTag("share_full_backup_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Share,
                                                    contentDescription = null,
                                                    tint = if (isDark) Color(0xFF1F2D60) else Color.White,
                                                    modifier = Modifier.size(17.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Share Full Backup (.json)",
                                                    color = if (isDark) Color(0xFF1F2D60) else Color.White,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            // Action 2: Save to Device
                                            OutlinedButton(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        isExportingFullBackup = true
                                                        val fullJson = viewModel.generateFullBackupJson()
                                                        val fileName = OrbitMindBackupManager.generateExportFileName("All_Universes", true, ".json")
                                                        pendingFullBackupContent = fullJson
                                                        isExportingFullBackup = false
                                                        createFullBackupDocumentLauncher.launch(fileName)
                                                    }
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorder),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(44.dp)
                                                    .testTag("save_full_backup_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.FileDownload,
                                                    contentDescription = null,
                                                    tint = TextPrimary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = stringResource(R.string.action_save_to_device),
                                                    color = TextPrimary,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium
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
    }
}

@Composable
private fun DriveBackupItemCard(
    file: DriveBackupFile,
    isDownloading: Boolean,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SleekSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (file.isFullBackup) SleekPrimary.copy(alpha = 0.15f) else Color(0xFF6366F1).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (file.isFullBackup) Icons.Default.CloudDownload else Icons.Default.Explore,
                    contentDescription = null,
                    tint = if (file.isFullBackup) SleekPrimary else Color(0xFF6366F1),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${file.formattedSize} • ${file.formattedDate}",
                    color = TextTertiary,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onRestore,
                enabled = !isDownloading,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.gdrive_download_and_restore), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.gdrive_delete_file),
                    tint = Color(0xFFBA1A1A).copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun BackupPreviewCard(
    preview: OrbitMindBackupManager.BackupPreview,
    isProcessing: Boolean,
    onConfirmImport: (asNewCopy: Boolean) -> Unit
) {
    val themeColor = remember(preview.primaryMapThemeColor) {
        OrbitColors.parseColor(preview.primaryMapThemeColor)
    }
    var replaceExisting by remember { mutableStateOf(true) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SleekSurfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, SleekPrimary.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("import_preview_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = SleekPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (preview.isFullBackup) {
                        stringResource(R.string.preview_valid_full_backup, preview.mapsCount)
                    } else {
                        stringResource(R.string.preview_valid_map)
                    },
                    color = SleekPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(themeColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = preview.primaryMapTitle,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${preview.totalNodesCount} ${stringResource(R.string.preview_nodes_count).replace("%1\$d ", "")}",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        if (preview.imagesCount > 0) {
                            Text(
                                text = " • ${preview.imagesCount} 🖼️",
                                color = SleekPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            if (preview.isFullBackup && preview.mapTitles.size > 1) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Includes: " + preview.mapTitles.joinToString(", "),
                    color = TextTertiary,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Replace & Sync in-place vs New Copy Selection
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (replaceExisting) SleekPrimary.copy(alpha = 0.08f) else SleekSurface
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (replaceExisting) SleekPrimary.copy(alpha = 0.3f) else SleekBorderSubtle
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { replaceExisting = !replaceExisting }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (replaceExisting) {
                                stringResource(R.string.import_replace_existing)
                            } else {
                                stringResource(R.string.import_as_new_copy)
                            },
                            color = if (replaceExisting) SleekPrimary else TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (replaceExisting) {
                                stringResource(R.string.import_replace_existing_desc)
                            } else {
                                stringResource(R.string.import_as_new_copy_desc)
                            },
                            color = TextTertiary,
                            fontSize = 10.sp
                        )
                    }
                    Switch(
                        checked = replaceExisting,
                        onCheckedChange = { replaceExisting = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SleekPrimary,
                            uncheckedThumbColor = TextTertiary,
                            uncheckedTrackColor = SleekSurfaceVariant
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = { onConfirmImport(!replaceExisting) },
                enabled = !isProcessing,
                colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("confirm_import_button")
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.importing_progress),
                        color = Color.White,
                        fontSize = 13.sp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.confirm_import_btn),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
