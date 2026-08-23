package com.example.data.repository

import com.example.data.db.MindMapDao
import com.example.data.db.MindNodeDao
import com.example.data.model.ChecklistItem
import com.example.data.model.MindMapEntity
import com.example.data.model.MindNodeEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class MindMapRepository(
    private val mindMapDao: MindMapDao,
    private val mindNodeDao: MindNodeDao
) {
    val allMaps: Flow<List<MindMapEntity>> = mindMapDao.getAllMaps()

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
        syncMasterId: String? = null
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
}
