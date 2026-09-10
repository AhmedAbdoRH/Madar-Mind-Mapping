package com.example.data.repository

import com.example.data.db.MindMapDao
import com.example.data.db.MindNodeDao
import com.example.data.model.ChecklistItem
import com.example.data.model.MindMapEntity
import com.example.data.model.MindNodeEntity
import com.example.ui.util.OrbitMindBackupManager
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class MindMapRepository(
    private val mindMapDao: MindMapDao,
    private val mindNodeDao: MindNodeDao
) {
    val allMaps: Flow<List<MindMapEntity>> = mindMapDao.getAllMaps()
    val nodeCountsPerMap: Flow<List<com.example.data.db.MapNodeCount>> = mindNodeDao.getNodeCountsPerMap()
    val totalNodesCount: Flow<Int> = mindNodeDao.getTotalNodesCount()

    fun getAllNodesForMap(mapId: Long): Flow<List<MindNodeEntity>> {
        return mindNodeDao.getAllNodesForMap(mapId)
    }

    fun getMapById(mapId: Long): Flow<MindMapEntity?> {
        return mindMapDao.getMapById(mapId)
    }

    suspend fun getMapByIdSync(mapId: Long): MindMapEntity? {
        return mindMapDao.getMapByIdSync(mapId)
    }

    suspend fun getAllNodesForMapSync(mapId: Long): List<MindNodeEntity> {
        return mindNodeDao.getAllNodesForMapSync(mapId)
    }

    suspend fun createMap(
        title: String,
        description: String = "",
        themeColorHex: String = "#6366F1",
        rootIcon: String = "rocket_launch"
    ): Long {
        val now = System.currentTimeMillis()
        val rootNodeId = UUID.randomUUID().toString()

        val map = MindMapEntity(
            title = title,
            description = description,
            themeColorHex = themeColorHex,
            rootNodeId = rootNodeId,
            createdAt = now,
            updatedAt = now
        )
        val mapId = mindMapDao.insertMap(map)

        // Create the root central node
        val rootNode = MindNodeEntity(
            id = rootNodeId,
            mapId = mapId,
            parentId = null,
            syncMasterId = null,
            title = title,
            notes = description,
            colorHex = themeColorHex,
            iconName = rootIcon,
            orderIndex = 0,
            createdAt = now,
            updatedAt = now
        )
        mindNodeDao.insertNode(rootNode)

        return mapId
    }

    suspend fun updateMap(map: MindMapEntity) {
        mindMapDao.updateMap(map.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun getAllMapsListSync(): List<MindMapEntity> {
        return mindMapDao.getAllMapsSync()
    }

    suspend fun replaceMapWithNodes(
        mapId: Long,
        updatedMap: MindMapEntity,
        nodes: List<MindNodeEntity>
    ) {
        mindMapDao.updateMap(updatedMap)
        mindNodeDao.deleteAllNodesForMap(mapId)
        if (nodes.isNotEmpty()) {
            mindNodeDao.insertNodes(nodes)
        }
    }

    suspend fun insertCustomMapAndNodes(
        map: MindMapEntity,
        nodesProvider: (Long) -> List<MindNodeEntity>
    ): Long {
        val mapId = mindMapDao.insertMap(map)
        val nodes = nodesProvider(mapId)
        if (nodes.isNotEmpty()) {
            mindNodeDao.insertNodes(nodes)
        }
        return mapId
    }

    suspend fun getAllMapsWithNodesSync(): List<OrbitMindBackupManager.MapExportPackage> {
        val maps = mindMapDao.getAllMapsSync()
        return maps.map { map ->
            val nodes = mindNodeDao.getAllNodesForMapSync(map.id)
            OrbitMindBackupManager.MapExportPackage(map, nodes)
        }
    }

    suspend fun deleteMap(mapId: Long) {
        mindNodeDao.deleteAllNodesForMap(mapId)
        mindMapDao.deleteMap(mapId)
    }

    suspend fun duplicateMap(mapId: Long): Long {
        val originalMap = mindMapDao.getMapByIdSync(mapId) ?: return 0L
        val originalNodes = mindNodeDao.getAllNodesForMapSync(mapId)
        val now = System.currentTimeMillis()

        val idMapping = mutableMapOf<String, String>()
        for (node in originalNodes) {
            idMapping[node.id] = UUID.randomUUID().toString()
        }

        val newRootNodeId = idMapping[originalMap.rootNodeId] ?: UUID.randomUUID().toString()
        val newMap = MindMapEntity(
            title = "${originalMap.title} (Copy)",
            description = originalMap.description,
            themeColorHex = originalMap.themeColorHex,
            rootNodeId = newRootNodeId,
            createdAt = now,
            updatedAt = now
        )
        val newMapId = mindMapDao.insertMap(newMap)

        val newNodes = originalNodes.map { node ->
            val newId = idMapping[node.id] ?: UUID.randomUUID().toString()
            val newParentId = if (node.parentId != null) idMapping[node.parentId] else null
            node.copy(
                id = newId,
                mapId = newMapId,
                parentId = newParentId,
                createdAt = now,
                updatedAt = now
            )
        }
        mindNodeDao.insertNodes(newNodes)
        return newMapId
    }

    suspend fun addNode(
        mapId: Long,
        parentId: String?,
        title: String,
        colorHex: String,
        iconName: String = "lightbulb",
        notes: String = "",
        checklist: List<ChecklistItem> = emptyList(),
        linkUrl: String = "",
        imageUri: String? = null,
        syncMasterId: String? = null,
        progress: Int? = null,
        impact: Int? = null,
        dueDate: Long? = null
    ): MindNodeEntity {
        val now = System.currentTimeMillis()
        val allMapNodes = mindNodeDao.getAllNodesForMapSync(mapId)
        val existingSiblings = allMapNodes.filter { it.parentId == parentId }
        val order = existingSiblings.size

        val parentNode = if (parentId != null) allMapNodes.firstOrNull { it.id == parentId } else null
        val isParentSynced = parentNode != null && parentNode.isSyncTwin

        // If parent is a sync twin, this new child will also be linked to twins across all twin parents!
        val assignedSyncKey = syncMasterId ?: if (isParentSynced) UUID.randomUUID().toString() else null

        val primaryNode = MindNodeEntity(
            id = UUID.randomUUID().toString(),
            mapId = mapId,
            parentId = parentId,
            syncMasterId = assignedSyncKey,
            title = title,
            notes = notes,
            checklistJson = ChecklistItem.listToJson(checklist),
            linkUrl = linkUrl,
            colorHex = colorHex,
            iconName = iconName,
            imageUri = imageUri,
            progress = progress,
            impact = impact,
            dueDate = dueDate,
            orderIndex = order,
            createdAt = now,
            updatedAt = now
        )

        val nodesToInsert = mutableListOf(primaryNode)

        // If parent is a sync twin, automatically add this child to all sibling twin parents!
        if (parentNode != null && isParentSynced && assignedSyncKey != null) {
            val parentSyncKey = parentNode.syncMasterId ?: parentNode.id
            val twinParents = allMapNodes.filter { candidate ->
                candidate.id != parentNode.id && (
                    candidate.syncMasterId == parentSyncKey ||
                    candidate.id == parentSyncKey ||
                    (parentNode.syncMasterId != null && candidate.syncMasterId == parentNode.syncMasterId)
                )
            }

            for (twinParent in twinParents) {
                val twinSiblings = allMapNodes.filter { it.parentId == twinParent.id }
                val twinChild = MindNodeEntity(
                    id = UUID.randomUUID().toString(),
                    mapId = mapId,
                    parentId = twinParent.id,
                    syncMasterId = assignedSyncKey,
                    title = title,
                    notes = notes,
                    checklistJson = ChecklistItem.listToJson(checklist),
                    linkUrl = linkUrl,
                    colorHex = colorHex,
                    iconName = iconName,
                    imageUri = imageUri,
                    progress = progress,
                    impact = impact,
                    dueDate = dueDate,
                    orderIndex = twinSiblings.size,
                    createdAt = now,
                    updatedAt = now
                )
                nodesToInsert.add(twinChild)
            }
        }

        mindNodeDao.insertNodes(nodesToInsert)

        // Touch map updated time
        val map = mindMapDao.getMapByIdSync(mapId)
        if (map != null) {
            mindMapDao.updateMap(map.copy(updatedAt = now))
        }

        return primaryNode
    }

    suspend fun updateNode(node: MindNodeEntity) {
        val now = System.currentTimeMillis()
        mindNodeDao.updateNode(node.copy(updatedAt = now))

        // Automatic synchronization for linked twin nodes across all orbits
        val syncGroupId = node.syncMasterId ?: node.id
        val allMapNodes = mindNodeDao.getAllNodesForMapSync(node.mapId)
        val twinNodes = allMapNodes.filter { candidate ->
            candidate.id != node.id && (
                candidate.syncMasterId == syncGroupId ||
                candidate.id == syncGroupId ||
                (node.syncMasterId != null && candidate.syncMasterId == node.syncMasterId)
            )
        }

        if (twinNodes.isNotEmpty()) {
            val updatedTwins = twinNodes.map { twin ->
                twin.copy(
                    title = node.title,
                    notes = node.notes,
                    checklistJson = node.checklistJson,
                    linkUrl = node.linkUrl,
                    colorHex = node.colorHex,
                    iconName = node.iconName,
                    imageUri = node.imageUri,
                    progress = node.progress,
                    impact = node.impact,
                    dueDate = node.dueDate,
                    updatedAt = now
                )
            }
            mindNodeDao.insertNodes(updatedTwins)
        }

        val map = mindMapDao.getMapByIdSync(node.mapId)
        if (map != null) {
            // If this is root node, also update map title/theme if root changed
            if (node.parentId == null) {
                mindMapDao.updateMap(map.copy(title = node.title, themeColorHex = node.colorHex, updatedAt = now))
            } else {
                mindMapDao.updateMap(map.copy(updatedAt = now))
            }
        }
    }

    /**
     * Applies a color or harmonic palette to nodes according to the selected ColorApplyScope.
     */
    suspend fun applyColorWithScope(
        nodeId: String,
        colorHex: String,
        scope: com.example.ui.util.ColorApplyScope,
        mapId: Long
    ) {
        val allMapNodes = mindNodeDao.getAllNodesForMapSync(mapId)
        val targetNode = allMapNodes.firstOrNull { it.id == nodeId } ?: return
        val now = System.currentTimeMillis()

        val updatedNodes = mutableListOf<MindNodeEntity>()

        when (scope) {
            com.example.ui.util.ColorApplyScope.THIS_NODE_ONLY -> {
                updatedNodes.add(targetNode.copy(colorHex = colorHex, updatedAt = now))
            }
            com.example.ui.util.ColorApplyScope.DIRECT_CHILDREN -> {
                val directChildren = allMapNodes.filter { it.parentId == nodeId }
                for (child in directChildren) {
                    updatedNodes.add(child.copy(colorHex = colorHex, updatedAt = now))
                }
            }
            com.example.ui.util.ColorApplyScope.ALL_DESCENDANTS -> {
                fun collectChildren(parentId: String) {
                    val children = allMapNodes.filter { it.parentId == parentId }
                    for (child in children) {
                        updatedNodes.add(child.copy(colorHex = colorHex, updatedAt = now))
                        collectChildren(child.id)
                    }
                }
                collectChildren(nodeId)
            }
            com.example.ui.util.ColorApplyScope.SIBLINGS -> {
                val siblings = allMapNodes.filter { it.parentId == targetNode.parentId && it.id != nodeId }
                for (sib in siblings) {
                    updatedNodes.add(sib.copy(colorHex = colorHex, updatedAt = now))
                }
            }
            com.example.ui.util.ColorApplyScope.ENTIRE_BRANCH -> {
                updatedNodes.add(targetNode.copy(colorHex = colorHex, updatedAt = now))
                fun collectBranch(parentId: String) {
                    val children = allMapNodes.filter { it.parentId == parentId }
                    for (child in children) {
                        updatedNodes.add(child.copy(colorHex = colorHex, updatedAt = now))
                        collectBranch(child.id)
                    }
                }
                collectBranch(nodeId)
            }
            com.example.ui.util.ColorApplyScope.NODE_AND_SIBLINGS -> {
                val orbitGroup = allMapNodes.filter { it.parentId == targetNode.parentId }
                for (node in orbitGroup) {
                    updatedNodes.add(node.copy(colorHex = colorHex, updatedAt = now))
                }
            }
            com.example.ui.util.ColorApplyScope.HARMONIC_GRADIENT -> {
                updatedNodes.add(targetNode.copy(colorHex = colorHex, updatedAt = now))
                fun applyHarmonics(parentId: String, level: Int) {
                    val children = allMapNodes.filter { it.parentId == parentId }
                    val levelHex = com.example.ui.util.OrbitColors.getHarmonicToneForLevel(colorHex, level)
                    for (child in children) {
                        updatedNodes.add(child.copy(colorHex = levelHex, updatedAt = now))
                        applyHarmonics(child.id, level + 1)
                    }
                }
                applyHarmonics(nodeId, 1)
            }
            com.example.ui.util.ColorApplyScope.SET_AS_PRIMARY_MAP_COLOR -> {
                updatedNodes.add(targetNode.copy(colorHex = colorHex, updatedAt = now))
                val root = allMapNodes.firstOrNull { it.parentId == null }
                if (root != null && root.id != nodeId) {
                    updatedNodes.add(root.copy(colorHex = colorHex, updatedAt = now))
                }
            }
        }

        if (updatedNodes.isNotEmpty()) {
            // Also update twin nodes if any node in updatedNodes is a synced twin
            val allUpdatedWithTwins = mutableListOf<MindNodeEntity>()
            allUpdatedWithTwins.addAll(updatedNodes)

            for (node in updatedNodes) {
                if (node.isSyncTwin) {
                    val syncKey = node.syncMasterId ?: node.id
                    val twins = allMapNodes.filter { candidate ->
                        candidate.id != node.id && (
                            candidate.syncMasterId == syncKey ||
                            candidate.id == syncKey ||
                            (node.syncMasterId != null && candidate.syncMasterId == node.syncMasterId)
                        )
                    }
                    for (twin in twins) {
                        if (!allUpdatedWithTwins.any { it.id == twin.id }) {
                            allUpdatedWithTwins.add(twin.copy(colorHex = node.colorHex, updatedAt = now))
                        }
                    }
                }
            }

            mindNodeDao.insertNodes(allUpdatedWithTwins)

            // If root node was updated, update map theme color
            if (updatedNodes.any { it.parentId == null }) {
                val rootUpdated = updatedNodes.first { it.parentId == null }
                val map = mindMapDao.getMapByIdSync(mapId)
                if (map != null) {
                    mindMapDao.updateMap(map.copy(themeColorHex = rootUpdated.colorHex, updatedAt = now))
                }
            } else {
                val map = mindMapDao.getMapByIdSync(mapId)
                if (map != null) {
                    mindMapDao.updateMap(map.copy(updatedAt = now))
                }
            }
        }
    }

    /**
     * Reorders sibling nodes on an orbit according to the list of node IDs.
     */
    suspend fun reorderSiblingNodes(orderedNodeIds: List<String>, mapId: Long) {
        val allMapNodes = mindNodeDao.getAllNodesForMapSync(mapId)
        val now = System.currentTimeMillis()
        val updatedNodes = orderedNodeIds.mapIndexedNotNull { index, id ->
            val node = allMapNodes.firstOrNull { it.id == id }
            node?.copy(orderIndex = index, updatedAt = now)
        }
        if (updatedNodes.isNotEmpty()) {
            mindNodeDao.insertNodes(updatedNodes)
            val map = mindMapDao.getMapByIdSync(mapId)
            if (map != null) {
                mindMapDao.updateMap(map.copy(updatedAt = now))
            }
        }
    }

    /**
     * Moves a node and its sub-orbit hierarchy directly to a new parent orbit.
     */
    suspend fun moveNode(nodeId: String, newParentId: String?, mapId: Long) {
        val existing = mindNodeDao.getNodeByIdSync(nodeId) ?: return
        if (existing.id == newParentId) return // Cannot be parent of itself

        val siblings = mindNodeDao.getAllNodesForMapSync(mapId).filter { it.parentId == newParentId }
        val now = System.currentTimeMillis()
        val updated = existing.copy(
            parentId = newParentId,
            orderIndex = siblings.size,
            updatedAt = now
        )
        mindNodeDao.updateNode(updated)

        val map = mindMapDao.getMapByIdSync(mapId)
        if (map != null) {
            mindMapDao.updateMap(map.copy(updatedAt = now))
        }
    }

    /**
     * Clones a node and all of its nested children subtrees under a new parent orbit.
     */
    suspend fun cloneNodeSubtree(sourceNodeId: String, newParentId: String?, mapId: Long): MindNodeEntity? {
        val allNodes = mindNodeDao.getAllNodesForMapSync(mapId)
        val sourceNode = allNodes.firstOrNull { it.id == sourceNodeId } ?: return null
        val now = System.currentTimeMillis()

        val siblings = allNodes.filter { it.parentId == newParentId }
        val newRootCloneId = UUID.randomUUID().toString()

        val rootClone = sourceNode.copy(
            id = newRootCloneId,
            parentId = newParentId,
            title = "${sourceNode.title} (Copy)",
            orderIndex = siblings.size,
            syncMasterId = null, // Independent clone
            createdAt = now,
            updatedAt = now
        )

        val nodesToInsert = mutableListOf(rootClone)
        val idMapping = mutableMapOf(sourceNodeId to newRootCloneId)

        // Recursively clone descendants
        fun cloneChildren(originalParentId: String, newAssignedParentId: String) {
            val children = allNodes.filter { it.parentId == originalParentId }
            for (child in children) {
                val newChildId = UUID.randomUUID().toString()
                idMapping[child.id] = newChildId
                val childClone = child.copy(
                    id = newChildId,
                    parentId = newAssignedParentId,
                    syncMasterId = null,
                    createdAt = now,
                    updatedAt = now
                )
                nodesToInsert.add(childClone)
                cloneChildren(child.id, newChildId)
            }
        }

        cloneChildren(sourceNodeId, newRootCloneId)
        mindNodeDao.insertNodes(nodesToInsert)

        val map = mindMapDao.getMapByIdSync(mapId)
        if (map != null) {
            mindMapDao.updateMap(map.copy(updatedAt = now))
        }

        return rootClone
    }

    /**
     * Creates a live synchronized twin of the node in the target parent orbit.
     * Both nodes and their entire internal descendant hierarchies share live synchronization
     * across title, notes, checklist, icon, colors, and child node additions/deletions.
     */
    suspend fun createSyncTwinNode(sourceNodeId: String, targetParentId: String?, mapId: Long): MindNodeEntity? {
        val allNodes = mindNodeDao.getAllNodesForMapSync(mapId)
        val sourceNode = allNodes.firstOrNull { it.id == sourceNodeId } ?: return null
        val now = System.currentTimeMillis()

        // Establish the shared sync group key for the root twin
        val rootSyncKey = sourceNode.syncMasterId ?: sourceNode.id

        val nodesToUpdate = mutableListOf<MindNodeEntity>()
        val nodesToInsert = mutableListOf<MindNodeEntity>()

        // Ensure source node is marked with the sync key
        if (sourceNode.syncMasterId == null) {
            nodesToUpdate.add(sourceNode.copy(syncMasterId = rootSyncKey, updatedAt = now))
        }

        val siblings = allNodes.filter { it.parentId == targetParentId }
        val twinRootId = UUID.randomUUID().toString()

        val twinRootNode = sourceNode.copy(
            id = twinRootId,
            parentId = targetParentId,
            syncMasterId = rootSyncKey,
            orderIndex = siblings.size,
            createdAt = now,
            updatedAt = now
        )
        nodesToInsert.add(twinRootNode)

        // Recursively clone and twin-link all internal descendants
        fun createTwinSubtree(originalParentId: String, newParentTwinId: String) {
            val children = allNodes.filter { it.parentId == originalParentId }
            for (child in children) {
                val childSyncKey = child.syncMasterId ?: child.id
                if (child.syncMasterId == null) {
                    nodesToUpdate.add(child.copy(syncMasterId = childSyncKey, updatedAt = now))
                }

                val childTwinId = UUID.randomUUID().toString()
                val twinChild = child.copy(
                    id = childTwinId,
                    parentId = newParentTwinId,
                    syncMasterId = childSyncKey,
                    createdAt = now,
                    updatedAt = now
                )
                nodesToInsert.add(twinChild)

                // Recurse deeper into sub-orbits
                createTwinSubtree(child.id, childTwinId)
            }
        }

        createTwinSubtree(sourceNodeId, twinRootId)

        if (nodesToUpdate.isNotEmpty()) {
            mindNodeDao.insertNodes(nodesToUpdate)
        }
        if (nodesToInsert.isNotEmpty()) {
            mindNodeDao.insertNodes(nodesToInsert)
        }

        val map = mindMapDao.getMapByIdSync(mapId)
        if (map != null) {
            mindMapDao.updateMap(map.copy(updatedAt = now))
        }

        return twinRootNode
    }

    suspend fun deleteNodeWithSubtree(nodeId: String, mapId: Long) {
        val allNodes = mindNodeDao.getAllNodesForMapSync(mapId)
        val nodeToDelete = allNodes.firstOrNull { it.id == nodeId } ?: return
        val idsToDelete = mutableSetOf<String>()

        fun collectDescendants(id: String) {
            idsToDelete.add(id)
            val children = allNodes.filter { it.parentId == id }
            for (child in children) {
                collectDescendants(child.id)
            }
        }

        // Check if the node being deleted is an internal child of a synchronized twin parent
        val parent = if (nodeToDelete.parentId != null) allNodes.firstOrNull { it.id == nodeToDelete.parentId } else null
        val isInternalSyncedChild = parent != null && parent.isSyncTwin && nodeToDelete.isSyncTwin

        if (isInternalSyncedChild) {
            // Delete this internal child and its corresponding twin counterparts in all twin parent branches
            val syncKey = nodeToDelete.syncMasterId ?: nodeToDelete.id
            val twinNodes = allNodes.filter { candidate ->
                candidate.id == nodeId ||
                candidate.syncMasterId == syncKey ||
                (nodeToDelete.syncMasterId != null && candidate.id == nodeToDelete.syncMasterId)
            }
            for (twin in twinNodes) {
                collectDescendants(twin.id)
            }
        } else {
            // Top-level twin deletion or normal node deletion: delete this node & its own subtree
            collectDescendants(nodeId)
        }

        mindNodeDao.deleteNodesByIds(idsToDelete.toList())

        val now = System.currentTimeMillis()
        val map = mindMapDao.getMapByIdSync(mapId)
        if (map != null) {
            mindMapDao.updateMap(map.copy(updatedAt = now))
        }
    }

    fun exportToMarkdown(map: MindMapEntity, nodes: List<MindNodeEntity>): String {
        val sb = StringBuilder()
        sb.append("# 🪐 ").append(map.title).append("\n\n")
        if (map.description.isNotBlank()) {
            sb.append("> ").append(map.description).append("\n\n")
        }

        val rootNode = nodes.firstOrNull { it.parentId == null }
        if (rootNode == null) return sb.toString()

        fun printNode(node: MindNodeEntity, indentLevel: Int) {
            val indent = "  ".repeat(indentLevel)
            val syncBadge = if (node.isSyncTwin) " 🔗 [Synced Twin]" else ""
            sb.append(indent).append("- **").append(node.title).append("**").append(syncBadge).append("\n")

            if (node.notes.isNotBlank()) {
                val notesIndent = "  ".repeat(indentLevel + 1)
                node.notes.lines().forEach { line ->
                    if (line.isNotBlank()) {
                        sb.append(notesIndent).append("_").append(line).append("_\n")
                    }
                }
            }

            if (node.checklist.isNotEmpty()) {
                val checkIndent = "  ".repeat(indentLevel + 1)
                node.checklist.forEach { item ->
                    val checkMark = if (item.isDone) "[x]" else "[ ]"
                    sb.append(checkIndent).append("- ").append(checkMark).append(" ").append(item.text).append("\n")
                }
            }

            if (node.linkUrl.isNotBlank()) {
                val linkIndent = "  ".repeat(indentLevel + 1)
                sb.append(linkIndent).append("🔗 ").append(node.linkUrl).append("\n")
            }

            val children = nodes.filter { it.parentId == node.id }.sortedBy { it.orderIndex }
            for (child in children) {
                printNode(child, indentLevel + 1)
            }
        }

        // Print children of root
        val directChildren = nodes.filter { it.parentId == rootNode.id }.sortedBy { it.orderIndex }
        for (child in directChildren) {
            printNode(child, 0)
        }

        return sb.toString()
    }

    suspend fun seedStarterMaps() {
        // 1. Startup Launch Blueprint
        val map1Id = createMap(
            title = "Startup Launch Cosmos",
            description = "Radial planetary roadmap for launching our next generation product",
            themeColorHex = "#6366F1",
            rootIcon = "rocket_launch"
        )
        val root1 = mindNodeDao.getRootNodeSync(map1Id)
        if (root1 != null) {
            // Level 1 Orbiters
            val productNode = addNode(
                mapId = map1Id,
                parentId = root1.id,
                title = "Product & Architecture",
                colorHex = "#3B82F6",
                iconName = "code",
                notes = "Core engine design and responsive UI",
                checklist = listOf(
                    ChecklistItem(text = "Finalize orbital canvas gestures", isDone = true),
                    ChecklistItem(text = "Implement Room database", isDone = true),
                    ChecklistItem(text = "Polish animation transitions", isDone = false)
                )
            )
            // Level 2 Sub-orbiters for Product
            addNode(mapId = map1Id, parentId = productNode.id, title = "Orbital Physics", colorHex = "#06B6D4", iconName = "bolt")
            addNode(mapId = map1Id, parentId = productNode.id, title = "Offline SQLite", colorHex = "#10B981", iconName = "check_circle")
            addNode(mapId = map1Id, parentId = productNode.id, title = "Gesture Engine", colorHex = "#8B5CF6", iconName = "touch_app")

            val marketingNode = addNode(
                mapId = map1Id,
                parentId = root1.id,
                title = "Growth & Marketing",
                colorHex = "#EC4899",
                iconName = "flag",
                notes = "Viral loops, social media presence, and community launch",
                checklist = listOf(
                    ChecklistItem(text = "Create demo preview video", isDone = false),
                    ChecklistItem(text = "Post on Product Hunt", isDone = false),
                    ChecklistItem(text = "Engage Twitter/X tech creators", isDone = false)
                )
            )
            addNode(mapId = map1Id, parentId = marketingNode.id, title = "Demo Video", colorHex = "#F43F5E", iconName = "smart_display")
            addNode(mapId = map1Id, parentId = marketingNode.id, title = "Press Kit", colorHex = "#FB923C", iconName = "menu_book")
            addNode(mapId = map1Id, parentId = marketingNode.id, title = "Early Beta Users", colorHex = "#A855F7", iconName = "group")

            val designNode = addNode(
                mapId = map1Id,
                parentId = root1.id,
                title = "Visual Identity",
                colorHex = "#8B5CF6",
                iconName = "palette",
                notes = "Mindly-inspired planetary aesthetic with neon glows and smooth curves"
            )
            addNode(mapId = map1Id, parentId = designNode.id, title = "Cosmic Dark Canvas", colorHex = "#6366F1", iconName = "dark_mode")
            addNode(mapId = map1Id, parentId = designNode.id, title = "Vibrant Swatches", colorHex = "#F59E0B", iconName = "palette")
            addNode(mapId = map1Id, parentId = designNode.id, title = "Dynamic Breadcrumbs", colorHex = "#10B981", iconName = "explore")

            val opsNode = addNode(
                mapId = map1Id,
                parentId = root1.id,
                title = "Finances & Strategy",
                colorHex = "#10B981",
                iconName = "attach_money",
                notes = "Pricing tiers, budget allocation, and sustainable growth"
            )
            addNode(mapId = map1Id, parentId = opsNode.id, title = "Freemium Model", colorHex = "#059669", iconName = "star")
            addNode(mapId = map1Id, parentId = opsNode.id, title = "Server Infrastructure", colorHex = "#0284C7", iconName = "cloud")
        }

        // 2. Creative Idea Sandbox
        val map2Id = createMap(
            title = "Creative Idea Universe",
            description = "Freeform thought mapping, inspiration bubbles, and art projects",
            themeColorHex = "#EC4899",
            rootIcon = "psychology"
        )
        val root2 = mindNodeDao.getRootNodeSync(map2Id)
        if (root2 != null) {
            val storyNode = addNode(mapId = map2Id, parentId = root2.id, title = "Sci-Fi Novel Concept", colorHex = "#8B5CF6", iconName = "menu_book")
            addNode(mapId = map2Id, parentId = storyNode.id, title = "Orbital Space Habitat", colorHex = "#3B82F6", iconName = "explore")
            addNode(mapId = map2Id, parentId = storyNode.id, title = "AI Character Arc", colorHex = "#06B6D4", iconName = "psychology")

            val artNode = addNode(mapId = map2Id, parentId = root2.id, title = "Digital Art Gallery", colorHex = "#F59E0B", iconName = "brush")
            addNode(mapId = map2Id, parentId = artNode.id, title = "Cyberpunk Cityscapes", colorHex = "#EC4899", iconName = "palette")
            addNode(mapId = map2Id, parentId = artNode.id, title = "Minimalist Vectors", colorHex = "#10B981", iconName = "star")

            addNode(mapId = map2Id, parentId = root2.id, title = "Audio & Soundscapes", colorHex = "#06B6D4", iconName = "music_note")
            addNode(mapId = map2Id, parentId = root2.id, title = "Podcasts & Interviews", colorHex = "#10B981", iconName = "mic")
        }

        // 3. Daily Habits & Life Balance
        val map3Id = createMap(
            title = "Daily Balance & Habits",
            description = "Holistic personal growth and mindfulness orbits",
            themeColorHex = "#10B981",
            rootIcon = "favorite"
        )
        val root3 = mindNodeDao.getRootNodeSync(map3Id)
        if (root3 != null) {
            val morningNode = addNode(
                mapId = map3Id,
                parentId = root3.id,
                title = "Morning Flow",
                colorHex = "#F59E0B",
                iconName = "wb_sunny",
                checklist = listOf(
                    ChecklistItem(text = "500ml hydration", isDone = true),
                    ChecklistItem(text = "10 min mindfulness / meditation", isDone = true),
                    ChecklistItem(text = "Light stretching & sun", isDone = false)
                )
            )
            addNode(mapId = map3Id, parentId = morningNode.id, title = "Hydration", colorHex = "#06B6D4", iconName = "water_drop")
            addNode(mapId = map3Id, parentId = morningNode.id, title = "Meditation", colorHex = "#8B5CF6", iconName = "self_improvement")

            val deepWorkNode = addNode(
                mapId = map3Id,
                parentId = root3.id,
                title = "Deep Work Rituals",
                colorHex = "#3B82F6",
                iconName = "timer",
                notes = "90 minute focus blocks without notifications"
            )
            addNode(mapId = map3Id, parentId = deepWorkNode.id, title = "No Phone Morning", colorHex = "#EF4444", iconName = "phonelink_erase")
            addNode(mapId = map3Id, parentId = deepWorkNode.id, title = "Task Prioritization", colorHex = "#10B981", iconName = "checklist")

            val fitnessNode = addNode(mapId = map3Id, parentId = root3.id, title = "Physical Fitness", colorHex = "#EC4899", iconName = "fitness_center")
            addNode(mapId = map3Id, parentId = fitnessNode.id, title = "Strength Training", colorHex = "#F97316", iconName = "bolt")
            addNode(mapId = map3Id, parentId = fitnessNode.id, title = "Evening Walk", colorHex = "#10B981", iconName = "directions_walk")
        }
    }

    suspend fun createTemplateMap(templateKey: String, isArabic: Boolean = true): Long {
        return when (templateKey) {
            "goals" -> {
                val mapId = createMap(
                    title = if (isArabic) "أهدافي وإنجازاتي السنوية" else "Annual Goals & Milestones",
                    description = if (isArabic) "خارطة طريق لتحقيق الطموحات في المسار المهني والصحي والمالي" else "Roadmap for career, fitness, and personal growth",
                    themeColorHex = "#6366F1",
                    rootIcon = "flag"
                )
                val root = mindNodeDao.getRootNodeSync(mapId)
                if (root != null) {
                    val career = addNode(mapId, root.id, if (isArabic) "النمو المهني" else "Career Growth", "#3B82F6", "business_center")
                    addNode(mapId, career.id, if (isArabic) "مهارات جديدة" else "Skill Mastery", "#06B6D4", "psychology")
                    addNode(mapId, career.id, if (isArabic) "بناء العلاقات" else "Networking", "#8B5CF6", "group")

                    val health = addNode(mapId, root.id, if (isArabic) "اللياقة والصحة" else "Health & Vitality", "#10B981", "fitness_center")
                    addNode(mapId, health.id, if (isArabic) "تمارين يومية" else "Daily Workout", "#059669", "bolt")
                    addNode(mapId, health.id, if (isArabic) "نوم صحي" else "Rest & Sleep", "#34D399", "bedtime")

                    val finance = addNode(mapId, root.id, if (isArabic) "الاستقرار المالي" else "Financial Growth", "#F59E0B", "account_balance_wallet")
                    addNode(mapId, finance.id, if (isArabic) "ادخار واستثمار" else "Savings & Investing", "#D97706", "trending_up")

                    val mind = addNode(mapId, root.id, if (isArabic) "تطوير الذات" else "Mindset & Learning", "#EC4899", "menu_book")
                    addNode(mapId, mind.id, if (isArabic) "قراءة كتب" else "Book Digest", "#F43F5E", "auto_stories")
                }
                mapId
            }
            "launch" -> {
                val mapId = createMap(
                    title = if (isArabic) "إطلاق مشروع ومنتج جديد" else "Product & Project Launch",
                    description = if (isArabic) "مخطط استراتيجي من الفكرة حتى الوصول للمستخدمين" else "End-to-end launch strategy from MVP to market",
                    themeColorHex = "#06B6D4",
                    rootIcon = "rocket_launch"
                )
                val root = mindNodeDao.getRootNodeSync(mapId)
                if (root != null) {
                    val product = addNode(mapId, root.id, if (isArabic) "المنتج والخواص" else "Product & MVP", "#0284C7", "devices")
                    addNode(mapId, product.id, if (isArabic) "الميزات الأساسية" else "Core Features", "#38BDF8", "checklist")
                    addNode(mapId, product.id, if (isArabic) "تجربة المستخدم" else "UX & Flow", "#818CF8", "touch_app")

                    val marketing = addNode(mapId, root.id, if (isArabic) "التسويق والجمهور" else "Marketing & Launch", "#F43F5E", "campaign")
                    addNode(mapId, marketing.id, if (isArabic) "وسائل التواصل" else "Social Media", "#FB7185", "share")
                    addNode(mapId, marketing.id, if (isArabic) "حملة الإطلاق" else "Launch Event", "#F59E0B", "celebration")

                    val ops = addNode(mapId, root.id, if (isArabic) "البنية والتشغيل" else "Infrastructure & Tech", "#8B5CF6", "cloud")
                    addNode(mapId, ops.id, if (isArabic) "السيرفرات والأمان" else "Hosting & Security", "#A855F7", "security")
                }
                mapId
            }
            "brainstorm" -> {
                val mapId = createMap(
                    title = if (isArabic) "عصف ذهني وأفكار إبداعية" else "Creative Brainstorming",
                    description = if (isArabic) "مساحة حرة لجمع الأفكار والربط بين المفاهيم الملهمة" else "Freeform thinking canvas to connect brilliant sparks",
                    themeColorHex = "#EC4899",
                    rootIcon = "psychology"
                )
                val root = mindNodeDao.getRootNodeSync(mapId)
                if (root != null) {
                    val bold = addNode(mapId, root.id, if (isArabic) "أفكار ثورية وجريئة" else "Moonshot Ideas", "#F43F5E", "lightbulb")
                    addNode(mapId, bold.id, if (isArabic) "حلول خارج الصندوق" else "Out of the box", "#FB923C", "auto_awesome")

                    val quick = addNode(mapId, root.id, if (isArabic) "مكاسب سريعة" else "Quick Wins", "#10B981", "speed")
                    addNode(mapId, quick.id, if (isArabic) "تطبيقات فورية" else "Immediate Actions", "#34D399", "check_circle")

                    val insp = addNode(mapId, root.id, if (isArabic) "مراجع وإلهام" else "Inspiration & Art", "#8B5CF6", "palette")
                    addNode(mapId, insp.id, if (isArabic) "تصاميم وأمثلة" else "Visual Moodboard", "#A855F7", "brush")
                }
                mapId
            }
            "study" -> {
                val mapId = createMap(
                    title = if (isArabic) "ملخص دراسي وبحث معرفي" else "Study Digest & Research",
                    description = if (isArabic) "تلخيص شامل للمفاهيم والدروس والأسئلة المفتاحية" else "Structured synthesis of topics, chapters, and key findings",
                    themeColorHex = "#F59E0B",
                    rootIcon = "menu_book"
                )
                val root = mindNodeDao.getRootNodeSync(mapId)
                if (root != null) {
                    val core = addNode(mapId, root.id, if (isArabic) "المفاهيم المركزية" else "Core Concepts", "#D97706", "star")
                    addNode(mapId, core.id, if (isArabic) "التعاريف والمصطلحات" else "Key Definitions", "#FBBF24", "bookmark")

                    val practical = addNode(mapId, root.id, if (isArabic) "تطبيقات وأمثلة" else "Case Studies & Examples", "#10B981", "science")
                    addNode(mapId, practical.id, if (isArabic) "تجارب واقعية" else "Real Scenarios", "#059669", "verified")

                    val notes = addNode(mapId, root.id, if (isArabic) "أسئلة ومراجعة" else "Questions & Flashcards", "#8B5CF6", "help")
                    addNode(mapId, notes.id, if (isArabic) "نقاط الامتحان" else "Exam Notes", "#A855F7", "quiz")
                }
                mapId
            }
            else -> {
                createMap(
                    title = if (isArabic) "عالم فكري جديد" else "New Mental Universe",
                    description = "",
                    themeColorHex = "#6366F1",
                    rootIcon = "explore"
                )
            }
        }
    }

    suspend fun mergeMapIntoMap(
        sourceMapId: Long,
        targetMapId: Long,
        targetParentNodeId: String? = null,
        deleteSourceMap: Boolean = true
    ): MindNodeEntity? {
        if (sourceMapId == targetMapId) return null
        val sourceMap = mindMapDao.getMapByIdSync(sourceMapId) ?: return null
        val targetMap = mindMapDao.getMapByIdSync(targetMapId) ?: return null
        val sourceNodes = mindNodeDao.getAllNodesForMapSync(sourceMapId)
        val targetNodes = mindNodeDao.getAllNodesForMapSync(targetMapId)
        if (sourceNodes.isEmpty() || targetNodes.isEmpty()) return null

        val targetRootNode = (if (targetParentNodeId != null) targetNodes.firstOrNull { it.id == targetParentNodeId } else null)
            ?: targetNodes.firstOrNull { it.parentId == null }
            ?: return null

        val sourceRootNode = sourceNodes.firstOrNull { it.parentId == null }
            ?: sourceNodes.firstOrNull { it.id == sourceMap.rootNodeId }
            ?: sourceNodes.first()

        val now = System.currentTimeMillis()

        // Map old node IDs to new IDs
        val idMapping = mutableMapOf<String, String>()
        for (node in sourceNodes) {
            idMapping[node.id] = UUID.randomUUID().toString()
        }

        val existingTargetSiblings = targetNodes.filter { it.parentId == targetRootNode.id }
        val newBranchRootId = idMapping[sourceRootNode.id] ?: UUID.randomUUID().toString()

        // Source root node becomes an orbital child of targetRootNode
        val newBranchRootNode = sourceRootNode.copy(
            id = newBranchRootId,
            mapId = targetMapId,
            parentId = targetRootNode.id,
            orderIndex = existingTargetSiblings.size,
            createdAt = now,
            updatedAt = now
        )

        val newNodesToInsert = mutableListOf<MindNodeEntity>()
        newNodesToInsert.add(newBranchRootNode)

        for (node in sourceNodes) {
            if (node.id == sourceRootNode.id) continue
            val newId = idMapping[node.id] ?: UUID.randomUUID().toString()
            val newParentId = if (node.parentId == sourceRootNode.id) {
                newBranchRootId
            } else if (node.parentId != null) {
                idMapping[node.parentId] ?: newBranchRootId
            } else {
                newBranchRootId
            }
            newNodesToInsert.add(
                node.copy(
                    id = newId,
                    mapId = targetMapId,
                    parentId = newParentId,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }

        mindNodeDao.insertNodes(newNodesToInsert)
        mindMapDao.updateMap(targetMap.copy(updatedAt = now))

        if (deleteSourceMap) {
            deleteMap(sourceMapId)
        }

        return newBranchRootNode
    }
}
