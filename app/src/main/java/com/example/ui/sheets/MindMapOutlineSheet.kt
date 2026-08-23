package com.example.ui.sheets

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MindMapEntity
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

data class OutlineNodeItem(
    val node: MindNodeEntity,
    val depth: Int,
    val childCount: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MindMapOutlineSheet(
    map: MindMapEntity,
    allNodes: List<MindNodeEntity>,
    onSelectNode: (String) -> Unit,
    onDismiss: () -> Unit,
    exportMarkdown: String
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var filterQuery by remember { mutableStateOf("") }

    // Build flattened hierarchical tree list
    val flattenedTree = remember(allNodes, filterQuery) {
        val rootNode = allNodes.firstOrNull { it.parentId == null }
        val result = mutableListOf<OutlineNodeItem>()

        fun traverse(node: MindNodeEntity, depth: Int) {
            val children = allNodes.filter { it.parentId == node.id }.sortedBy { it.orderIndex }
            val matches = filterQuery.isBlank() || node.title.contains(filterQuery, ignoreCase = true) || node.notes.contains(filterQuery, ignoreCase = true)
            if (matches || children.isNotEmpty()) {
                result.add(OutlineNodeItem(node, depth, children.size))
            }
            for (child in children) {
                traverse(child, depth + 1)
            }
        }

        if (rootNode != null) {
            traverse(rootNode, 0)
        }
        result
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SleekSurface,
        dragHandle = null,
        modifier = Modifier.testTag("mind_map_outline_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(22.dp)
        ) {
            // Top Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Map Outline",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${allNodes.size} total concepts in '${map.title}'",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SleekSurfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search filter
            OutlinedTextField(
                value = filterQuery,
                onValueChange = { filterQuery = it },
                placeholder = { Text("Filter concepts...") },
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

            Spacer(modifier = Modifier.height(14.dp))

            // Tree List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(flattenedTree) { item ->
                    val nodeColor = OrbitColors.parseColor(item.node.colorHex)
                    val nodeTextColor = OrbitColors.getContrastingTextColor(nodeColor)
                    val isRoot = item.depth == 0

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .padding(start = (item.depth * 18).dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isRoot) SleekPrimary.copy(alpha = 0.10f) else SleekSurfaceVariant.copy(alpha = 0.6f))
                            .border(
                                width = 1.dp,
                                color = if (isRoot) SleekPrimary.copy(alpha = 0.3f) else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                onSelectNode(item.node.id)
                                onDismiss()
                            }
                            .padding(horizontal = 12.dp, vertical = 9.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(nodeColor)
                                .border(1.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            val itemIcon = OrbitIcons.getIcon(item.node.iconName)
                            if (itemIcon != null) {
                                Icon(
                                    imageVector = itemIcon,
                                    contentDescription = null,
                                    tint = nodeTextColor,
                                    modifier = Modifier.size(15.dp)
                                )
                            } else {
                                Text(
                                    text = item.node.title.take(1).uppercase().ifBlank { "•" },
                                    color = nodeTextColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.node.title,
                                color = TextPrimary,
                                fontSize = if (isRoot) 15.sp else 13.sp,
                                fontWeight = if (isRoot) FontWeight.Bold else FontWeight.Medium
                            )
                            if (item.node.notes.isNotBlank()) {
                                Text(
                                    text = item.node.notes,
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }
                        }

                        if (item.childCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SleekSurfaceElevated,
                                border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorderSubtle),
                                modifier = Modifier.padding(start = 6.dp)
                            ) {
                                Text(
                                    text = "${item.childCount}",
                                    color = TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = SleekBorderSubtle)
            Spacer(modifier = Modifier.height(14.dp))

            // Export Actions
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("OrbitMind Markdown", exportMarkdown)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Markdown copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorder),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Outline", fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, exportMarkdown)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "Share Mind Map")
                        context.startActivity(shareIntent)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SleekPrimary,
                        contentColor = if (AppTheme.colors.isDark) Color(0xFF1F2D60) else Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share Outline", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

