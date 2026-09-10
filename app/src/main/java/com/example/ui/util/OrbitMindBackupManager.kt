package com.example.ui.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.ChecklistItem
import com.example.data.model.MindMapEntity
import com.example.data.model.MindNodeEntity
import com.example.data.repository.MindMapRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Robust, high-precision export and import manager for OrbitMind.
 * Supports full single-map and multi-map backups, complete node hierarchy preservation,
 * live sync twin re-mapping, embedded Base64 image portability, and formatted text/markdown export.
 */
object OrbitMindBackupManager {

    const val FORMAT_IDENTIFIER = "madar_backup"
    const val CURRENT_VERSION = 1
    const val FILE_EXTENSION = ".json"

    data class MapExportPackage(
        val map: MindMapEntity,
        val nodes: List<MindNodeEntity>
    )

    data class BackupPreview(
        val isFullBackup: Boolean,
        val mapsCount: Int,
        val totalNodesCount: Int,
        val imagesCount: Int,
        val exportedAt: Long,
        val mapTitles: List<String>,
        val primaryMapTitle: String,
        val primaryMapThemeColor: String,
        val rawJson: String
    )

    data class ImportResult(
        val importedMapIds: List<Long>,
        val totalMaps: Int,
        val totalNodes: Int,
        val primaryMapId: Long?
    )

    // ==========================================
    // EXPORT IMPLEMENTATION
    // ==========================================

    /**
     * Builds a comprehensive JSON backup string for a single mind map with all its nodes,
     * styles, tasks, notes, links, and embedded images.
     */
    suspend fun createSingleMapBackupJson(
        context: Context,
        map: MindMapEntity,
        nodes: List<MindNodeEntity>
    ): String = withContext(Dispatchers.IO) {
        val rootJson = JSONObject()
        rootJson.put("format", FORMAT_IDENTIFIER)
        rootJson.put("version", CURRENT_VERSION)
        rootJson.put("backupType", "single_map")
        rootJson.put("exportedAt", System.currentTimeMillis())
        rootJson.put("appVersion", "1.0")

        // Map Metadata
        val mapJson = mapToJson(map)
        rootJson.put("map", mapJson)

        // Nodes & Image extraction
        val nodesArray = JSONArray()
        val imagesMap = JSONObject()

        for (node in nodes) {
            val nodeJson = nodeToJson(node)

            // If node has an image, encode to base64
            if (!node.imageUri.isNullOrBlank()) {
                val imageKey = tryEncodeImageToBase64(node.imageUri)
                if (imageKey != null) {
                    nodeJson.put("imageFileName", imageKey.first)
                    imagesMap.put(imageKey.first, imageKey.second)
                }
            }

            nodesArray.put(nodeJson)
        }

        rootJson.put("nodes", nodesArray)
        rootJson.put("images", imagesMap)

        return@withContext rootJson.toString(2)
    }

    /**
     * Extracts a node and all of its recursive descendants from a list of nodes.
     */
    fun extractSubtreeNodes(startNodeId: String, allNodes: List<MindNodeEntity>): List<MindNodeEntity> {
        val startNode = allNodes.firstOrNull { it.id == startNodeId } ?: return allNodes
        val result = mutableListOf<MindNodeEntity>()
        val queue = ArrayDeque<String>()
        queue.add(startNodeId)

        val visited = mutableSetOf<String>()

        while (queue.isNotEmpty()) {
            val currId = queue.removeFirst()
            if (visited.add(currId)) {
                val node = allNodes.firstOrNull { it.id == currId }
                if (node != null) {
                    result.add(node)
                    val children = allNodes.filter { it.parentId == currId }
                    for (child in children) {
                        queue.add(child.id)
                    }
                }
            }
        }

        return result
    }

    /**
     * Builds a comprehensive JSON backup string for a single mind map or a focused subtree branch.
     * If rootNodeOverride is provided, that node becomes the root of the exported structure.
     */
    suspend fun createSingleMapOrBranchBackupJson(
        context: Context,
        map: MindMapEntity,
        nodes: List<MindNodeEntity>,
        rootNodeIdOverride: String? = null
    ): String = withContext(Dispatchers.IO) {
        val exportNodes = if (!rootNodeIdOverride.isNullOrBlank()) {
            extractSubtreeNodes(rootNodeIdOverride, nodes).map { node ->
                if (node.id == rootNodeIdOverride) {
                    node.copy(parentId = null) // Make the selected branch node the root of the exported map
                } else {
                    node
                }
            }
        } else {
            nodes
        }

        val exportMap = if (!rootNodeIdOverride.isNullOrBlank()) {
            val startNode = nodes.firstOrNull { it.id == rootNodeIdOverride }
            map.copy(
                title = startNode?.title?.ifBlank { map.title } ?: map.title,
                rootNodeId = rootNodeIdOverride,
                themeColorHex = startNode?.colorHex ?: map.themeColorHex
            )
        } else {
            map
        }

        return@withContext createSingleMapBackupJson(context, exportMap, exportNodes)
    }

    /**
     * Builds a full application backup JSON containing all maps and nodes in the universe.
     */
    suspend fun createFullUniverseBackupJson(
        context: Context,
        allMapsWithNodes: List<MapExportPackage>
    ): String = withContext(Dispatchers.IO) {
        val rootJson = JSONObject()
        rootJson.put("format", FORMAT_IDENTIFIER)
        rootJson.put("version", CURRENT_VERSION)
        rootJson.put("backupType", "full_backup")
        rootJson.put("exportedAt", System.currentTimeMillis())
        rootJson.put("appVersion", "1.0")

        val mapsArray = JSONArray()
        val imagesMap = JSONObject()

        for (pkg in allMapsWithNodes) {
            val pkgJson = JSONObject()
            pkgJson.put("map", mapToJson(pkg.map))

            val nodesArray = JSONArray()
            for (node in pkg.nodes) {
                val nodeJson = nodeToJson(node)
                if (!node.imageUri.isNullOrBlank()) {
                    val imageKey = tryEncodeImageToBase64(node.imageUri)
                    if (imageKey != null) {
                        nodeJson.put("imageFileName", imageKey.first)
                        imagesMap.put(imageKey.first, imageKey.second)
                    }
                }
                nodesArray.put(nodeJson)
            }
            pkgJson.put("nodes", nodesArray)
            mapsArray.put(pkgJson)
        }

        rootJson.put("maps", mapsArray)
        rootJson.put("images", imagesMap)

        return@withContext rootJson.toString(2)
    }

    private fun mapToJson(map: MindMapEntity): JSONObject {
        return JSONObject().apply {
            put("id", map.id)
            put("title", map.title)
            put("description", map.description)
            put("themeColorHex", map.themeColorHex)
            put("rootNodeId", map.rootNodeId)
            put("createdAt", map.createdAt)
            put("updatedAt", map.updatedAt)
        }
    }

    private fun nodeToJson(node: MindNodeEntity): JSONObject {
        return JSONObject().apply {
            put("id", node.id)
            put("parentId", node.parentId ?: JSONObject.NULL)
            put("syncMasterId", node.syncMasterId ?: JSONObject.NULL)
            put("title", node.title)
            put("notes", node.notes)
            put("checklistJson", node.checklistJson)
            put("linkUrl", node.linkUrl)
            put("colorHex", node.colorHex)
            put("iconName", node.iconName)
            if (node.progress != null) put("progress", node.progress)
            if (node.impact != null) put("impact", node.impact)
            put("orderIndex", node.orderIndex)
            put("createdAt", node.createdAt)
            put("updatedAt", node.updatedAt)
        }
    }

    private fun tryEncodeImageToBase64(imagePathOrUri: String): Pair<String, String>? {
        return try {
            val file = File(imagePathOrUri)
            if (file.exists() && file.isFile) {
                val bytes = file.readBytes()
                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                val fileName = file.name.ifBlank { "img_${UUID.randomUUID()}.jpg" }
                Pair(fileName, base64)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ==========================================
    // PREVIEW & VALIDATION
    // ==========================================

    /**
     * Inspects a JSON backup string and extracts a human-readable preview before importing.
     */
    fun parseBackupPreview(jsonStr: String): Result<BackupPreview> {
        return try {
            val root = JSONObject(jsonStr.trim())
            val format = root.optString("format", "")
            val backupType = root.optString("backupType", "single_map")
            val exportedAt = root.optLong("exportedAt", System.currentTimeMillis())

            val imagesJson = root.optJSONObject("images")
            val imagesCount = imagesJson?.length() ?: 0

            if (backupType == "full_backup" || root.has("maps")) {
                val mapsArray = root.optJSONArray("maps") ?: JSONArray()
                val titles = mutableListOf<String>()
                var totalNodes = 0
                var firstTheme = "#6366F1"

                for (i in 0 until mapsArray.length()) {
                    val pkgJson = mapsArray.getJSONObject(i)
                    val mapJson = pkgJson.optJSONObject("map")
                    val title = mapJson?.optString("title", "Unnamed Map") ?: "Unnamed Map"
                    if (i == 0 && mapJson != null) {
                        firstTheme = mapJson.optString("themeColorHex", "#6366F1")
                    }
                    titles.add(title)
                    val nodesArray = pkgJson.optJSONArray("nodes")
                    totalNodes += (nodesArray?.length() ?: 0)
                }

                Result.success(
                    BackupPreview(
                        isFullBackup = true,
                        mapsCount = titles.size,
                        totalNodesCount = totalNodes,
                        imagesCount = imagesCount,
                        exportedAt = exportedAt,
                        mapTitles = titles,
                        primaryMapTitle = titles.firstOrNull() ?: "Full Backup",
                        primaryMapThemeColor = firstTheme,
                        rawJson = jsonStr
                    )
                )
            } else {
                // Single map format
                val mapJson = root.optJSONObject("map") ?: root
                val title = mapJson.optString("title", "Imported Map")
                val themeColor = mapJson.optString("themeColorHex", "#6366F1")
                val nodesArray = root.optJSONArray("nodes") ?: JSONArray()

                Result.success(
                    BackupPreview(
                        isFullBackup = false,
                        mapsCount = 1,
                        totalNodesCount = nodesArray.length(),
                        imagesCount = imagesCount,
                        exportedAt = exportedAt,
                        mapTitles = listOf(title),
                        primaryMapTitle = title,
                        primaryMapThemeColor = themeColor,
                        rawJson = jsonStr
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Invalid OrbitMind map format: ${e.localizedMessage}"))
        }
    }

    // ==========================================
    // IMPORT IMPLEMENTATION
    // ==========================================

    /**
     * Executes the actual import of a single map or full backup, restoring hierarchy,
     * recreating node IDs, preserving sync twin relationships, and restoring photos.
     */
    suspend fun executeImport(
        context: Context,
        repository: MindMapRepository,
        jsonStr: String,
        importAsNewCopy: Boolean = true
    ): Result<ImportResult> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonStr.trim())
            val backupType = root.optString("backupType", "single_map")
            val isFull = backupType == "full_backup" || root.has("maps")

            // 1. Extract and store any embedded Base64 images into internal storage
            val imagesJson = root.optJSONObject("images")
            val savedImageUriMap = mutableMapOf<String, String>() // imageFileName -> localFilePath
            if (imagesJson != null) {
                val imagesDir = File(context.filesDir, "node_images")
                if (!imagesDir.exists()) imagesDir.mkdirs()

                val keys = imagesJson.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val base64 = imagesJson.optString(key, "")
                    if (base64.isNotBlank()) {
                        try {
                            val decodedBytes = Base64.decode(base64, Base64.DEFAULT)
                            val targetFile = File(imagesDir, "imported_${UUID.randomUUID()}_$key")
                            FileOutputStream(targetFile).use { it.write(decodedBytes) }
                            savedImageUriMap[key] = targetFile.absolutePath
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }

            val importedMapIds = mutableListOf<Long>()
            var totalImportedNodes = 0

            if (isFull) {
                val mapsArray = root.optJSONArray("maps") ?: JSONArray()
                for (i in 0 until mapsArray.length()) {
                    val pkgJson = mapsArray.getJSONObject(i)
                    val mapJson = pkgJson.getJSONObject("map")
                    val nodesArray = pkgJson.optJSONArray("nodes") ?: JSONArray()

                    val (mapId, nodeCount) = importSingleMapInternal(
                        repository = repository,
                        mapJson = mapJson,
                        nodesArray = nodesArray,
                        savedImageUriMap = savedImageUriMap,
                        importAsNewCopy = importAsNewCopy
                    )
                    importedMapIds.add(mapId)
                    totalImportedNodes += nodeCount
                }
            } else {
                val mapJson = root.optJSONObject("map") ?: root
                val nodesArray = root.optJSONArray("nodes") ?: JSONArray()

                val (mapId, nodeCount) = importSingleMapInternal(
                    repository = repository,
                    mapJson = mapJson,
                    nodesArray = nodesArray,
                    savedImageUriMap = savedImageUriMap,
                    importAsNewCopy = importAsNewCopy
                )
                importedMapIds.add(mapId)
                totalImportedNodes += nodeCount
            }

            Result.success(
                ImportResult(
                    importedMapIds = importedMapIds,
                    totalMaps = importedMapIds.size,
                    totalNodes = totalImportedNodes,
                    primaryMapId = importedMapIds.firstOrNull()
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private suspend fun importSingleMapInternal(
        repository: MindMapRepository,
        mapJson: JSONObject,
        nodesArray: JSONArray,
        savedImageUriMap: Map<String, String>,
        importAsNewCopy: Boolean
    ): Pair<Long, Int> {
        val now = System.currentTimeMillis()
        val originalTitle = mapJson.optString("title", "Imported Universe")
        val finalTitle = if (importAsNewCopy) originalTitle else originalTitle
        val description = mapJson.optString("description", "")
        val themeColor = mapJson.optString("themeColorHex", "#6366F1")
        val oldRootNodeId = mapJson.optString("rootNodeId", "")

        // Read all nodes from JSON
        val rawNodes = mutableListOf<RawImportNode>()
        for (i in 0 until nodesArray.length()) {
            val nodeObj = nodesArray.getJSONObject(i)
            val id = nodeObj.optString("id", UUID.randomUUID().toString())
            val parentId = if (nodeObj.isNull("parentId")) null else nodeObj.optString("parentId").ifBlank { null }
            val syncMasterId = if (nodeObj.isNull("syncMasterId")) null else nodeObj.optString("syncMasterId").ifBlank { null }
            val title = nodeObj.optString("title", "Idea")
            val notes = nodeObj.optString("notes", "")
            val checklistJson = nodeObj.optString("checklistJson", "")
            val linkUrl = nodeObj.optString("linkUrl", "")
            val colorHex = nodeObj.optString("colorHex", "#3B82F6")
            val iconName = nodeObj.optString("iconName", "lightbulb")
            val orderIndex = nodeObj.optInt("orderIndex", i)
            val progress = if (nodeObj.has("progress") && !nodeObj.isNull("progress")) nodeObj.optInt("progress") else null
            val impact = if (nodeObj.has("impact") && !nodeObj.isNull("impact")) nodeObj.optInt("impact") else null
            val imageFileName = nodeObj.optString("imageFileName", "")
            val resolvedImageUri = if (imageFileName.isNotBlank()) savedImageUriMap[imageFileName] else null

            rawNodes.add(
                RawImportNode(
                    id = id,
                    parentId = parentId,
                    syncMasterId = syncMasterId,
                    title = title,
                    notes = notes,
                    checklistJson = checklistJson,
                    linkUrl = linkUrl,
                    colorHex = colorHex,
                    iconName = iconName,
                    resolvedImageUri = resolvedImageUri,
                    progress = progress,
                    impact = impact,
                    orderIndex = orderIndex
                )
            )
        }

        // Check if we should replace an existing local map in-place
        if (!importAsNewCopy) {
            val origMapId = mapJson.optLong("id", -1L)
            val existingMaps = repository.getAllMapsListSync()
            val matchingMap = if (origMapId > 0) {
                existingMaps.firstOrNull { it.id == origMapId }
                    ?: existingMaps.firstOrNull { it.title.trim().equals(originalTitle.trim(), ignoreCase = true) }
            } else {
                existingMaps.firstOrNull { it.title.trim().equals(originalTitle.trim(), ignoreCase = true) }
            }

            if (matchingMap != null) {
                val targetMapId = matchingMap.id
                val effectiveRootNodeId = oldRootNodeId.ifBlank {
                    rawNodes.firstOrNull { it.parentId == null }?.id ?: UUID.randomUUID().toString()
                }

                val updatedMap = matchingMap.copy(
                    title = originalTitle,
                    description = description,
                    themeColorHex = themeColor,
                    rootNodeId = effectiveRootNodeId,
                    updatedAt = now
                )

                val nodesToInsert = if (rawNodes.isEmpty()) {
                    listOf(
                        MindNodeEntity(
                            id = effectiveRootNodeId,
                            mapId = targetMapId,
                            parentId = null,
                            title = originalTitle,
                            notes = description,
                            colorHex = themeColor,
                            iconName = "rocket_launch",
                            orderIndex = 0,
                            createdAt = now,
                            updatedAt = now
                        )
                    )
                } else {
                    rawNodes.map { raw ->
                        MindNodeEntity(
                            id = raw.id,
                            mapId = targetMapId,
                            parentId = raw.parentId,
                            syncMasterId = raw.syncMasterId,
                            title = raw.title,
                            notes = raw.notes,
                            checklistJson = raw.checklistJson,
                            linkUrl = raw.linkUrl,
                            colorHex = raw.colorHex,
                            iconName = raw.iconName,
                            imageUri = raw.resolvedImageUri,
                            progress = raw.progress,
                            impact = raw.impact,
                            orderIndex = raw.orderIndex,
                            createdAt = now,
                            updatedAt = now
                        )
                    }
                }

                repository.replaceMapWithNodes(targetMapId, updatedMap, nodesToInsert)
                return Pair(targetMapId, nodesToInsert.size)
            }
        }

        // Map old node IDs to newly generated UUIDs to avoid any primary key collision when importing as new copy
        val idMapping = mutableMapOf<String, String>()
        for (n in rawNodes) {
            idMapping[n.id] = UUID.randomUUID().toString()
        }

        // Map syncMasterIds cleanly: if twin group shares a master, map them consistently
        val syncKeyMapping = mutableMapOf<String, String>()
        for (n in rawNodes) {
            val sm = n.syncMasterId
            if (sm != null && !syncKeyMapping.containsKey(sm)) {
                syncKeyMapping[sm] = idMapping[sm] ?: UUID.randomUUID().toString()
            }
        }

        val newRootNodeId = idMapping[oldRootNodeId] ?: (rawNodes.firstOrNull { it.parentId == null }?.id?.let { idMapping[it] } ?: UUID.randomUUID().toString())

        // Insert new MindMap entity
        val mapEntity = MindMapEntity(
            title = finalTitle,
            description = description,
            themeColorHex = themeColor,
            rootNodeId = newRootNodeId,
            createdAt = now,
            updatedAt = now
        )

        // Using direct repository helper to insert Map & Nodes
        val mapId = repository.insertCustomMapAndNodes(
            map = mapEntity,
            nodesProvider = { generatedMapId ->
                if (rawNodes.isEmpty()) {
                    listOf(
                        MindNodeEntity(
                            id = newRootNodeId,
                            mapId = generatedMapId,
                            parentId = null,
                            title = finalTitle,
                            notes = description,
                            colorHex = themeColor,
                            iconName = "rocket_launch",
                            orderIndex = 0,
                            createdAt = now,
                            updatedAt = now
                        )
                    )
                } else {
                    rawNodes.map { raw ->
                        val newId = idMapping[raw.id] ?: UUID.randomUUID().toString()
                        val newParentId = if (raw.parentId != null) idMapping[raw.parentId] else null
                        val newSyncId = if (raw.syncMasterId != null) syncKeyMapping[raw.syncMasterId] else null

                        MindNodeEntity(
                            id = newId,
                            mapId = generatedMapId,
                            parentId = newParentId,
                            syncMasterId = newSyncId,
                            title = raw.title,
                            notes = raw.notes,
                            checklistJson = raw.checklistJson,
                            linkUrl = raw.linkUrl,
                            colorHex = raw.colorHex,
                            iconName = raw.iconName,
                            imageUri = raw.resolvedImageUri,
                            progress = raw.progress,
                            impact = raw.impact,
                            orderIndex = raw.orderIndex,
                            createdAt = now,
                            updatedAt = now
                        )
                    }
                }
            }
        )

        return Pair(mapId, rawNodes.size.coerceAtLeast(1))
    }

    private data class RawImportNode(
        val id: String,
        val parentId: String?,
        val syncMasterId: String?,
        val title: String,
        val notes: String,
        val checklistJson: String,
        val linkUrl: String,
        val colorHex: String,
        val iconName: String,
        val resolvedImageUri: String?,
        val progress: Int? = null,
        val impact: Int? = null,
        val orderIndex: Int
    )

    // ==========================================
    // TEXT & MARKDOWN EXPORT
    // ==========================================

    fun exportToPlainText(
        map: MindMapEntity,
        nodes: List<MindNodeEntity>,
        rootNodeIdOverride: String? = null
    ): String {
        val rootNode = if (!rootNodeIdOverride.isNullOrBlank()) {
            nodes.firstOrNull { it.id == rootNodeIdOverride }
        } else {
            nodes.firstOrNull { it.parentId == null }
        } ?: return ""

        val effectiveTitle = if (!rootNodeIdOverride.isNullOrBlank()) rootNode.title else map.title

        val sb = StringBuilder()
        sb.append(effectiveTitle.uppercase()).append("\n")
        if (map.description.isNotBlank() && rootNodeIdOverride.isNullOrBlank()) {
            sb.append("Description: ").append(map.description).append("\n")
        }
        sb.append("------------------------------------\n\n")

        fun printPlain(node: MindNodeEntity, level: Int) {
            val indent = "   ".repeat(level)
            val bullet = if (level == 0) "● " else "○ "
            sb.append(indent).append(bullet).append(node.title).append("\n")

            if (node.notes.isNotBlank()) {
                val noteIndent = "   ".repeat(level + 1)
                node.notes.lines().forEach { line ->
                    if (line.isNotBlank()) sb.append(noteIndent).append("| ").append(line).append("\n")
                }
            }

            if (node.checklist.isNotEmpty()) {
                val checkIndent = "   ".repeat(level + 1)
                node.checklist.forEach { item ->
                    val mark = if (item.isDone) "[✓]" else "[ ]"
                    sb.append(checkIndent).append(mark).append(" ").append(item.text).append("\n")
                }
            }

            if (node.linkUrl.isNotBlank()) {
                val linkIndent = "   ".repeat(level + 1)
                sb.append(linkIndent).append("→ ").append(node.linkUrl).append("\n")
            }

            val children = nodes.filter { it.parentId == node.id }.sortedBy { it.orderIndex }
            for (child in children) {
                printPlain(child, level + 1)
            }
        }

        // If it's a specific sub-branch, start by printing the branch node itself at root
        if (!rootNodeIdOverride.isNullOrBlank()) {
            printPlain(rootNode, 0)
        } else {
            val directChildren = nodes.filter { it.parentId == rootNode.id }.sortedBy { it.orderIndex }
            for (child in directChildren) {
                printPlain(child, 0)
            }
        }

        return sb.toString()
    }

    // ==========================================
    // ANDROID FILE SHARING & DISK I/O HELPERS
    // ==========================================

    /**
     * Writes backup content to cache file and triggers Android system share chooser.
     */
    fun shareBackupFile(
        context: Context,
        suggestedFileName: String,
        content: String,
        mimeType: String = "application/json"
    ) {
        try {
            val exportsDir = File(context.cacheDir, "exports")
            if (!exportsDir.exists()) exportsDir.mkdirs()

            val sanitizedName = suggestedFileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val targetFile = File(exportsDir, sanitizedName)
            FileOutputStream(targetFile).use { it.write(content.toByteArray(Charsets.UTF_8)) }

            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                targetFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, suggestedFileName)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share Map / Backup")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error sharing file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Writes content to a target document Uri obtained via CreateDocument activity contract.
     */
    fun writeContentToDocumentUri(context: Context, targetUri: Uri, content: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(targetUri)?.use { out ->
                out.write(content.toByteArray(Charsets.UTF_8))
                out.flush()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Reads text content from a source document Uri obtained via OpenDocument activity contract.
     */
    fun readContentFromDocumentUri(context: Context, sourceUri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                input.bufferedReader(Charsets.UTF_8).use { it.readText() }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Copies string content to the system clipboard.
     */
    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    /**
     * Generates a safe and beautiful default filename for export.
     */
    fun generateExportFileName(mapTitle: String, isFullBackup: Boolean = false, extension: String = FILE_EXTENSION): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        val cleanTitle = if (isFullBackup) "Madar_FullBackup" else "Madar_" + mapTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(25)
        return "${cleanTitle}_$dateFormat$extension"
    }
}
