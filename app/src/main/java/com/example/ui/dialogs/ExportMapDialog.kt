package com.example.ui.dialogs

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.model.MindMapEntity
import com.example.data.model.MindNodeEntity
import com.example.ui.theme.AppTheme
import com.example.ui.theme.SleekBorder
import com.example.ui.theme.SleekBorderSubtle
import com.example.ui.theme.SleekPrimary
import com.example.ui.theme.SleekSurface
import com.example.ui.theme.SleekSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.util.GoogleDriveSyncManager
import com.example.ui.util.OrbitColors
import com.example.ui.util.OrbitMindBackupManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

import com.example.ui.util.MindMapExportVisualizer
import android.content.Intent

enum class ExportFormat {
    ORBITMIND_JSON,
    IMAGE_WITH_BG,
    IMAGE_TRANSPARENT,
    PDF_DOCUMENT,
    MARKDOWN_OUTLINE,
    PLAIN_TEXT
}

enum class ExportScope {
    ENTIRE_MAP,
    CURRENT_BRANCH
}

@Composable
fun ExportMapDialog(
    map: MindMapEntity,
    nodes: List<MindNodeEntity>,
    currentNode: MindNodeEntity? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isDark = AppTheme.colors.isDark

    // Scope selection: Only show choice if we are currently standing on a sub-node
    val canExportBranch = currentNode != null && currentNode.parentId != null
    var selectedScope by remember {
        mutableStateOf(if (canExportBranch) ExportScope.CURRENT_BRANCH else ExportScope.ENTIRE_MAP)
    }

    var selectedFormat by remember { mutableStateOf(ExportFormat.ORBITMIND_JSON) }
    var isExporting by remember { mutableStateOf(false) }
    var isUploadingToDrive by remember { mutableStateOf(false) }

    // Content cached when preparing to save to file URI
    var pendingExportContent by remember { mutableStateOf<String?>(null) }
    var googleAccount by remember { mutableStateOf(GoogleDriveSyncManager.getLastSignedInAccount(context)) }

    // Calculate effective nodes based on selected scope
    val effectiveNodes = remember(selectedScope, nodes, currentNode) {
        if (selectedScope == ExportScope.CURRENT_BRANCH && currentNode != null) {
            OrbitMindBackupManager.extractSubtreeNodes(currentNode.id, nodes)
        } else {
            nodes
        }
    }

    val effectiveTitle = remember(selectedScope, map, currentNode) {
        if (selectedScope == ExportScope.CURRENT_BRANCH && currentNode != null) {
            "${map.title} - ${currentNode.title}"
        } else {
            map.title
        }
    }

    val rootNodeIdOverride = remember(selectedScope, currentNode) {
        if (selectedScope == ExportScope.CURRENT_BRANCH && currentNode != null) {
            currentNode.id
        } else {
            null
        }
    }

    var triggerUploadDriveAccount by remember { mutableStateOf<com.google.android.gms.auth.api.signin.GoogleSignInAccount?>(null) }

    val authRecoveryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val account = googleAccount
            if (account != null) {
                triggerUploadDriveAccount = account
            }
        }
    }

    fun uploadMapToDrive(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount) {
        coroutineScope.launch {
            isUploadingToDrive = true
            val json = OrbitMindBackupManager.createSingleMapOrBranchBackupJson(
                context = context,
                map = map,
                nodes = effectiveNodes,
                rootNodeIdOverride = rootNodeIdOverride
            )
            val fileName = OrbitMindBackupManager.generateExportFileName(effectiveTitle, false, ".json")
            val uploadRes = GoogleDriveSyncManager.uploadBackupFile(
                context = context,
                account = account,
                fileName = fileName,
                jsonContent = json,
                description = "Madar Map: $effectiveTitle"
            )
            isUploadingToDrive = false
            uploadRes.fold(
                onSuccess = {
                    Toast.makeText(context, "Saved to Google Drive: $fileName", Toast.LENGTH_LONG).show()
                    onDismiss()
                },
                onFailure = { err ->
                    if (err is com.google.android.gms.auth.UserRecoverableAuthException) {
                        val intent = err.intent
                        if (intent != null) {
                            authRecoveryLauncher.launch(intent)
                        } else {
                            Toast.makeText(context, "Google Drive authorization required", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Drive upload failed: ${err.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
            )
        }
    }

    LaunchedEffect(triggerUploadDriveAccount) {
        val account = triggerUploadDriveAccount
        if (account != null) {
            triggerUploadDriveAccount = null
            uploadMapToDrive(account)
        }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val signedAccount = task.getResult(ApiException::class.java)
            googleAccount = signedAccount
            uploadMapToDrive(signedAccount)
        } catch (e: Exception) {
            Toast.makeText(context, "Google Drive sign-in canceled or failed", Toast.LENGTH_SHORT).show()
        }
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val content = pendingExportContent
        if (uri != null && content != null) {
            val success = OrbitMindBackupManager.writeContentToDocumentUri(context, uri, content)
            if (success) {
                Toast.makeText(context, "Saved successfully to device!", Toast.LENGTH_SHORT).show()
                onDismiss()
            } else {
                Toast.makeText(context, "Error saving file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val themeColor = remember(map.themeColorHex, currentNode, selectedScope) {
        if (selectedScope == ExportScope.CURRENT_BRANCH && currentNode != null) {
            OrbitColors.parseColor(currentNode.colorHex)
        } else {
            OrbitColors.parseColor(map.themeColorHex)
        }
    }

    val imagesCount = remember(effectiveNodes) {
        effectiveNodes.count { !it.imageUri.isNullOrBlank() }
    }

    Dialog(
        onDismissRequest = { if (!isExporting && !isUploadingToDrive) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SleekSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorderSubtle),
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
                .testTag("export_map_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(SleekPrimary.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            tint = SleekPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.export_dialog_title),
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.export_dialog_subtitle),
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        enabled = !isExporting && !isUploadingToDrive,
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

                Spacer(modifier = Modifier.height(16.dp))

                // Map/Branch Identity Card Preview
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekSurfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(themeColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (selectedScope == ExportScope.CURRENT_BRANCH) Icons.Default.AccountTree else Icons.Default.Explore,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = effectiveTitle,
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${effectiveNodes.size} ${stringResource(R.string.preview_nodes_count).replace("%1\$d ", "")}",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                                if (imagesCount > 0) {
                                    Text(
                                        text = " • $imagesCount 🖼️",
                                        color = SleekPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // Scope Selector (If standing on a sub-branch / child node)
                if (canExportBranch && currentNode != null) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.export_scope_title),
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Option A: Entire Map
                    ScopeOptionCard(
                        title = stringResource(R.string.export_scope_entire_map),
                        desc = stringResource(R.string.export_scope_entire_map_desc),
                        badge = "${nodes.size} nodes",
                        isSelected = selectedScope == ExportScope.ENTIRE_MAP,
                        onClick = { selectedScope = ExportScope.ENTIRE_MAP }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Option B: Current Node & Descendant Children
                    ScopeOptionCard(
                        title = stringResource(R.string.export_scope_current_branch),
                        desc = stringResource(R.string.export_scope_current_branch_desc, currentNode.title),
                        badge = "${effectiveNodes.size} nodes",
                        isSelected = selectedScope == ExportScope.CURRENT_BRANCH,
                        onClick = { selectedScope = ExportScope.CURRENT_BRANCH }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Select Export Format",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Format Selector Options
                FormatOptionCard(
                    title = stringResource(R.string.format_orbitmind_json),
                    desc = stringResource(R.string.format_orbitmind_json_desc),
                    badge = "Full Fidelity 🪐",
                    isSelected = selectedFormat == ExportFormat.ORBITMIND_JSON,
                    onClick = { selectedFormat = ExportFormat.ORBITMIND_JSON }
                )

                Spacer(modifier = Modifier.height(8.dp))

                FormatOptionCard(
                    title = stringResource(R.string.format_image_with_bg),
                    desc = stringResource(R.string.format_image_with_bg_desc),
                    badge = "PNG Image 🖼️",
                    isSelected = selectedFormat == ExportFormat.IMAGE_WITH_BG,
                    onClick = { selectedFormat = ExportFormat.IMAGE_WITH_BG }
                )

                Spacer(modifier = Modifier.height(8.dp))

                FormatOptionCard(
                    title = stringResource(R.string.format_image_transparent),
                    desc = stringResource(R.string.format_image_transparent_desc),
                    badge = "Transparent 🎨",
                    isSelected = selectedFormat == ExportFormat.IMAGE_TRANSPARENT,
                    onClick = { selectedFormat = ExportFormat.IMAGE_TRANSPARENT }
                )

                Spacer(modifier = Modifier.height(8.dp))

                FormatOptionCard(
                    title = stringResource(R.string.format_pdf_document),
                    desc = stringResource(R.string.format_pdf_document_desc),
                    badge = "Vector PDF 📄",
                    isSelected = selectedFormat == ExportFormat.PDF_DOCUMENT,
                    onClick = { selectedFormat = ExportFormat.PDF_DOCUMENT }
                )

                Spacer(modifier = Modifier.height(8.dp))

                FormatOptionCard(
                    title = stringResource(R.string.format_markdown),
                    desc = stringResource(R.string.format_markdown_desc),
                    badge = "Markdown 📝",
                    isSelected = selectedFormat == ExportFormat.MARKDOWN_OUTLINE,
                    onClick = { selectedFormat = ExportFormat.MARKDOWN_OUTLINE }
                )

                Spacer(modifier = Modifier.height(8.dp))

                FormatOptionCard(
                    title = stringResource(R.string.format_plain_text),
                    desc = stringResource(R.string.format_plain_text_desc),
                    badge = "Text 📄",
                    isSelected = selectedFormat == ExportFormat.PLAIN_TEXT,
                    onClick = { selectedFormat = ExportFormat.PLAIN_TEXT }
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (isExporting || isUploadingToDrive) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                color = SleekPrimary,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isUploadingToDrive) stringResource(R.string.gdrive_uploading_progress) else stringResource(R.string.exporting_progress),
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    // Action 1: Google Drive Direct Backup Button (if ORBITMIND_JSON)
                    if (selectedFormat == ExportFormat.ORBITMIND_JSON) {
                        Button(
                            onClick = {
                                val currentAcc = googleAccount
                                if (currentAcc != null) {
                                    uploadMapToDrive(currentAcc)
                                } else {
                                    val client = GoogleDriveSyncManager.getGoogleSignInClient(context)
                                    googleSignInLauncher.launch(client.signInIntent)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("export_gdrive_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF1F2D60) else Color.White,
                                modifier = Modifier.size(19.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Save to Google Drive ☁️",
                                color = if (isDark) Color(0xFF1F2D60) else Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Action 2: Share File (or share text / image / pdf)
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                isExporting = true
                                try {
                                    when (selectedFormat) {
                                        ExportFormat.ORBITMIND_JSON -> {
                                            val json = OrbitMindBackupManager.createSingleMapOrBranchBackupJson(
                                                context = context,
                                                map = map,
                                                nodes = effectiveNodes,
                                                rootNodeIdOverride = rootNodeIdOverride
                                            )
                                            val fileName = OrbitMindBackupManager.generateExportFileName(effectiveTitle, false, ".json")
                                            OrbitMindBackupManager.shareBackupFile(context, fileName, json, "application/json")
                                        }
                                        ExportFormat.IMAGE_WITH_BG, ExportFormat.IMAGE_TRANSPARENT -> {
                                            val withBg = selectedFormat == ExportFormat.IMAGE_WITH_BG
                                            val bitmap = MindMapExportVisualizer.renderMapToBitmap(
                                                context = context,
                                                map = map,
                                                nodes = effectiveNodes,
                                                rootNodeIdOverride = rootNodeIdOverride,
                                                withBackground = withBg,
                                                isDark = isDark
                                            )
                                            val fileName = OrbitMindBackupManager.generateExportFileName(effectiveTitle, false, if (withBg) ".png" else "_transparent.png")
                                            val uri = MindMapExportVisualizer.saveBitmapToCacheAndGetUri(context, bitmap, fileName)
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "image/png"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                putExtra(Intent.EXTRA_SUBJECT, effectiveTitle)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, effectiveTitle))
                                        }
                                        ExportFormat.PDF_DOCUMENT -> {
                                            val pdfUri = MindMapExportVisualizer.generatePdf(
                                                context = context,
                                                map = map,
                                                nodes = effectiveNodes,
                                                rootNodeIdOverride = rootNodeIdOverride,
                                                isDark = isDark
                                            )
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "application/pdf"
                                                putExtra(Intent.EXTRA_STREAM, pdfUri)
                                                putExtra(Intent.EXTRA_SUBJECT, "$effectiveTitle.pdf")
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, effectiveTitle))
                                        }
                                        ExportFormat.MARKDOWN_OUTLINE -> {
                                            val md = OrbitMindBackupManager.exportToPlainText(
                                                map = map,
                                                nodes = effectiveNodes,
                                                rootNodeIdOverride = rootNodeIdOverride
                                            )
                                            val fileName = OrbitMindBackupManager.generateExportFileName(effectiveTitle, false, ".md")
                                            OrbitMindBackupManager.shareBackupFile(context, fileName, md, "text/markdown")
                                        }
                                        ExportFormat.PLAIN_TEXT -> {
                                            val txt = OrbitMindBackupManager.exportToPlainText(
                                                map = map,
                                                nodes = effectiveNodes,
                                                rootNodeIdOverride = rootNodeIdOverride
                                            )
                                            val fileName = OrbitMindBackupManager.generateExportFileName(effectiveTitle, false, ".txt")
                                            OrbitMindBackupManager.shareBackupFile(context, fileName, txt, "text/plain")
                                        }
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Export error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                                isExporting = false
                                onDismiss()
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("export_share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.action_share_file),
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Save to Device Storage
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    isExporting = true
                                    try {
                                        when (selectedFormat) {
                                            ExportFormat.ORBITMIND_JSON -> {
                                                val content = OrbitMindBackupManager.createSingleMapOrBranchBackupJson(
                                                    context = context,
                                                    map = map,
                                                    nodes = effectiveNodes,
                                                    rootNodeIdOverride = rootNodeIdOverride
                                                )
                                                pendingExportContent = content
                                                val fileName = OrbitMindBackupManager.generateExportFileName(effectiveTitle, false, ".json")
                                                createDocumentLauncher.launch(fileName)
                                            }
                                            ExportFormat.IMAGE_WITH_BG, ExportFormat.IMAGE_TRANSPARENT -> {
                                                val withBg = selectedFormat == ExportFormat.IMAGE_WITH_BG
                                                val bitmap = MindMapExportVisualizer.renderMapToBitmap(
                                                    context = context,
                                                    map = map,
                                                    nodes = effectiveNodes,
                                                    rootNodeIdOverride = rootNodeIdOverride,
                                                    withBackground = withBg,
                                                    isDark = isDark
                                                )
                                                val fileName = OrbitMindBackupManager.generateExportFileName(effectiveTitle, false, if (withBg) ".png" else "_transparent.png")
                                                val uri = MindMapExportVisualizer.saveBitmapToCacheAndGetUri(context, bitmap, fileName)
                                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "image/png"
                                                    putExtra(Intent.EXTRA_STREAM, uri)
                                                    putExtra(Intent.EXTRA_SUBJECT, effectiveTitle)
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                context.startActivity(Intent.createChooser(shareIntent, "Save or Send $effectiveTitle"))
                                                onDismiss()
                                            }
                                            ExportFormat.PDF_DOCUMENT -> {
                                                val pdfUri = MindMapExportVisualizer.generatePdf(
                                                    context = context,
                                                    map = map,
                                                    nodes = effectiveNodes,
                                                    rootNodeIdOverride = rootNodeIdOverride,
                                                    isDark = isDark
                                                )
                                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "application/pdf"
                                                    putExtra(Intent.EXTRA_STREAM, pdfUri)
                                                    putExtra(Intent.EXTRA_SUBJECT, "$effectiveTitle.pdf")
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                context.startActivity(Intent.createChooser(shareIntent, "Save or Send $effectiveTitle"))
                                                onDismiss()
                                            }
                                            ExportFormat.MARKDOWN_OUTLINE -> {
                                                val md = OrbitMindBackupManager.exportToPlainText(
                                                    map = map,
                                                    nodes = effectiveNodes,
                                                    rootNodeIdOverride = rootNodeIdOverride
                                                )
                                                pendingExportContent = md
                                                val fileName = OrbitMindBackupManager.generateExportFileName(effectiveTitle, false, ".md")
                                                createDocumentLauncher.launch(fileName)
                                            }
                                            ExportFormat.PLAIN_TEXT -> {
                                                val txt = OrbitMindBackupManager.exportToPlainText(
                                                    map = map,
                                                    nodes = effectiveNodes,
                                                    rootNodeIdOverride = rootNodeIdOverride
                                                )
                                                pendingExportContent = txt
                                                val fileName = OrbitMindBackupManager.generateExportFileName(effectiveTitle, false, ".txt")
                                                createDocumentLauncher.launch(fileName)
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    }
                                    isExporting = false
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorder),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("export_save_device_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = null,
                                tint = TextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.action_save_to_device),
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Copy to Clipboard
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    isExporting = true
                                    val content = when (selectedFormat) {
                                        ExportFormat.ORBITMIND_JSON -> OrbitMindBackupManager.createSingleMapOrBranchBackupJson(
                                            context = context,
                                            map = map,
                                            nodes = effectiveNodes,
                                            rootNodeIdOverride = rootNodeIdOverride
                                        )
                                        ExportFormat.IMAGE_WITH_BG, ExportFormat.IMAGE_TRANSPARENT, ExportFormat.PDF_DOCUMENT,
                                        ExportFormat.MARKDOWN_OUTLINE, ExportFormat.PLAIN_TEXT -> OrbitMindBackupManager.exportToPlainText(
                                            map = map,
                                            nodes = effectiveNodes,
                                            rootNodeIdOverride = rootNodeIdOverride
                                        )
                                    }
                                    OrbitMindBackupManager.copyToClipboard(context, effectiveTitle, content)
                                    isExporting = false
                                    onDismiss()
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorder),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("export_copy_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = TextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.action_copy_content),
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScopeOptionCard(
    title: String,
    desc: String,
    badge: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) SleekPrimary else SleekBorderSubtle
    val bgColor = if (isSelected) SleekPrimary.copy(alpha = 0.08f) else SleekSurfaceVariant

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = androidx.compose.foundation.BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor),
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
                    .size(22.dp)
                    .clip(CircleShape)
                    .border(
                        1.5.dp,
                        if (isSelected) SleekPrimary else TextTertiary,
                        CircleShape
                    )
                    .background(if (isSelected) SleekPrimary else Color.Transparent)
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SleekPrimary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = badge,
                            color = SleekPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = desc,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
private fun FormatOptionCard(
    title: String,
    desc: String,
    badge: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) SleekPrimary else SleekBorderSubtle
    val bgColor = if (isSelected) SleekPrimary.copy(alpha = 0.08f) else SleekSurfaceVariant

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = androidx.compose.foundation.BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor),
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
                    .size(22.dp)
                    .clip(CircleShape)
                    .border(
                        1.5.dp,
                        if (isSelected) SleekPrimary else TextTertiary,
                        CircleShape
                    )
                    .background(if (isSelected) SleekPrimary else Color.Transparent)
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SleekPrimary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = badge,
                            color = SleekPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = desc,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}
