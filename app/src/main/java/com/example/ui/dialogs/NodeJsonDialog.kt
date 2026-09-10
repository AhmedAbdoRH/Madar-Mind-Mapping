package com.example.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.ChecklistItem
import com.example.data.model.MindNodeEntity
import com.example.ui.theme.SleekBackground
import com.example.ui.theme.SleekBorder
import com.example.ui.theme.SleekBorderSubtle
import com.example.ui.theme.SleekPrimary
import com.example.ui.theme.SleekSurface
import com.example.ui.theme.SleekSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import org.json.JSONArray
import org.json.JSONObject

data class NodeJsonPayload(
    val title: String,
    val notes: String,
    val colorHex: String,
    val iconName: String,
    val linkUrl: String,
    val imageUri: String?,
    val progress: Int?, // 0..10
    val impact: Int?, // 1..5
    val dueDate: Long? = null, // Epoch timestamp in ms
    val checklist: List<ChecklistItem>,
    val children: List<NodeJsonPayload> = emptyList()
)

object NodeJsonSerializer {
    fun serialize(payload: NodeJsonPayload): String {
        val json = serializePayload(payload)
        return json.toString(2)
    }

    fun buildSubtreePayload(
        targetNode: MindNodeEntity,
        allNodes: List<MindNodeEntity>,
        overrideTitle: String? = null,
        overrideNotes: String? = null,
        overrideColorHex: String? = null,
        overrideIconName: String? = null,
        overrideLinkUrl: String? = null,
        overrideImageUri: String? = null,
        overrideProgress: Int? = null,
        overrideProgressEnabled: Boolean = false,
        overrideImpact: Int? = null,
        overrideImpactEnabled: Boolean = false,
        overrideDueDate: Long? = null,
        overrideDueDateEnabled: Boolean = false,
        overrideChecklist: List<ChecklistItem>? = null
    ): NodeJsonPayload {
        val effectiveTitle = overrideTitle ?: targetNode.title
        val effectiveNotes = overrideNotes ?: targetNode.notes
        val effectiveColor = overrideColorHex ?: targetNode.colorHex
        val effectiveIcon = overrideIconName ?: targetNode.iconName
        val effectiveLink = overrideLinkUrl ?: targetNode.linkUrl
        val effectiveImage = overrideImageUri ?: targetNode.imageUri
        val effectiveProgress = if (overrideTitle != null) {
            if (overrideProgressEnabled) overrideProgress else null
        } else {
            targetNode.progress
        }
        val effectiveImpact = if (overrideTitle != null) {
            if (overrideImpactEnabled) overrideImpact else null
        } else {
            targetNode.impact
        }
        val effectiveDueDate = if (overrideTitle != null) {
            if (overrideDueDateEnabled) overrideDueDate else null
        } else {
            targetNode.dueDate
        }
        val effectiveChecklist = overrideChecklist ?: targetNode.checklist

        val childNodes = allNodes.filter { it.parentId == targetNode.id }.sortedBy { it.orderIndex }
        val childrenPayloads = childNodes.map { child ->
            buildSubtreePayload(child, allNodes)
        }

        return NodeJsonPayload(
            title = effectiveTitle,
            notes = effectiveNotes,
            colorHex = effectiveColor,
            iconName = effectiveIcon,
            linkUrl = effectiveLink,
            imageUri = effectiveImage,
            progress = effectiveProgress,
            impact = effectiveImpact,
            dueDate = effectiveDueDate,
            checklist = effectiveChecklist,
            children = childrenPayloads
        )
    }

    private fun serializePayload(payload: NodeJsonPayload): JSONObject {
        return JSONObject().apply {
            put("title", payload.title)
            put("notes", payload.notes)
            put("colorHex", payload.colorHex)
            put("iconName", payload.iconName)
            if (payload.linkUrl.isNotBlank()) put("linkUrl", payload.linkUrl)
            if (!payload.imageUri.isNullOrBlank()) put("imageUri", payload.imageUri)
            
            // Explicit progress level (0..10)
            if (payload.progress != null) {
                put("progress", payload.progress)
            } else {
                put("progress", JSONObject.NULL)
            }

            // Explicit impact intensity (1..5)
            if (payload.impact != null) {
                put("impact", payload.impact)
            } else {
                put("impact", JSONObject.NULL)
            }

            // Due Date timestamp
            if (payload.dueDate != null && payload.dueDate > 0L) {
                put("dueDate", payload.dueDate)
            } else {
                put("dueDate", JSONObject.NULL)
            }

            if (payload.checklist.isNotEmpty()) {
                val array = JSONArray()
                payload.checklist.forEach { item ->
                    val itemObj = JSONObject().apply {
                        put("text", item.text)
                        put("isDone", item.isDone)
                    }
                    array.put(itemObj)
                }
                put("checklist", array)
            } else {
                put("checklist", JSONArray())
            }

            if (payload.children.isNotEmpty()) {
                val childrenArray = JSONArray()
                payload.children.forEach { child ->
                    childrenArray.put(serializePayload(child))
                }
                put("children", childrenArray)
            } else {
                put("children", JSONArray())
            }
        }
    }

    fun deserialize(jsonStr: String): NodeJsonPayload? {
        return try {
            val json = JSONObject(jsonStr)
            deserializePayload(json)
        } catch (e: Exception) {
            null
        }
    }

    private fun deserializePayload(json: JSONObject): NodeJsonPayload {
        val title = json.optString("title", "").ifBlank {
            json.optString("name", "عقدة")
        }
        val notes = json.optString("notes", json.optString("description", ""))
        val colorHex = json.optString("colorHex", json.optString("color", "#6366F1"))
        val iconName = json.optString("iconName", json.optString("icon", "lightbulb"))
        val linkUrl = json.optString("linkUrl", json.optString("url", ""))
        val imageUri = if (json.has("imageUri") && !json.isNull("imageUri")) json.optString("imageUri", null) else null

        // Support progress, rating, evaluation, score
        val progress = when {
            json.has("progress") && !json.isNull("progress") -> json.optInt("progress", 0).coerceIn(0, 10)
            json.has("rating") && !json.isNull("rating") -> json.optInt("rating", 0).coerceIn(0, 10)
            json.has("evaluation") && !json.isNull("evaluation") -> json.optInt("evaluation", 0).coerceIn(0, 10)
            json.has("score") && !json.isNull("score") -> json.optInt("score", 0).coerceIn(0, 10)
            else -> null
        }

        // Support impact, impactPower, power, priority
        val impact = when {
            json.has("impact") && !json.isNull("impact") -> json.optInt("impact", 3).coerceIn(1, 5)
            json.has("impactPower") && !json.isNull("impactPower") -> json.optInt("impactPower", 3).coerceIn(1, 5)
            json.has("power") && !json.isNull("power") -> json.optInt("power", 3).coerceIn(1, 5)
            json.has("priority") && !json.isNull("priority") -> json.optInt("priority", 3).coerceIn(1, 5)
            else -> null
        }

        // Support dueDate / deadline timestamp
        val dueDate = when {
            json.has("dueDate") && !json.isNull("dueDate") -> json.optLong("dueDate", 0L).takeIf { it > 0L }
            json.has("deadline") && !json.isNull("deadline") -> json.optLong("deadline", 0L).takeIf { it > 0L }
            else -> null
        }

        val checklist = mutableListOf<ChecklistItem>()
        if (json.has("checklist")) {
            val array = json.optJSONArray("checklist")
            if (array != null) {
                for (i in 0 until array.length()) {
                    val itemObj = array.optJSONObject(i)
                    if (itemObj != null) {
                        val text = itemObj.optString("text", itemObj.optString("title", ""))
                        val isDone = itemObj.optBoolean("isDone", false)
                        if (text.isNotBlank()) {
                            checklist.add(ChecklistItem(text = text, isDone = isDone))
                        }
                    } else {
                        val itemStr = array.optString(i, "")
                        if (itemStr.isNotBlank()) {
                            checklist.add(ChecklistItem(text = itemStr, isDone = false))
                        }
                    }
                }
            }
        }

        val childrenList = mutableListOf<NodeJsonPayload>()
        val childrenArray = json.optJSONArray("children")
            ?: json.optJSONArray("subnodes")
            ?: json.optJSONArray("subNodes")
            ?: json.optJSONArray("child_nodes")

        if (childrenArray != null) {
            for (i in 0 until childrenArray.length()) {
                val childObj = childrenArray.optJSONObject(i)
                if (childObj != null) {
                    childrenList.add(deserializePayload(childObj))
                }
            }
        }

        return NodeJsonPayload(
            title = title,
            notes = notes,
            colorHex = colorHex,
            iconName = iconName,
            linkUrl = linkUrl,
            imageUri = imageUri,
            progress = progress,
            impact = impact,
            dueDate = dueDate,
            checklist = checklist,
            children = childrenList
        )
    }
}

@Composable
fun NodeJsonDialog(
    initialPayload: NodeJsonPayload,
    onDismiss: () -> Unit,
    onApply: (NodeJsonPayload) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = remember {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    var jsonText by remember {
        mutableStateOf(NodeJsonSerializer.serialize(initialPayload))
    }
    var parseError by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = SleekSurface,
                border = BorderStroke(1.dp, SleekBorderSubtle),
                shadowElevation = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(SleekPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DataObject,
                                    contentDescription = null,
                                    tint = SleekPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "بيانات العقدة (JSON)",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "تعديل أو نسخ أو استبدال بيانات العقدة فورياً",
                                    fontSize = 11.5.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quick Actions (Copy & Paste)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = {
                                val clip = ClipData.newPlainText("Node JSON", jsonText)
                                clipboardManager.setPrimaryClip(clip)
                                Toast.makeText(context, "تم نسخ JSON العقدة إلى الحافظة 📋", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, SleekBorderSubtle),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("نسخ JSON", fontSize = 12.sp, color = TextPrimary)
                        }

                        OutlinedButton(
                            onClick = {
                                val clip = clipboardManager.primaryClip
                                if (clip != null && clip.itemCount > 0) {
                                    val text = clip.getItemAt(0).text?.toString() ?: ""
                                    if (text.isNotBlank()) {
                                        jsonText = text
                                        parseError = null
                                        Toast.makeText(context, "تم اللصق من الحافظة 📥", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, SleekBorderSubtle),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("لصق واستبدال", fontSize = 12.sp, color = TextPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Monospace Code Editor Field
                    OutlinedTextField(
                        value = jsonText,
                        onValueChange = {
                            jsonText = it
                            parseError = null
                        },
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = TextPrimary,
                            lineHeight = 16.sp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SleekPrimary,
                            unfocusedBorderColor = SleekBorderSubtle,
                            focusedContainerColor = SleekSurfaceElevated,
                            unfocusedContainerColor = SleekSurfaceElevated
                        ),
                        placeholder = {
                            Text(
                                text = "{\"title\": \"اسم العقدة\"...}",
                                color = TextTertiary,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    )

                    if (parseError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = parseError!!,
                            color = Color(0xFFEF4444),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Bottom Buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إلغاء", color = TextSecondary)
                        }

                        Button(
                            onClick = {
                                val parsed = NodeJsonSerializer.deserialize(jsonText)
                                if (parsed != null) {
                                    onApply(parsed)
                                    Toast.makeText(context, "تم تطبيق بيانات العقدة بنجاح ✨", Toast.LENGTH_SHORT).show()
                                } else {
                                    parseError = "صيغة JSON غير صحيحة. يرجى التأكد من الأقواس والعلامات."
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تطبيق واستبدال", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
